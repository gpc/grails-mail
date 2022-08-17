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

package grails.plugins.mail

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import grails.core.GrailsApplication
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.mail.MailMessage
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMailMessage
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.web.context.request.RequestContextHolder
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import javax.mail.Message
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

@Integration
class MailServiceSpec extends Specification  {

    static transactional = false

    MailService mimeCapableMailService
    MailService nonMimeCapableMailService
    @Autowired
    MailMessageContentRenderer mailMessageContentRenderer // autowired
    @Autowired
    GrailsApplication grailsApplication // autowired

	@Shared
    public GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP)


    def setupSpec() {
      greenMail.start()
      Thread.sleep(3000)
    }
    def cleanupSpec() {
      greenMail.stop()
    }
    def setup() {
        MailSender mimeMailSender = new JavaMailSenderImpl(host: "localhost", port: ServerSetupTest.SMTP.port)
        MailMessageBuilderFactory mimeMessageBuilderFactor = new MailMessageBuilderFactory(
            mailSender: mimeMailSender,
            mailMessageContentRenderer: mailMessageContentRenderer
        )
        mimeCapableMailService = new MailService(
            mailMessageBuilderFactory: mimeMessageBuilderFactor,
                mailConfigurationProperties: new MailConfigurationProperties())
        mimeCapableMailService.afterPropertiesSet()

        MailSender simpleMailSender = new SimpleMailSender()
        MailMessageBuilderFactory simpleMessageBuilderFactory = new MailMessageBuilderFactory(
            mailSender: simpleMailSender,
            mailMessageContentRenderer: mailMessageContentRenderer
        )
        nonMimeCapableMailService = new MailService(
            mailMessageBuilderFactory: simpleMessageBuilderFactory,
                mailConfigurationProperties: new MailConfigurationProperties())
        nonMimeCapableMailService.afterPropertiesSet()
    }

    def cleanup() {
        mimeCapableMailService.destroy()
        nonMimeCapableMailService.destroy()
        greenMail.reset()
    }

    void testSendSimpleMessage() {
        when:
        MailMessage message = nonMimeCapableMailService.sendMail {
            to "fred@g2one.com"
            title "Hello John"
            body 'this is some text'
            from 'king@g2one.com'
        }
        then:
         message instanceof SimpleMailMessage
        "Hello John" == ((SimpleMailMessage)message).getSubject()
        'this is some text'  ==message.getText()
        'fred@g2one.com' == message.to[0]
        'king@g2one.com' == message.from
    }

    void testAsyncSendSimpleMessage() {
      when:
        MailMessage message = nonMimeCapableMailService.sendMail {
          async true
          to "fred@g2one.com"
          title "Hello John"
          body 'this is some text'
          from 'king@g2one.com'
        }
      then:
        message instanceof SimpleMailMessage
        "Hello John" == ((SimpleMailMessage)message).getSubject()
        'this is some text' == message.getText()
        'fred@g2one.com' == message.to[0]
        'king@g2one.com' == message.from
    }

    void testSendToMultipleRecipients() {
      when:
        MailMessage message = nonMimeCapableMailService.sendMail {
          to "fred@g2one.com", "ginger@g2one.com"
          title "Hello John"
          body 'this is some text'
        }
      then:
        message.subject == "Hello John"
        message.text == "this is some text"
        message.to[0] == "fred@g2one.com"
        message.to[1] == "ginger@g2one.com"
    }




    void testSendToMultipleRecipientsUsingList() {
      when:
        MailMessage message = nonMimeCapableMailService.sendMail {
            to(["fred@g2one.com", "ginger@g2one.com"])
            title "Hello John"
            body 'this is some text'
        }
      then:
        message.getTo()[0] == "fred@g2one.com"
        message.getTo()[1] == "ginger@g2one.com"
    }

    void testSendToMultipleCCRecipientsUsingList() {
      when:
        MailMessage message = nonMimeCapableMailService.sendMail {
            to 'joe@g2one.com'
            cc(["fred@g2one.com", "ginger@g2one.com"])
            title "Hello John"
            body 'this is some text'
        }
      then:
        "fred@g2one.com" == message.cc[0]
        "ginger@g2one.com" == message.cc[1]
    }

    void testSendToMultipleBCCRecipientsUsingList() {
      when:
        MailMessage message = nonMimeCapableMailService.sendMail {
            to("joe@g2one.com")
            bcc(["fred@g2one.com", "ginger@g2one.com"])
            title "Hello John"
            body 'this is some text'
        }
      then:
        "fred@g2one.com" == message.bcc[0]
        "ginger@g2one.com" == message.bcc[1]
    }

    void testSendToMultipleRecipientsAndCC() {
      when:
        MailMessage message = nonMimeCapableMailService.sendMail {
            to "fred@g2one.com", "ginger@g2one.com"
            from "john@g2one.com"
            cc "marge@g2one.com", "ed@g2one.com"
            bcc "joe@g2one.com"
            title "Hello John"
            body 'this is some text'
        }
      then:
        message instanceof SimpleMailMessage
        "Hello John" == ((SimpleMailMessage)message).getSubject()
        message.text == 'this is some text'
        message.to.size() == 2
        message.getTo()[0] == "fred@g2one.com"
        message.getTo()[1] == "ginger@g2one.com"
        message.from == "john@g2one.com"
        message.getText() == 'this is some text'
        message.to.size() == 2
        message.getTo()[0] == "fred@g2one.com"
        message.getTo()[1] == "ginger@g2one.com"
        message.from == "john@g2one.com"
        message.cc.size() == 2
        message.cc[0] == "marge@g2one.com"
        message.cc[1] == "ed@g2one.com"
    }

    void testSendMailWithEnvelopeFrom() {
      given:

        def message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            title "Hello John"
            body 'this is some text'
            from 'king@g2one.com'
            envelopeFrom 'peter@g2one.com'
        }
      when:
        def msg = message.mimeMessage
        def greenMsg = greenMail.getReceivedMessages()[0]
      then:
         msg.getSubject() == "Hello John"
         msg.getFrom()[0].toString() == "king@g2one.com"
         greenMsg.getHeader("Return-Path")[0] == "<peter@g2one.com>"
    }

    void testSendMailWithEnvelopeFromAndBasicMailSender() {
        when:
            nonMimeCapableMailService.sendMail {
                to "fred@g2one.com"
                title "Hello John"
                body 'this is some text'
                from 'king@g2one.com'
                envelopeFrom 'peter@g2one.com'
            }
        then:
          thrown GrailsMailException

    }

    void testSendHtmlMail() {
      when:
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            html '<b>Hello</b> World'
        }
      then:
        message.getMimeMessage().getSubject() == "Hello John"
        message.mimeMessage.contentType.startsWith('text/html')
        message.getMimeMessage().getContent() == '<b>Hello</b> World'
    }

    void testSendMailView() {
      when:
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body(view: '/_testemails/test', model: [msg: 'hello'])
        }
      then:
        message.getMimeMessage().getSubject() == "Hello John"
        message.mimeMessage.contentType.startsWith('text/plain')
        message.getMimeMessage().getContent().trim() == 'Message is: hello'
    }

    void testSendMailViewText() {
      when:
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            text view: '/_testemails/test', model: [msg: 'hello']
        }
      then:
      message.mimeMessage.contentType.startsWith('text/plain')
        message.getMimeMessage().getContent().trim() == 'Message is: hello'
    }

    void testSendMailViewHtmlMethod() {
      when:
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            html view: '/_testemails/testhtml', model: [msg: 'hello']
        }
      then:
        message.getMimeMessage().getSubject() == "Hello John"
        message.mimeMessage.contentType.startsWith('text/html')
        message.getMimeMessage().getContent().trim() == '<b>Message is: hello</b>'
    }

    void testSendMailViewHTML() {
      when:
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body(view: '/_testemails/testhtml', model: [msg: 'hello'])
        }
      then:
         message.getMimeMessage().getSubject() == "Hello John"
         message.mimeMessage.contentType.startsWith('text/html')
         message.getMimeMessage().getContent().trim() == '<b>Message is: hello</b>'
    }

    void testSendMailViewWithTags() {
      when:
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body(view: '/_testemails/tagtest', model: [condition: true])
        }
        MimeMailMessage message2 = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body(view: '/_testemails/tagtest', model: [condition: false])
        }
      then:

        message.getMimeMessage().getSubject() == "Hello John"
        message.getMimeMessage().getContent().trim() == 'Condition is true'
        message2.getMimeMessage().getSubject() == "Hello John"
        message2.getMimeMessage().getContentType()?.startsWith('text/plain')
        message2.getMimeMessage().getContent().trim() == ''
    }

    void testSendMailViewNoModel() {
      when:
        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body(view: '/_testemails/test')
        }
      then:
        message.getMimeMessage().getSubject() == "Hello John"
        message.getMimeMessage().getContentType()?.startsWith('text/plain')
        message.getMimeMessage().getContent().trim() == 'Message is:'
    }

    /**
     * Tests the "headers" feature of the mail DSL. It should add the
     * specified headers to the underlying MIME message.
     */
    void testSendMailWithHeaders() {
      when:
        MailMessage message = mimeCapableMailService.sendMail {
            headers "X-Mailing-List": "user@grails.codehaus.org",
                "Sender": "dilbert@somewhere.org"
            to "fred@g2one.com"
            subject "Hello Fred"
            body 'How are you?'
        }
        MimeMessage msg = ((MimeMailMessage)message).mimeMessageHelper.mimeMessage
      then:
        message instanceof MimeMailMessage

        msg.getHeader("X-Mailing-List")[0] == "user@grails.codehaus.org"
        msg.getHeader("Sender")[0] == "dilbert@somewhere.org"
        to(msg) == ["fred@g2one.com"]
        msg.subject == "Hello Fred"
        msg.content == "How are you?"
    }

    /**
     * Tests that the builder throws an exception if the user tries to
     * specify custom headers with just a plain MailSender.
     */
    void testSendMailWithHeadersAndBasicMailSender() {
        when:
          nonMimeCapableMailService.sendMail {
              headers "Content-Type": "text/plain;charset=UTF-8",
                  "Sender": "dilbert@somewhere.org"
              to "fred@g2one.com"
              subject "Hello Fred"
              body 'How are you?'
          }
        then:
          thrown GrailsMailException
    }

    void testSendmailWithTranslations() {
      when:
        MailMessage message = mimeCapableMailService.sendMail {
            from 'neur0maner@gmail.com'
            to "neur0maner@gmail.com"
            locale 'en_US'
            subject "Hello"
            body(view: '/_testemails/i18ntest', model: [name: 'Luis'])
        }
        MailMessage message2 = mimeCapableMailService.sendMail {
            from 'neur0maner@gmail.com'
            to "neur0maner@gmail.com"
            locale Locale.FRENCH
            subject "Hello"
            body(view: '/_testemails/i18ntest', model: [name: 'Luis'])
        }
        MimeMessage msg = ((MimeMailMessage)message).mimeMessageHelper.mimeMessage
        MimeMessage msg2 = ((MimeMailMessage)message2).mimeMessageHelper.mimeMessage
        final def slurper = new XmlSlurper()
        def html = slurper.parseText(msg.content)
        def html2 = slurper.parseText(msg2.content)
      then:
        html.body.toString() == 'Translate this: Luis'
        html2.body.toString() == 'Traduis ceci: Luis'
    }

    void testSendmailWithByteArrayAttachment() {
      when:
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
        def content = ((MimeMailMessage)message).mimeMessage.content
      then:
        content.count == 2
        content.getBodyPart(1).inputStream.text == 'Hello World'
    }

    void testSendmailWithByteArrayAndResourceAttachments() {
      given:
        File tmpFile
      when:
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
      then:
        content.count == 3
        content.getBodyPart(1).inputStream.text == 'Dear John'
        content.getBodyPart(2).inputStream.text == 'Hello World'
      cleanup:
        tmpFile?.delete()
    }

    void testInlineAttachment() {
      when:

        byte[] bytes = new File("src/integration-test/groovy/grails/plugins/mail/grailslogo.png").bytes

        MailMessage message = mimeCapableMailService.sendMail {
            multipart true
            to "fred@g2one.com", "ginger@g2one.com"
            from "john@g2one.com"
            title "Hello John"
            text 'this is some text <img src="cid:abc123" />'
            inline 'abc123', 'image/png', bytes
        }
        def inlinePart = ((MimeMailMessage)message).mimeMessage.content.getBodyPart(0).content.getBodyPart("<abc123>")
      then:
        inlinePart.inputStream.bytes == bytes
    }

    void testHtmlContentType() {
      when:
        MimeMessage msg = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "test"
            html '<html><head></head><body>How are you?</body></html>'
        }.mimeMessage
      then:
      msg.contentType.startsWith("text/html")
        msg.content == '<html><head></head><body>How are you?</body></html>'
    }

    void testMultipart_html_first() {
      when:
        MimeMessage msg = mimeCapableMailService.sendMail {
            multipart true
            to "fred@g2one.com"
            subject "test"
            html '<html><head></head><body>How are you?</body></html>'
            text 'How are you?'
        }.mimeMessage
        MimeMultipart mp = msg.content.getBodyPart(0).content.getBodyPart(0).content
      then:
        mp.count == 2
        mp.getBodyPart(0).contentType.startsWith('text/plain')
        mp.getBodyPart(0).content == 'How are you?'
        mp.getBodyPart(1).contentType.startsWith('text/html')
        mp.getBodyPart(1).content == '<html><head></head><body>How are you?</body></html>'
    }

    void testMultipart_text_first() {
      when:
        MimeMessage msg = mimeCapableMailService.sendMail {
            multipart true
            to "fred@g2one.com"
            subject "test"
            text 'How are you?'
            html '<html><head></head><body>How are you?</body></html>'
        }.mimeMessage
        MimeMultipart mp = msg.content.getBodyPart(0).content.getBodyPart(0).content
      then:
        mp.count == 2
        mp.getBodyPart(0).contentType.startsWith('text/plain')
        mp.getBodyPart(0).content == 'How are you?'
        mp.getBodyPart(1).contentType.startsWith('text/html')
        mp.getBodyPart(1).content == '<html><head></head><body>How are you?</body></html>'
    }

    void testMultipartMode() {
        when:
        MimeMessage msg = mimeCapableMailService.sendMail {
            multipart MimeMessageHelper.MULTIPART_MODE_RELATED
            to "fred@g2one.com"
            subject "test"
            text 'How are you?'
            html '<html><head></head><body>How are you?</body></html>'
        }.mimeMessage

        then:
        msg.content instanceof MimeMultipart

        and:
        MimeMultipart content = (MimeMultipart) msg.content

        content.getBodyPart(0) instanceof MimeBodyPart

        and:
        MimeBodyPart mimeBodyPart = content.getBodyPart(0)
        MimeMultipart mp = mimeBodyPart.content

        mp.count == 2
        mp.getBodyPart(0) instanceof MimeBodyPart
        ((MimeBodyPart) mp.getBodyPart(0)).contentType.startsWith('text/plain')
        ((MimeBodyPart) mp.getBodyPart(0)).content == 'How are you?'

        mp.getBodyPart(1) instanceof MimeBodyPart
        ((MimeBodyPart) mp.getBodyPart(1)).contentType.startsWith('text/html')
        ((MimeBodyPart) mp.getBodyPart(1)).content == '<html><head></head><body>How are you?</body></html>'
    }

    @Ignore('Not possible to get the currentResponse')
    void testContentTypeDoesNotGetChanged() {
        when:
        String originalContentType = RequestContextHolder.currentRequestAttributes().currentResponse.contentType

        MimeMailMessage message = mimeCapableMailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body(view: '/_testemails/test', model: [msg: 'hello'])
        }
        then:
        message.getMimeMessage().getSubject() == "Hello John"
        message.mimeMessage.contentType.startsWith('text/plain') == true
        message.getMimeMessage().getContent().trim() == 'Message is: hello'
        RequestContextHolder.currentRequestAttributes().currentResponse.contentType == originalContentType
    }

//    void testViewResolutionFromPlugin() {
//        MimeMailMessage message = mimeCapableMailService.sendMail {
//            to "fred@g2one.com"
//            subject "Hello John"
//            body view: '/email/email', plugin: 'for-plugin-view-resolution'
//        }
//
//        message.getMimeMessage().getSubject() == "Hello John"
//        message.mimeMessage.contentType.startsWith('text/plain') == true
//        assertEquals 'This is from a plugin!!!', message.getMimeMessage().getContent().trim()
//    }

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
