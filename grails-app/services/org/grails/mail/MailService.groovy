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

package org.grails.mail

import javax.mail.internet.MimeMessage
import org.springframework.mail.MailMessage
import org.springframework.mail.MailSender
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMailMessage

/**
 *
 * @author Graeme Rocher
 */
class MailService {

    static transactional = false

    def groovyPagesTemplateEngine
    
    MailSender mailSender
    
    MailMessage sendMail(Closure callable) {
        def messageBuilder = new MailMessageBuilder(this, mailSender)
        callable.delegate = messageBuilder
        callable.resolveStrategy = Closure.DELEGATE_FIRST
        callable.call()

        def message = messageBuilder.createMessage()
        initMessage(message)
        sendMail message
        return message
    }

    protected initMessage(message) {
        message.sentDate = new Date()
    }

    protected sendMail(message) {
        if(message) {
            if(message instanceof MimeMailMessage) {
                MimeMailMessage msg = message
                if(mailSender instanceof JavaMailSender) {
                    mailSender.send((MimeMessage)msg.getMimeMessage())
                    if (log.traceEnabled) log.trace("Sent mail re: [${message.subject}] from [${message.from}] to [${message.to}]")
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
