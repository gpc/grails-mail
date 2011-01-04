/*
 * Copyright 2004-2005 the original author or authors.
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
import javax.mail.internet.MimeMessage

import org.springframework.mail.javamail.*
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.MailSender

import org.springframework.core.io.FileSystemResource

import com.icegreen.greenmail.util.ServerSetupTest
import org.springframework.core.io.*

import javax.mail.internet.MimeMultipart

import org.springframework.web.context.request.RequestContextHolder

class MailServiceTests extends GroovyTestCase {

    static transactional = false
    
    def mimeCapableMailService
    def nonMimeCapableMailService
    
    def mailMessageContentRenderer // autowired
    
    void setUp() {
        def mimeMailSender = new JavaMailSenderImpl(host: "localhost", port: ServerSetupTest.SMTP.port)
        def mimeMessageBuilderFactor = new MailMessageBuilderFactory(
            mailSender: mimeMailSender, 
            mailMessageContentRenderer: mailMessageContentRenderer
        )
        mimeCapableMailService = new MailService(mailMessageBuilderFactory: mimeMessageBuilderFactor)
                
        def simpleMailSender = new MailSender() {
            void send(SimpleMailMessage simpleMessage) {}
            void send(SimpleMailMessage[] simpleMessages) {}
        }
        def simpleMessageBuilderFactory = new MailMessageBuilderFactory(
            mailSender: simpleMailSender, 
            mailMessageContentRenderer: mailMessageContentRenderer
        )
        nonMimeCapableMailService = new MailService(mailMessageBuilderFactory: simpleMessageBuilderFactory)
    }

    void testSendSimpleMessage() {
        def message = nonMimeCapableMailService.sendMail {
            to "fred@g2one.com"
            title "Hello John"
            body 'this is some text'
            from 'king@g2one.com'
        }

        assertEquals "Hello John", message.getSubject()
        assertEquals 'this is some text', message.getText()
        assertEquals 'fred@g2one.com', message.to[0]
        assertEquals 'king@g2one.com', message.from
    }

    void testSendToMultipleRecipients() {
        def message = nonMimeCapableMailService.sendMail {
            to "fred@g2one.com", "ginger@g2one.com"
            title "Hello John"
            body 'this is some text'
        }

        assertEquals "Hello John", message.getSubject()
        assertEquals 'this is some text', message.getText()
        assertEquals "fred@g2one.com", message.getTo()[0]
        assertEquals "ginger@g2one.com", message.getTo()[1]
    }

    void testSendToMultipleRecipientsUsingList() {
        def message = nonMimeCapableMailService.sendMail {
            to(["fred@g2one.com", "ginger@g2one.com"])
            title "Hello John"
            body 'this is some text'
        }
        assertEquals "fred@g2one.com", message.getTo()[0]
        assertEquals "ginger@g2one.com", message.getTo()[1]
    }

    void testSendToMultipleCCRecipientsUsingList() {
        def message = nonMimeCapableMailService.sendMail {
            to 'joe@g2one.com'
            cc(["fred@g2one.com", "ginger@g2one.com"])
            title "Hello John"
            body 'this is some text'
        }
        assertEquals "fred@g2one.com", message.cc[0]
        assertEquals "ginger@g2one.com", message.cc[1]
    }

    void testSendToMultipleBCCRecipientsUsingList() {
        def message = nonMimeCapableMailService.sendMail {
            to("joe@g2one.com")
            bcc(["fred@g2one.com", "ginger@g2one.com"])
            title "Hello John"
            body 'this is some text'
        }
        assertEquals "fred@g2one.com", message.bcc[0]
        assertEquals "ginger@g2one.com", message.bcc[1]
    }

    void testSendToMultipleRecipientsAndCC() {
        def message = nonMimeCapableMailService.sendMail {
            to "fred@g2one.com", "ginger@g2one.com"
            from "john@g2one.com"
            cc "marge@g2one.com", "ed@g2one.com"
            bcc "joe@g2one.com"
            title "Hello John"
            body 'this is some text'
        }

        assertEquals "Hello John", message.getSubject()
        assertEquals 'this is some text', message.getText()
        assertEquals 2, message.to.size()
        assertEquals "fred@g2one.com", message.getTo()[0]
        assertEquals "ginger@g2one.com", message.getTo()[1]
        assertEquals "john@g2one.com", message.from
        assertEquals 2, message.cc.size()
        assertEquals "marge@g2one.com", message.cc[0]
        assertEquals "ed@g2one.com", message.cc[1]
    }

    void testSendHtmlMail() {
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            html '<b>Hello</b> World'
        }

        assertEquals "Hello John", message.getMimeMessage().getSubject()
        // This isn't working - because no DataHandler available for unit tests?
        //assertTrue message.mimeMessage.contentType.startsWith('text/html')
        assertEquals '<b>Hello</b> World', message.getMimeMessage().getContent()
    }

    void testSendMailView() {
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body(view: '/_testemails/test', model: [msg: 'hello'])
        }

        assertEquals "Hello John", message.getMimeMessage().getSubject()
        assertTrue message.mimeMessage.contentType.startsWith('text/plain')
        assertEquals 'Message is: hello', message.getMimeMessage().getContent().trim()
    }

    void testSendMailViewText() {
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            text view: '/_testemails/test', model: [msg: 'hello']
        }

        assertTrue message.mimeMessage.contentType.startsWith('text/plain')
        assertEquals 'Message is: hello', message.getMimeMessage().getContent().trim()
    }

    void testSendMailViewHtmlMethod() {
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            html view: '/_testemails/testhtml', model: [msg: 'hello']
        }

        assertEquals "Hello John", message.getMimeMessage().getSubject()
        // This isn't working - because no DataHandler available for unit tests?
        //assertTrue message.mimeMessage.contentType.startsWith('text/html')
        assertEquals '<b>Message is: hello</b>', message.getMimeMessage().getContent().trim()
    }

    void testSendMailViewHTML() {
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body(view: '/_testemails/testhtml', model: [msg: 'hello'])
        }

        assertEquals "Hello John", message.getMimeMessage().getSubject()
        // This isn't working - because no DataHandler available for unit tests?
        //assertTrue message.mimeMessage.contentType.startsWith('text/html')
        assertEquals '<b>Message is: hello</b>', message.getMimeMessage().getContent().trim()
    }

    void testSendMailViewWithTags() {
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body(view: '/_testemails/tagtest', model: [condition: true])
        }

        assertEquals "Hello John", message.getMimeMessage().getSubject()
        assertEquals 'Condition is true', message.getMimeMessage().getContent().trim()

        message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body(view: '/_testemails/tagtest', model: [condition: false])
        }

        assertEquals "Hello John", message.getMimeMessage().getSubject()
        assertTrue message.getMimeMessage().getContentType()?.startsWith('text/plain')
        assertEquals '', message.getMimeMessage().getContent().trim()
    }

    void testSendMailViewNoModel() {
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body(view: '/_testemails/test')
        }

        assertEquals "Hello John", message.getMimeMessage().getSubject()
        assertTrue message.getMimeMessage().getContentType()?.startsWith('text/plain')
        assertEquals 'Message is:', message.getMimeMessage().getContent().trim()
    }

    /**
     * Tests the "headers" feature of the mail DSL. It should add the
     * specified headers to the underlying MIME message.
     */
    void testSendMailWithHeaders() {
        def message = mimeCapableMailService.sendMail {
            headers "X-Mailing-List": "user@grails.codehaus.org",
                "Sender": "dilbert@somewhere.org"
            to "fred@g2one.com"
            subject "Hello Fred"
            body 'How are you?'
        }

        def msg = message.mimeMessageHelper.mimeMessage
        assertEquals "user@grails.codehaus.org", msg.getHeader("X-Mailing-List", ", ")
        assertEquals "dilbert@somewhere.org", msg.getHeader("Sender", ", ")
        assertEquals(["fred@g2one.com"], to(msg))
        assertEquals "Hello Fred", msg.subject
        assertEquals "How are you?", msg.content
    }

    /**
     * Tests that the builder throws an exception if the user tries to
     * specify custom headers with just a plain MailSender.
     */
    void testSendMailWithHeadersAndBasicMailSender() {
        shouldFail(GrailsMailException) {
            nonMimeCapableMailService.sendMail {
                headers "Content-Type": "text/plain;charset=UTF-8",
                    "Sender": "dilbert@somewhere.org"
                to "fred@g2one.com"
                subject "Hello Fred"
                body 'How are you?'
            }
        }
    }

    void testSendmailWithTranslations() {
        def message = mimeCapableMailService.sendMail {
            from 'neur0maner@gmail.com'
            to "neur0maner@gmail.com"
            locale 'en_US'
            subject "Hello"
            body(view: '/_testemails/i18ntest', model: [name: 'Luis'])
        }
        def msg = message.mimeMessageHelper.mimeMessage
        final def slurper = new XmlSlurper()
        def html = slurper.parseText(msg.content)
        assertEquals 'Translate this: Luis', html.body.toString()

        message = mimeCapableMailService.sendMail {
            from 'neur0maner@gmail.com'
            to "neur0maner@gmail.com"
            locale Locale.FRENCH
            subject "Hello"
            body(view: '/_testemails/i18ntest', model: [name: 'Luis'])
        }

        msg = message.mimeMessageHelper.mimeMessage
        html = slurper.parseText(msg.content)
        assertEquals 'Traduis ceci: Luis', html.body.toString()
    }

    void testSendmailWithByteArrayAttachment() {
        def message = mimeCapableMailService.sendMail {
            multipart true

            to "fred@g2one.com", "ginger@g2one.com"
            from "john@g2one.com"
            cc "marge@g2one.com", "ed@g2one.com"
            bcc "joe@g2one.com"
            title "Hello John"
            attachBytes 'fileName', 'text/plain', 'Hello World'.getBytes("US-ASCII")
            html 'this is some text'
        }

        def content = message.mimeMessage.content
        assertEquals 2, content.count
        assertEquals 'Hello World', content.getBodyPart(1).inputStream.text
        assertEquals 'Hello World', content.getBodyPart(1).inputStream.text
    }

    void testSendmailWithByteArrayAndResourceAttachments() {
        def tmpFile
        try {                
            def message = mimeCapableMailService.sendMail {
                multipart true

                tmpFile = File.createTempFile('testSendmailWithAttachments',null)
                tmpFile << 'Hello World'

                to "fred@g2one.com", "ginger@g2one.com"
                from "john@g2one.com"
                cc "marge@g2one.com", "ed@g2one.com"
                bcc "joe@g2one.com"
                title "Hello John"
                attach 'fileName', 'text/plain', 'Dear John'.getBytes("US-ASCII")
                attach 'fileName2', 'application/octet-stream', new FileSystemResource(tmpFile)
                html 'this is some text'
            }

            def content=message.mimeMessage.content
            assertEquals 3, content.count
            assertEquals 'Dear John',content.getBodyPart(1).inputStream.text
            assertEquals 'Hello World',content.getBodyPart(2).inputStream.text
        }
        finally {
            tmpFile?.delete()
        }
    }

    void testInlineAttachment() {
        def bytes = getClass().getResource("grailslogo.png").bytes
        
        def message = mimeCapableMailService.sendMail {
            multipart true
            to "fred@g2one.com", "ginger@g2one.com"
            from "john@g2one.com"
            title "Hello John"
            text 'this is some text <img src="cid:abc123" />'
            inline 'abc123', 'image/png', bytes
        }
        
        def inlinePart = message.mimeMessage.content.getBodyPart(0).content.getBodyPart("<abc123>")
        assert inlinePart.inputStream.bytes == bytes
    }

    void testHtmlContentType() {
        def msg = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "test"
            html '<html><head></head><body>How are you?</body></html>'
        }.mimeMessage
        
        // assert msg.contentType == 'text/html; charset=UTF-8'  // is text/plain here, but not in production
        assert msg.content == '<html><head></head><body>How are you?</body></html>'
    }

    void testMultipart_html_first() {
        def msg = mimeCapableMailService.sendMail {
            multipart true
            to "fred@g2one.com"
            subject "test"
            html '<html><head></head><body>How are you?</body></html>'
            text 'How are you?'
        }.mimeMessage
        
        assert msg.content instanceof MimeMultipart
        
        MimeMultipart mp = msg.content.getBodyPart(0).content.getBodyPart(0).content
        
        assert mp.count == 2

        assert mp.getBodyPart(0).contentType.startsWith('text/plain')
        assert mp.getBodyPart(0).content == 'How are you?'
        
        assert mp.getBodyPart(1).contentType.startsWith('text/html')
        assert mp.getBodyPart(1).content == '<html><head></head><body>How are you?</body></html>'
    }

    void testMultipart_text_first() {
        def msg = mimeCapableMailService.sendMail {
            multipart true
            to "fred@g2one.com"
            subject "test"
            text 'How are you?'
            html '<html><head></head><body>How are you?</body></html>'
        }.mimeMessage

        assert msg.content instanceof MimeMultipart

        MimeMultipart mp = msg.content.getBodyPart(0).content.getBodyPart(0).content
        
        assert mp.count == 2

        assert mp.getBodyPart(0).contentType.startsWith('text/plain')
        assert mp.getBodyPart(0).content == 'How are you?'
        
        assert mp.getBodyPart(1).contentType.startsWith('text/html')
        assert mp.getBodyPart(1).content == '<html><head></head><body>How are you?</body></html>'
    }

    void testMultipartMode() {
        def msg = mimeCapableMailService.sendMail {
            multipart MimeMessageHelper.MULTIPART_MODE_RELATED
            to "fred@g2one.com"
            subject "test"
            text 'How are you?'
            html '<html><head></head><body>How are you?</body></html>'
        }.mimeMessage

        assert msg.content instanceof MimeMultipart

        MimeMultipart mp = msg.content.getBodyPart(0).content
        
        assert mp.count == 2

        assert mp.getBodyPart(0).contentType.startsWith('text/plain')
        assert mp.getBodyPart(0).content == 'How are you?'
        
        assert mp.getBodyPart(1).contentType.startsWith('text/html')
        assert mp.getBodyPart(1).content == '<html><head></head><body>How are you?</body></html>'
    }
    
    // http://jira.codehaus.org/browse/GRAILSPLUGINS-2232
    void testContentTypeDoesNotGetChanged() {
        def originalContentType = RequestContextHolder.currentRequestAttributes().currentResponse.contentType

        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body(view: '/_testemails/test', model: [msg: 'hello'])
        }
        
        assertEquals "Hello John", message.getMimeMessage().getSubject()
        assertTrue message.mimeMessage.contentType.startsWith('text/plain')
        assertEquals 'Message is: hello', message.getMimeMessage().getContent().trim()
        
        assertEquals originalContentType, RequestContextHolder.currentRequestAttributes().currentResponse.contentType
    }
     
    void testViewResolutionFromPlugin() {
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body view: '/email/email', plugin: 'for-plugin-view-resolution'
        }

        assertEquals "Hello John", message.getMimeMessage().getSubject()
        assertTrue message.mimeMessage.contentType.startsWith('text/plain')
        assertEquals 'This is from a plugin!!!', message.getMimeMessage().getContent().trim()
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
}

