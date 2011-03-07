/*
 * Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.mail

import grails.util.GrailsUtil

import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimeUtility
import javax.servlet.ServletContext
import org.springframework.mail.MailSender
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.web.context.support.ServletContextResource

/**
 * Test case for {@link MailMessageBuilder}.
 */
class MailMessageBuilderTests extends GroovyTestCase {

    def testJavaMailSenderBuilder
    def testBasicMailSenderBuilder

    private String defaultFrom = "from@grailsplugin.com"
    private String defaultTo = "to@grailsplugin.com"

    protected void setUp() {
        def config = new ConfigObject()
        config.default.from = defaultFrom
        config.default.to = defaultTo

        def mockJavaMailSender = [
            createMimeMessage: {-> new MimeMessage(Session.getInstance(new Properties())) }
        ] as JavaMailSender
        testJavaMailSenderBuilder = new MailMessageBuilder(mockJavaMailSender, config)

        def mockBasicMailSender = [:] as MailSender
        testBasicMailSenderBuilder = new MailMessageBuilder(mockBasicMailSender, config)
    }

    void testStreamCharBufferForGrails12() {
        if (!GrailsUtil.grailsVersion.startsWith("1.2")) {
            return
        }

        processDsl {
            to "fred@g2one.com"
            subject "Hello Fred"

            def text = getClass().classLoader.loadClass("org.codehaus.groovy.grails.web.util.StreamCharBuffer").newInstance()
            text.writer << 'How are you?'
            body text
        }

        def msg = testJavaMailSenderBuilder.message.mimeMessage
        assertEquals 1, to(msg).size()
        assertEquals "fred@g2one.com", to(msg)[0].toString()
        assertEquals "Hello Fred", msg.subject
        assertEquals "How are you?", msg.content
    }

    /**
     * Tests the basic elements of the mail DSL.
     */
    void testBasics() {
        processDsl {
            to "fred@g2one.com"
            subject "Hello Fred"
            body 'How are you?'
        }

        def msg = testJavaMailSenderBuilder.message.mimeMessage
        assertEquals 1, to(msg).size()
        assertEquals "fred@g2one.com", to(msg)[0].toString()
        assertEquals "Hello Fred", msg.subject
        assertEquals "How are you?", msg.content
    }

    /**
     * Tests that multiple recipients are added to the underlying mail
     * message correctly.
     */
    void testMultipleRecipients() {
        processDsl {
            to "fred@g2one.com","ginger@g2one.com", "grace@hollywood.com"
            from "john@g2one.com"
            cc "marge@g2one.com", "ed@g2one.com"
            bcc "joe@g2one.com"
            subject "Hello John"
            body 'this is some text'
        }

        def msg = testJavaMailSenderBuilder.message.mimeMessage
        assertEquals([ "fred@g2one.com", "ginger@g2one.com", "grace@hollywood.com" ], to(msg))
        assertEquals([ "marge@g2one.com", "ed@g2one.com" ], cc(msg))
        assertEquals([ "joe@g2one.com" ], bcc(msg))
        assertEquals 1, msg.from.size()
        assertEquals "john@g2one.com", msg.from[0].toString()
        assertEquals "Hello John", msg.subject
        assertEquals "this is some text", msg.content
    }

    /**
     * Tests the "headers" feature of the mail DSL. It should add the
     * specified headers to the underlying MIME message.
     */
    void testHeaders() {
        processDsl {
            headers "X-Mailing-List": "user@grails.codehaus.org",
                    "Sender": "dilbert@somewhere.org"
            to "fred@g2one.com"
            subject "Hello Fred"
            body 'How are you?'
        }

        def msg = testJavaMailSenderBuilder.message.mimeMessage
        assertEquals "user@grails.codehaus.org", msg.getHeader("X-Mailing-List", ", ")
        assertEquals "dilbert@somewhere.org", msg.getHeader("Sender", ", ")
        assertEquals([ "fred@g2one.com" ], to(msg))
        assertEquals "Hello Fred", msg.subject
        assertEquals "How are you?", msg.content
    }

    /**
     * Tests that the builder throws an exception if the user tries to
     * specify custom headers with just a plain MailSender.
     */
    void testHeadersWithBasicMailSender() {
        shouldFail(GrailsMailException) {
            processDsl(testBasicMailSenderBuilder) {
                headers "Content-Type": "text/plain;charset=UTF-8",
                        "Sender": "dilbert@somewhere.org"
                to "fred@g2one.com"
                subject "Hello Fred"
                body 'How are you?'
            }
        }
    }

    void testDefaultToAndFrom() {
        processDsl {
            subject "Hello Fred"
            body 'How are you?'
        }

        def msg = testJavaMailSenderBuilder.message.mimeMessage
        assertEquals defaultTo, to(msg)[0].toString()
        assertEquals defaultFrom, msg.from[0].toString()
    }

    void testEnvelopeFrom() {
        processDsl {
            to "fred@g2one.com"
            from "john@g2one.com"
            envelopeFrom "peter@g2one.com"
            subject "Hello Fred"
            body 'How are you?'
        }

        def msg = testJavaMailSenderBuilder.message.mimeMessage
        assertEquals "fred@g2one.com", to(msg)[0].toString()
        assertEquals "john@g2one.com", msg.from[0].toString()
        assertEquals "peter@g2one.com", testJavaMailSenderBuilder.envelopeFrom
    }
    
    void testAttachment() {
        processDsl {
            multipart true
            to "fred@g2one.com"
            subject "Hello Fred"
            body 'How are you?'
            attachBytes "dummy.bin", "application/binary", "abcdef".bytes
            attachBytes "äöü.bin", "application/binary", "abcdef".bytes
        }
        def msg = testJavaMailSenderBuilder.message.mimeMessage
        assertTrue msg.content instanceof MimeMultipart
        assertEquals 3, msg.content.count

        def attachment = msg.content.getBodyPart(1)
        assertEquals "abcdef", attachment.content.text
        assertEquals "dummy.bin", attachment.fileName

        attachment = msg.content.getBodyPart(2)
        assertEquals MimeUtility.encodeWord("äöü.bin"), attachment.fileName
    }

    void testStream() {
        def servletContext = [getResourceAsStream: { new ByteArrayInputStream("abcdef".bytes) }] as ServletContext
        processDsl {
           multipart true
           to "fred@g2one.com"
           subject "Hello Fred"
           body 'How are you?'
           attach "dummy.bin", "application/binary", new ServletContextResource(servletContext, "path/to/file")
        }

        def msg = testJavaMailSenderBuilder.message.mimeMessage
        assertTrue msg.content instanceof MimeMultipart
        assertEquals 2, msg.content.count

        def attachment = msg.content.getBodyPart(1)
        assertEquals "abcdef", attachment.content.text
        assertEquals "dummy.bin", attachment.fileName
    }

    void testAttachmentUsingAttachFileNoFilenameOverride() {
        //create temp file to attach
        def tempFile = File.createTempFile("grailsMailUnitTest",".txt")
        tempFile << 'abcdef'

        try {
            processDsl {
                multipart true
                to "fred@g2one.com"
                subject "Hello Fred"
                body 'How are you?'
                attach tempFile
            }
            def msg = testJavaMailSenderBuilder.message.mimeMessage
            assertTrue msg.content instanceof MimeMultipart
            assertEquals 2, msg.content.count

            def attachment = msg.content.getBodyPart(1)
            assertEquals 'abcdef', attachment.content
            assertEquals tempFile.name, attachment.fileName
            assertEquals 'text/plain', attachment.contentType
        }
        finally {
            tempFile.delete()
        }
    }

    void testAttachmentUsingAttachFileWithFilenameOverride() {
        //create temp file to attach
        def tempFile = File.createTempFile("grailsMailUnitTest",".txt")
        tempFile << 'abcdef'

        try {
            processDsl {
                multipart true
                to "fred@g2one.com"
                subject "Hello Fred"
                body 'How are you?'
                attach 'alternativeName.txt', tempFile
            }
            def msg = testJavaMailSenderBuilder.message.mimeMessage
            assertTrue msg.content instanceof MimeMultipart
            assertEquals 2, msg.content.count

            def attachment = msg.content.getBodyPart(1)
            assertEquals 'abcdef', attachment.content
            assertEquals 'alternativeName.txt', attachment.fileName
            assertEquals 'text/plain', attachment.contentType
        }
        finally {
            tempFile.delete()
        }
    }

    void testAttachFileWithNonExistentFile() {
        def ex = shouldFail(FileNotFoundException) {
            processDsl {
                multipart true
                to "fred@g2one.com"
                subject "Hello Fred"
                body 'How are you?'
                attach new File("I don't exist.zip")
            }
        }
    }
	
	//for issue GPMAIL-60
	void testAttachCallInBeginningOfDsl() {
		
		def servletContext = [getResourceAsStream: { new ByteArrayInputStream("abcdef".bytes) }] as ServletContext
		processDsl {
		   multipart true
		   attach "dummy.bin", "application/binary", new ServletContextResource(servletContext, "path/to/file")
		   to "fred@g2one.com"
		   subject "Hello Fred"
		   body 'How are you?'
		}

		def msg = testJavaMailSenderBuilder.message.mimeMessage
		assertTrue msg.content instanceof MimeMultipart
		assertEquals 2, msg.content.count
		assertEquals "fred@g2one.com", msg.getHeader("To", " ")
		assertEquals "Hello Fred", msg.getHeader("Subject", " ")

		def attachment = msg.content.getBodyPart(1)
		assertEquals "abcdef", attachment.content.text
		assertEquals "dummy.bin", attachment.fileName
	}

    private List to(MimeMessage msg) {
        msg.getRecipients(Message.RecipientType.TO)*.toString()
    }

    private List cc(MimeMessage msg) {
        msg.getRecipients(Message.RecipientType.CC)*.toString()
    }

    private List bcc(MimeMessage msg) {
        msg.getRecipients(Message.RecipientType.BCC)*.toString()
    }

    private processDsl(Closure c) {
        processDsl(testJavaMailSenderBuilder, c)
    }

    private processDsl(MailMessageBuilder builder, Closure c) {
        c.delegate = builder
        c.call()
        builder.finishMessage()
    }
}
