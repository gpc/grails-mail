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

import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import javax.mail.internet.MimeMessage
import org.springframework.mail.MailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMailMessage

/**
 *
 * @author Graeme Rocher
 */
class MailService {

    static transactional = false

    MailSender mailSender

    MailMessage sendMail(Closure callable) {
        def messageBuilder = new MailMessageBuilder(mailSender)
        callable.delegate = messageBuilder
        callable.resolveStrategy = Closure.DELEGATE_FIRST
        callable.call()

        def message = messageBuilder.createMessage()
        message.sentDate = new Date()
        sendMail message
        return message
    }

    protected sendMail(message) {
        if(message) {
            if(message instanceof MimeMailMessage) {
                MimeMailMessage msg = message
                if(mailSender instanceof JavaMailSender) {
                    mailSender.send((MimeMessage)msg.getMimeMessage())
                }
                else {
                    throw new GrailsMailException("MimeMessages require an instance of 'org.springframework.mail.javamail.JavaMailSender' to be configured!")
                }
            }
            else {
                mailSender?.send(message)
            }
        }
    }

}

class MailMessageBuilder {
    private MailMessage message

    MailSender mailSender

    MailMessageBuilder(MailSender mailSender) {
        this.mailSender = mailSender
    }

    private MailMessage getMessage() {
        if(!message) {
            if(mailSender instanceof JavaMailSender) {
                message = new MimeMailMessage(mailSender.createMimeMessage() )
            }
            else {
                message = new SimpleMailMessage()
            }
        }
        return message
    }

    MailMessage createMessage() { getMessage() }

    void to(String recip) {
        if(recip) {
            getMessage().to = [recip] as String[]
        }
    }
    void to(Object[] args) {
        if(args) {
            getMessage().to = args as String[]
        }
    }
    void title(title) {
        subject(title)
    }
    void subject(title) {
        getMessage().subject = title?.toString()
    }
    void body(body) {
        text(body)
    }
    void text(body) {
        getMessage().text = body?.toString()
    }
    void html(text) {
        def msg = getMessage()
        if(msg instanceof MimeMailMessage) {
            MimeMailMessage mm = msg
            mm.getMimeMessageHelper().setText(text, true)
        }
    }
    void bcc(String bcc) {
        getMessage().bcc = [bcc] as String[]
    }
    void bcc(Object[] args) {
        getMessage().bcc = args as String[]
    }
    void cc(String cc) {
        getMessage().cc = [cc] as String[]
    }
    void cc(Object[] args) {
        getMessage().cc = args as String[]
    }
    void replyTo(String replyTo) {
        getMessage().replyTo = replyTo
    }
    void from(String from) {
        getMessage().from = from
    }


}