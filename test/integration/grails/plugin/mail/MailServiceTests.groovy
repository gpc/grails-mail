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

import java.util.concurrent.ExecutorService;

import javax.mail.Message
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.core.io.FileSystemResource
import org.springframework.mail.MailMessage
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMailMessage
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.web.context.request.RequestContextHolder

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.instanceOf;

import groovy.mock.interceptor.MockFor

import grails.plugin.greenmail.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import grails.test.mixin.integration.IntegrationTestMixin
import grails.test.mixin.*
import org.junit.*
import static org.junit.Assert.*

@TestMixin(IntegrationTestMixin)
class MailServiceTests  {

    static transactional = false

    MailService mimeCapableMailService
    MailService nonMimeCapableMailService

    MailMessageContentRenderer mailMessageContentRenderer // autowired
	GrailsApplication grailsApplication // autowired
	GreenMail greenMail

	@Before
    void setUp() {
        MailSender mimeMailSender = new JavaMailSenderImpl(host: "localhost", port: ServerSetupTest.SMTP.port)
        MailMessageBuilderFactory mimeMessageBuilderFactor = new MailMessageBuilderFactory(
            mailSender: mimeMailSender,
            mailMessageContentRenderer: mailMessageContentRenderer
        )
        mimeCapableMailService = new MailService(
            mailMessageBuilderFactory: mimeMessageBuilderFactor,
            grailsApplication: grailsApplication)
		mimeCapableMailService.afterPropertiesSet()

        MailSender simpleMailSender = new SimpleMailSender()
        MailMessageBuilderFactory simpleMessageBuilderFactory = new MailMessageBuilderFactory(
            mailSender: simpleMailSender,
            mailMessageContentRenderer: mailMessageContentRenderer
        )
        nonMimeCapableMailService = new MailService(
            mailMessageBuilderFactory: simpleMessageBuilderFactory,
            grailsApplication: grailsApplication)
		nonMimeCapableMailService.afterPropertiesSet()
    }

	@After
	void tearDown(){
		mimeCapableMailService.destroy()
		nonMimeCapableMailService.destroy()
		greenMail.deleteAllMessages()
	}

    void testSendSimpleMessage() {
        MailMessage message = nonMimeCapableMailService.sendMail {
            to "fred@g2one.com"
            title "Hello John"
            body 'this is some text'
            from 'king@g2one.com'
        }
		assertThat(message, instanceOf(SimpleMailMessage.class));

        assert "Hello John" == ((SimpleMailMessage)message).getSubject()
        assert 'this is some text'  ==message.getText()
        assert 'fred@g2one.com' == message.to[0]
        assert 'king@g2one.com' == message.from
    }

    void testAsyncSendSimpleMessage() {
        MailMessage message = nonMimeCapableMailService.sendMail {
			async true
            to "fred@g2one.com"
            title "Hello John"
            body 'this is some text'
            from 'king@g2one.com'
        }
		assertThat(message, instanceOf(SimpleMailMessage.class));

        assert "Hello John" == ((SimpleMailMessage)message).getSubject()
        assert 'this is some text' == message.getText()
        assert 'fred@g2one.com' == message.to[0]
        assert 'king@g2one.com' == message.from
    }

    void testSendToMultipleRecipients() {
        MailMessage message = nonMimeCapableMailService.sendMail {
            to "fred@g2one.com", "ginger@g2one.com"
            title "Hello John"
            body 'this is some text'
        }

		assertThat(message, instanceOf(SimpleMailMessage.class));
        assertEquals "Hello John", ((SimpleMailMessage)message).getSubject()
        assertEquals 'this is some text', message.getText()
        assertEquals "fred@g2one.com", message.getTo()[0]
        assertEquals "ginger@g2one.com", message.getTo()[1]
    }

    void testSendToMultipleRecipientsUsingList() {
        MailMessage message = nonMimeCapableMailService.sendMail {
            to(["fred@g2one.com", "ginger@g2one.com"])
            title "Hello John"
            body 'this is some text'
        }
        assertEquals "fred@g2one.com", message.getTo()[0]
        assertEquals "ginger@g2one.com", message.getTo()[1]
    }

    void testSendToMultipleCCRecipientsUsingList() {
        MailMessage message = nonMimeCapableMailService.sendMail {
            to 'joe@g2one.com'
            cc(["fred@g2one.com", "ginger@g2one.com"])
            title "Hello John"
            body 'this is some text'
        }
        assertEquals "fred@g2one.com", message.cc[0]
        assertEquals "ginger@g2one.com", message.cc[1]
    }

    void testSendToMultipleBCCRecipientsUsingList() {
        MailMessage message = nonMimeCapableMailService.sendMail {
            to("joe@g2one.com")
            bcc(["fred@g2one.com", "ginger@g2one.com"])
            title "Hello John"
            body 'this is some text'
        }
        assertEquals "fred@g2one.com", message.bcc[0]
        assertEquals "ginger@g2one.com", message.bcc[1]
    }

    void testSendToMultipleRecipientsAndCC() {
        MailMessage message = nonMimeCapableMailService.sendMail {
            to "fred@g2one.com", "ginger@g2one.com"
            from "john@g2one.com"
            cc "marge@g2one.com", "ed@g2one.com"
            bcc "joe@g2one.com"
            title "Hello John"
            body 'this is some text'
        }

		assertThat(message, instanceOf(SimpleMailMessage.class));

        assertEquals "Hello John", ((SimpleMailMessage)message).getSubject()
        assertEquals 'this is some text', message.getText()
        assertEquals 2, message.to.size()
        assertEquals "fred@g2one.com", message.getTo()[0]
        assertEquals "ginger@g2one.com", message.getTo()[1]
        assertEquals "john@g2one.com", message.from
        assertEquals 2, message.cc.size()
        assertEquals "marge@g2one.com", message.cc[0]
        assertEquals "ed@g2one.com", message.cc[1]
    }

    void testSendMailWithEnvelopeFrom() {
        def message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            title "Hello John"
            body 'this is some text'
            from 'king@g2one.com'
            envelopeFrom 'peter@g2one.com'
        }

        def msg = message.mimeMessage
        assertEquals "Hello John", msg.getSubject()
        assertEquals "king@g2one.com", msg.getFrom()[0].toString()

        def greenMsg = greenMail.getReceivedMessages()[0]
        assertEquals "<peter@g2one.com>", greenMsg.getHeader("Return-Path", ",")
    }

    void testSendMailWithEnvelopeFromAndBasicMailSender() {
        shouldFail(GrailsMailException) {
            def message = nonMimeCapableMailService.sendMail {
                to "fred@g2one.com"
                title "Hello John"
                body 'this is some text'
                from 'king@g2one.com'
                envelopeFrom 'peter@g2one.com'
            }
        }
    }

    void testSendHtmlMail() {
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            html '<b>Hello</b> World'
        }

        assertEquals "Hello John", message.getMimeMessage().getSubject()
        assertTrue message.mimeMessage.contentType.startsWith('text/html')
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
        assertTrue message.mimeMessage.contentType.startsWith('text/html')
        assertEquals '<b>Message is: hello</b>', message.getMimeMessage().getContent().trim()
    }

    void testSendMailViewHTML() {
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body(view: '/_testemails/testhtml', model: [msg: 'hello'])
        }

        assertEquals "Hello John", message.getMimeMessage().getSubject()
        assertTrue message.mimeMessage.contentType.startsWith('text/html')
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
        MailMessage message = mimeCapableMailService.sendMail {
            headers "X-Mailing-List": "user@grails.codehaus.org",
                "Sender": "dilbert@somewhere.org"
            to "fred@g2one.com"
            subject "Hello Fred"
            body 'How are you?'
        }
		assertThat(message, instanceOf(MimeMailMessage.class));

        MimeMessage msg = ((MimeMailMessage)message).mimeMessageHelper.mimeMessage
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
        MailMessage message = mimeCapableMailService.sendMail {
            from 'neur0maner@gmail.com'
            to "neur0maner@gmail.com"
            locale 'en_US'
            subject "Hello"
            body(view: '/_testemails/i18ntest', model: [name: 'Luis'])
        }
		assertThat(message, instanceOf(MimeMailMessage.class));
        MimeMessage msg = ((MimeMailMessage)message).mimeMessageHelper.mimeMessage
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
		assertThat(message, instanceOf(MimeMailMessage.class));

        msg = ((MimeMailMessage)message).mimeMessageHelper.mimeMessage
        html = slurper.parseText(msg.content)
        assertEquals 'Traduis ceci: Luis', html.body.toString()
    }

    void testSendmailWithByteArrayAttachment() {
        MailMessage message = mimeCapableMailService.sendMail {
            multipart true

            to "fred@g2one.com", "ginger@g2one.com"
            from "john@g2one.com"
            cc "marge@g2one.com", "ed@g2one.com"
            bcc "joe@g2one.com"
            title "Hello John"
            attachBytes 'fileName', 'text/plain', 'Hello World'.getBytes("US-ASCII")
            html 'this is some text'
        }
		assertThat(message, instanceOf(MimeMailMessage.class));

        def content = ((MimeMailMessage)message).mimeMessage.content
        assertEquals 2, content.count
        assertEquals 'Hello World', content.getBodyPart(1).inputStream.text
        assertEquals 'Hello World', content.getBodyPart(1).inputStream.text
    }

    void testSendmailWithByteArrayAndResourceAttachments() {
        File tmpFile
        try {
            MailMessage message = mimeCapableMailService.sendMail {
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
        byte[] bytes = getClass().getResource("grailslogo.png").bytes

        MailMessage message = mimeCapableMailService.sendMail {
            multipart true
            to "fred@g2one.com", "ginger@g2one.com"
            from "john@g2one.com"
            title "Hello John"
            text 'this is some text <img src="cid:abc123" />'
            inline 'abc123', 'image/png', bytes
        }
		assertThat(message, instanceOf(MimeMailMessage.class));

        def inlinePart = ((MimeMailMessage)message).mimeMessage.content.getBodyPart(0).content.getBodyPart("<abc123>")
        assert inlinePart.inputStream.bytes == bytes
    }

    void testHtmlContentType() {
        MimeMessage msg = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "test"
            html '<html><head></head><body>How are you?</body></html>'
        }.mimeMessage

        assert msg.contentType.startsWith("text/html")
        assert msg.content == '<html><head></head><body>How are you?</body></html>'
    }

    void testMultipart_html_first() {
        MimeMessage msg = mimeCapableMailService.sendMail {
            multipart true
            to "fred@g2one.com"
            subject "test"
            html '<html><head></head><body>How are you?</body></html>'
            text 'How are you?'
        }.mimeMessage
	
		assertThat(msg.content, instanceOf(MimeMultipart.class));

        MimeMultipart mp = msg.content.getBodyPart(0).content.getBodyPart(0).content

        assert mp.count == 2

        assert mp.getBodyPart(0).contentType.startsWith('text/plain')
        assert mp.getBodyPart(0).content == 'How are you?'

        assert mp.getBodyPart(1).contentType.startsWith('text/html')
        assert mp.getBodyPart(1).content == '<html><head></head><body>How are you?</body></html>'
    }

    void testMultipart_text_first() {
        MimeMessage msg = mimeCapableMailService.sendMail {
            multipart true
            to "fred@g2one.com"
            subject "test"
            text 'How are you?'
            html '<html><head></head><body>How are you?</body></html>'
        }.mimeMessage

		assertThat(msg.content, instanceOf(MimeMultipart.class));

        MimeMultipart mp = msg.content.getBodyPart(0).content.getBodyPart(0).content

        assert mp.count == 2

        assert mp.getBodyPart(0).contentType.startsWith('text/plain')
        assert mp.getBodyPart(0).content == 'How are you?'

        assert mp.getBodyPart(1).contentType.startsWith('text/html')
        assert mp.getBodyPart(1).content == '<html><head></head><body>How are you?</body></html>'
    }

    void testMultipartMode() {
        MimeMessage msg = mimeCapableMailService.sendMail {
            multipart MimeMessageHelper.MULTIPART_MODE_RELATED
            to "fred@g2one.com"
            subject "test"
            text 'How are you?'
            html '<html><head></head><body>How are you?</body></html>'
        }.mimeMessage

        assertThat(msg.content, instanceOf(MimeMultipart.class));
		
		MimeMultipart content = (MimeMultipart) msg.content
		
        assertThat(content.getBodyPart(0), instanceOf(MimeBodyPart.class));
		MimeBodyPart mimeBodyPart = content.getBodyPart(0)

        MimeMultipart mp = mimeBodyPart.content

        assert mp.count == 2
		
		assertThat(mp.getBodyPart(0), instanceOf(MimeBodyPart.class));
        assert ((MimeBodyPart)mp.getBodyPart(0)).contentType.startsWith('text/plain')
        assert ((MimeBodyPart)mp.getBodyPart(0)).content == 'How are you?'
		
		assertThat(mp.getBodyPart(1), instanceOf(MimeBodyPart.class));
        assert ((MimeBodyPart)mp.getBodyPart(1)).contentType.startsWith('text/html')
        assert ((MimeBodyPart)mp.getBodyPart(1)).content == '<html><head></head><body>How are you?</body></html>'
    }

    // http://jira.codehaus.org/browse/GRAILSPLUGINS-2232
    void testContentTypeDoesNotGetChanged() {
        String originalContentType = RequestContextHolder.currentRequestAttributes().currentResponse.contentType

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

    private List<String> to(MimeMessage msg) {
        msg.getRecipients(Message.RecipientType.TO)*.toString()
    }

    private List<String> cc(MimeMessage msg) {
        msg.getRecipients(Message.RecipientType.CC)*.toString()
    }

    private List<String> bcc(MimeMessage msg) {
        msg.getRecipients(Message.RecipientType.BCC)*.toString()
    }
}
class SimpleMailSender implements  MailSender {
    void send(SimpleMailMessage simpleMessage) {}
    void send(SimpleMailMessage[] simpleMessages) {}
}
