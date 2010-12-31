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

import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.MimeMessage
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.springframework.mail.MailSender
import org.springframework.mail.javamail.JavaMailSender
import javax.mail.internet.MimeMultipart

/**
 * Test case for {@link MailMessageBuilder}.
 */
class MailMessageBuilderTests extends GroovyTestCase {

    def testJavaMailSenderBuilder
    def testBasicMailSenderBuilder

    void setUp() {
        def config = new ConfigObject()
        config.default.from = "test@grailsplugin.com"
        
        def mockJavaMailSender = [
                createMimeMessage: {-> return new MimeMessage(Session.getInstance(new Properties())) }
        ] as JavaMailSender
        testJavaMailSenderBuilder = new MailMessageBuilder(mockJavaMailSender, config)
        
        def mockBasicMailSender = [:] as MailSender
        testBasicMailSenderBuilder = new MailMessageBuilder(mockBasicMailSender, config)
    }

    void testStreamCharBufferForGrails12() {
        if(grails.util.GrailsUtil.grailsVersion.startsWith("1.2")) {
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

    void testAttachment() {
        processDsl {
            multipart true
            to "fred@g2one.com"
            subject "Hello Fred"
            body 'How are you?'
            attachBytes "dummy.bin", "application/binary", "abcdef".bytes
        }
        def msg = testJavaMailSenderBuilder.message.mimeMessage
        assertTrue msg.content instanceof MimeMultipart
        assertEquals 2, msg.content.count

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
    }
    
}
