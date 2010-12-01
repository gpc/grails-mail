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

package org.grails.mail

import javax.mail.internet.MimeMessage
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.MailSender
import org.springframework.mail.MailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMailMessage
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.springframework.web.context.support.WebApplicationContextUtils
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import javax.mail.Message
import org.springframework.core.io.FileSystemResource


class MailServiceTests extends GroovyTestCase {

    static transactional = false

    MailSender mailSender


    void testSendSimpleMessage() {
        def mailService = new MailService()


        def message = mailService.sendMail {
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
        def mailService = new MailService()

        def message = mailService.sendMail {
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
        def mailService = new MailService()

        def message = mailService.sendMail {
            to(["fred@g2one.com", "ginger@g2one.com"])
            title "Hello John"
            body 'this is some text'
        }
        assertEquals "fred@g2one.com", message.getTo()[0]
        assertEquals "ginger@g2one.com", message.getTo()[1]
    }

    void testSendToMultipleCCRecipientsUsingList() {
        def mailService = new MailService()

        def message = mailService.sendMail {
            to 'joe@g2one.com'
            cc(["fred@g2one.com", "ginger@g2one.com"])
            title "Hello John"
            body 'this is some text'
        }
        assertEquals "fred@g2one.com", message.cc[0]
        assertEquals "ginger@g2one.com", message.cc[1]
    }

    void testSendToMultipleBCCRecipientsUsingList() {
        def mailService = new MailService()

        def message = mailService.sendMail {
            to("joe@g2one.com")
            bcc(["fred@g2one.com", "ginger@g2one.com"])
            title "Hello John"
            body 'this is some text'
        }
        assertEquals "fred@g2one.com", message.bcc[0]
        assertEquals "ginger@g2one.com", message.bcc[1]
    }

    void testSendToMultipleRecipientsAndCC() {
        def mailService = new MailService()

        def message = mailService.sendMail {
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
        def mailService = new MailService()

        // stub send method
        MailSender.metaClass.send = { SimpleMailMessage smm -> }
        JavaMailSender.metaClass.send = { MimeMessage mm -> }

        mailService.mailSender = mailSender

        MimeMailMessage message = mailService.sendMail {
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
        def mailService = new MailService()

        def ctx = RequestContextHolder.currentRequestAttributes().servletContext
        def applicationContext = ctx[GrailsApplicationAttributes.APPLICATION_CONTEXT]

        mailService.groovyPagesTemplateEngine = applicationContext.getBean(GroovyPagesTemplateEngine.BEAN_ID)

        // stub send method
        MailSender.metaClass.send = { SimpleMailMessage smm -> }
        JavaMailSender.metaClass.send = { MimeMessage mm -> }

        mailService.mailSender = mailSender

        MimeMailMessage message = mailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body(view: '/_testemails/test', model: [msg: 'hello'])
        }

        assertEquals "Hello John", message.getMimeMessage().getSubject()
        assertTrue message.mimeMessage.contentType.startsWith('text/plain')
        assertEquals 'Message is: hello', message.getMimeMessage().getContent().trim()
    }

    void testSendMailViewHTML() {
        def mailService = new MailService()

        def ctx = RequestContextHolder.currentRequestAttributes().servletContext
        def applicationContext = ctx[GrailsApplicationAttributes.APPLICATION_CONTEXT]

        mailService.groovyPagesTemplateEngine = applicationContext.getBean(GroovyPagesTemplateEngine.BEAN_ID)

        // stub send method
        MailSender.metaClass.send = { SimpleMailMessage smm -> }
        JavaMailSender.metaClass.send = { MimeMessage mm -> }

        mailService.mailSender = mailSender

        MimeMailMessage message = mailService.sendMail {
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
        def mailService = new MailService()

        def ctx = RequestContextHolder.currentRequestAttributes().servletContext
        def applicationContext = ctx[GrailsApplicationAttributes.APPLICATION_CONTEXT]

        mailService.groovyPagesTemplateEngine = applicationContext.getBean(GroovyPagesTemplateEngine.BEAN_ID)

        // stub send method
        MailSender.metaClass.send = { SimpleMailMessage smm -> }
        JavaMailSender.metaClass.send = { MimeMessage mm -> }

        mailService.mailSender = mailSender

        MimeMailMessage message = mailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body(view: '/_testemails/tagtest', model: [condition: true])
        }

        assertEquals "Hello John", message.getMimeMessage().getSubject()
        assertEquals 'Condition is true', message.getMimeMessage().getContent().trim()

        message = mailService.sendMail {
            to "fred@g2one.com"
            subject "Hello John"
            body(view: '/_testemails/tagtest', model: [condition: false])
        }

        assertEquals "Hello John", message.getMimeMessage().getSubject()
        assertTrue message.getMimeMessage().getContentType()?.startsWith('text/plain')
        assertEquals '', message.getMimeMessage().getContent().trim()
    }

    void testSendMailViewNoModel() {
        def mailService = new MailService()

        def ctx = RequestContextHolder.currentRequestAttributes().servletContext
        def applicationContext = ctx[GrailsApplicationAttributes.APPLICATION_CONTEXT]

        mailService.groovyPagesTemplateEngine = applicationContext.getBean(GroovyPagesTemplateEngine.BEAN_ID)

        // stub send method
        MailSender.metaClass.send = { SimpleMailMessage smm -> }
        JavaMailSender.metaClass.send = { MimeMessage mm -> }

        mailService.mailSender = mailSender

        MimeMailMessage message = mailService.sendMail {
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
        MailSender.metaClass.send = { SimpleMailMessage smm -> }
        JavaMailSender.metaClass.send = { MimeMessage mm -> }

        def mailService = new MailService()
        mailService.mailSender = mailSender

        def message = mailService.sendMail {
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
        MailSender.metaClass.send = { SimpleMailMessage smm -> }
        JavaMailSender.metaClass.send = { MimeMessage mm -> }

        def mailService = new MailService()

        shouldFail(GrailsMailException) {
            mailService.sendMail {
                headers "Content-Type": "text/plain;charset=UTF-8",
                    "Sender": "dilbert@somewhere.org"
                to "fred@g2one.com"
                subject "Hello Fred"
                body 'How are you?'
            }
        }
    }

    void testSendmailWithTranslations() {
        MailSender.metaClass.send = { SimpleMailMessage smm -> }
        JavaMailSender.metaClass.send = { MimeMessage mm -> }

        def mailService = new MailService()
        mailService.mailSender = mailSender
        def ctx = RequestContextHolder.currentRequestAttributes().servletContext
        def applicationContext = ctx[GrailsApplicationAttributes.APPLICATION_CONTEXT]
        mailService.groovyPagesTemplateEngine = applicationContext.getBean(GroovyPagesTemplateEngine.BEAN_ID)



        def message = mailService.sendMail {
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

        message = mailService.sendMail {
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

    void testSendmailWithAttachments() {
        MailSender.metaClass.send = { SimpleMailMessage smm -> }
        JavaMailSender.metaClass.send = { MimeMessage mm -> }
        def mailService = new MailService()
        mailService.mailSender = mailSender

        def message = mailService.sendMail {
            multipart true

            to "fred@g2one.com", "ginger@g2one.com"
            from "john@g2one.com"
            cc "marge@g2one.com", "ed@g2one.com"
            bcc "joe@g2one.com"
            title "Hello John"
            attachBytes('fileName','application/octet-stream','Hello World'.bytes)
            html 'this is some text'
        }
        def content=message.mimeMessage.content
        assertEquals 2, content.count
        assertEquals 'Hello World',content.getBodyPart(1).inputStream.text


        def tmpFile
        try {                
            message = mailService.sendMail {
                multipart true

                tmpFile=File.createTempFile('testSendmailWithAttachments',null)
                tmpFile << 'Hello World'

                to "fred@g2one.com", "ginger@g2one.com"
                from "john@g2one.com"
                cc "marge@g2one.com", "ed@g2one.com"
                bcc "joe@g2one.com"
                title "Hello John"
                attachResource('fileName','application/octet-stream',new FileSystemResource(tmpFile))
                html 'this is some text'
            }
            content=message.mimeMessage.content
            assertEquals 2, content.count
            assertEquals 'Hello World',content.getBodyPart(1).inputStream.text
        } finally {
            tmpFile?.delete()
        }
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

