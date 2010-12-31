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

import org.springframework.mail.MailMessage
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.*
import org.springframework.core.io.*

import org.apache.commons.logging.LogFactory

/**
 * The builder that implements the mail DSL.
 */
class MailMessageBuilder {

    private MailMessage message
    private Locale locale

    private log = LogFactory.getLog(MailMessageBuilder)
    
    MailSender mailSender
    MailService mailService
    boolean multipart = false // by default, we're sending non-multipart emails

    MailMessageBuilder(MailService svc, MailSender mailSender) {
        this.mailSender = mailSender
        this.mailService = svc
    }

    private MailMessage getMessage() {
        if(!message) {
            if(mailSender instanceof JavaMailSender) {
                def helper = new MimeMessageHelper(mailSender.createMimeMessage(), multipart)
                message = new MimeMailMessage(helper)
            }
            else {
                message = new SimpleMailMessage()
            }
            message.from = mailService.defaultFrom
            if (mailService.overrideAddress)
                message.from = mailService.overrideAddress
        }
        return message
    }

    MailMessage createMessage() { getMessage() }


    void multipart(boolean multipart) {
        this.multipart = multipart
    }

    void to(recip) {
        if(recip) {
            if (mailService.overrideAddress)
                recip = mailService.overrideAddress
            getMessage().setTo([recip.toString()] as String[])
        }
    }

    void attachBytes(String fileName, String contentType, byte[] bytes) {
        attachResource(fileName, contentType, new ByteArrayResource(bytes))
    }

    void attachResource(String fileName, String contentType, Resource res) {
        def msg = getMessage()
        if(msg instanceof MimeMailMessage) {
            assert multipart, "message is not marked as 'multipart'; use 'multipart true' as the first line in your builder DSL"
            msg.mimeMessageHelper.addAttachment(fileName, res, contentType)
        }
        else {
            throw new IllegalStateException("Message is not an instance of org.springframework.mail.javamail.MimeMessage, cannot attach bytes!")
        }
    }

    void to(Object[] args) {
        if(args) {
            if (mailService.overrideAddress)
               args = args.collect { mailService.overrideAddress }.toArray()

            getMessage().setTo((args.collect { it?.toString() }) as String[])
        }
    }
    void to(List args) {
        if(args) {
            if (mailService.overrideAddress)
               args = args.collect { mailService.overrideAddress }   
            getMessage().setTo((args.collect { it?.toString() }) as String[])
        }
    }
    void title(title) {
        subject(title)
    }
    void subject(title) {
        getMessage().subject = title?.toString()
    }
    void headers(Map hdrs) {
        def msg = getMessage()

        // The message must be of type MimeMailMessage to add headers.
        if (!(msg instanceof MimeMailMessage)) {
            throw new GrailsMailException("You must use a JavaMailSender to customise the headers.")
        }

        msg = msg.mimeMessageHelper.mimeMessage
        hdrs.each { name, value ->
            msg.setHeader(name.toString(), value?.toString())
        }
    }
    void body(body) {
        text(body)
    }
    void body(Map params) {
        if (params.view) {
            // Here need to render it first, establish content type of virtual response / contentType model param
            def render = createContentRenderer().render(new StringWriter(), params.view, params.model, locale, params.plugin)
            
            if (render.html) {
                html(render.out.toString()) // @todo Spring mail helper will not set correct mime type if we give it XHTML
            } else {
                text(render.out.toString())
            }
        } 
    }
    
    protected createContentRenderer() {
        new MailMessageContentRenderer(mailService)
    }
    
    void text(body) {
        getMessage().text = body?.toString()
    }
    void html(text) {
        def msg = getMessage()
        if(msg instanceof MimeMailMessage) {
            MimeMailMessage mm = msg
            mm.getMimeMessageHelper().setText(text?.toString(), true)
        }
    }
    void bcc(bcc) {
        if (mailService.overrideAddress)
            bcc = mailService.overrideAddress
    
        getMessage().setBcc([bcc?.toString()] as String[])
    }
    void bcc(Object[] args) {
        if (mailService.overrideAddress)
           args = args.collect { mailService.overrideAddress }.toArray()
    
        getMessage().setBcc((args.collect { it?.toString() }) as String[])
    }
    void bcc(List args) {
        if (mailService.overrideAddress)
           args = args.collect { mailService.overrideAddress }
    
        getMessage().setBcc((args.collect { it?.toString() }) as String[])
    }
    void cc(cc) {
        if (mailService.overrideAddress)
            cc = mailService.overrideAddress
    
        getMessage().setCc([cc?.toString()] as String[])
    }
    void cc(Object[] args) {
        if (mailService.overrideAddress)
           args = args.collect { mailService.overrideAddress }.toArray()
    
        getMessage().setCc((args.collect { it?.toString() }) as String[])
    }
    void cc(List args) {
        if (mailService.overrideAddress)
           args = args.collect { mailService.overrideAddress }
    
        getMessage().setCc((args.collect { it?.toString() }) as String[])
    }

    void replyTo(replyTo) {
        getMessage().replyTo = replyTo?.toString()
    }
    void from(from) {
        getMessage().from = from?.toString()
    }
    void locale(String localeStr) {
        def split=localeStr.split('_')
        String language=split[0]
        if (split.length>1) {
            String country=split[1]
            locale=new Locale(language,country)
        } else {
            locale=new Locale(language)
        }
    }

    void locale(Locale locale) {
        this.locale=locale
    }
    
}
