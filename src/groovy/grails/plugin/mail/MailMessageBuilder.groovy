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

import org.springframework.mail.MailMessage
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.*
import org.springframework.core.io.*

import org.apache.commons.logging.LogFactory

import javax.mail.Message
import javax.mail.internet.MimeUtility

/**
 * Provides a DSL style interface to mail message sending/generation.
 * 
 * If the builder is constructed without a MailMessageContentRenderer, it is incapable
 * of rendering GSP views into message content.
 */
class MailMessageBuilder {

    private log = LogFactory.getLog(MailMessageBuilder)
    
    final MailSender mailSender
    final MailMessageContentRenderer mailMessageContentRenderer
    
    final String defaultFrom
    final String defaultTo
    final String overrideAddress
    
    private MailMessage message
    private Locale locale
    
    private String textContent = null
    private String htmlContent = null
    
    private multipart = false // by default, we're sending non-multipart emails
    
    private List<Inline> inlines = []
    
    static private class Inline {
        String id
        String contentType
        def toAdd
    }

    MailMessageBuilder(MailSender mailSender, ConfigObject config, MailMessageContentRenderer mailMessageContentRenderer = null) {
        this.mailSender = mailSender
        this.mailMessageContentRenderer = mailMessageContentRenderer
        
        this.overrideAddress = config.overrideAddress ?: null
        this.defaultFrom = overrideAddress ?: (config.default.from ?: null)
        this.defaultTo = overrideAddress ?: (config.default.to ?: null)
    }

    private MailMessage getMessage() {
        if (!message) {
            if (mimeCapable) {
                def helper = new MimeMessageHelper(mailSender.createMimeMessage(), multipart)
                message = new MimeMailMessage(helper)
            } else {
                message = new SimpleMailMessage()
            }
            
            message.from = defaultFrom

            if (defaultTo) {
                message.setTo(defaultTo)
            }
        }
        
        message
    }

    MailMessage sendMessage() {
        def message = finishMessage()
        
        if (log.traceEnabled) {
            log.trace("Sending mail ${getDescription(message)}} ...")
        }
        
        mailSender.send(message instanceof MimeMailMessage ? message.mimeMessage : message)
        
        if (log.traceEnabled) {
            log.trace("Sent mail ${getDescription(message)}} ...")
        }
        
        message
    }
    
    void multipart(boolean multipart) {
        this.multipart = multipart
    }
    
    void multipart(int multipartMode) {
        this.multipart = multipartMode
    }
    
    void headers(Map hdrs) {
        // The message must be of type MimeMailMessage to add headers.
        if (!mimeCapable) {
            throw new GrailsMailException("You must use a JavaMailSender to customise the headers.")
        }

        def msg = getMessage()
        msg = msg.mimeMessageHelper.mimeMessage
        hdrs.each { name, value ->
            msg.setHeader(name.toString(), value?.toString())
        }
    }
    
    void to(Object[] args) {
        getMessage().setTo(toDestinationAddresses(args))
    }
    
    void to(List args) {
        to(*args)
    }
    
    void bcc(Object[] args) {
        getMessage().setBcc(toDestinationAddresses(args))
    }
    
    void bcc(List args) {
        bcc(*args)
    }
        
    void cc(Object[] args) {
        getMessage().setCc(toDestinationAddresses(args))
    }
    
    void cc(List args) {
        cc(*args)
    }

    void replyTo(replyTo) {
        getMessage().replyTo = replyTo?.toString()
    }
    
    void from(from) {
        getMessage().from = from?.toString()
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
    
    void body(Map params) {
        def render = doRender(params)
    
        if (render.html) {
            html(render.out.toString()) // @todo Spring mail helper will not set correct mime type if we give it XHTML
        } else {
            text(render.out.toString())
        }
    }
    
    protected MailMessageContentRender doRender(Map params) {
        if (mailMessageContentRenderer == null) {
            throw new GrailsMailException("mail message builder was constructed without a message content render so cannot render views")
        }
        
        if (!params.view) {
            throw new GrailsMailException("no view specified")
        }
        
        mailMessageContentRenderer.render(new StringWriter(), params.view, params.model, locale, params.plugin)
    }
    
    void text(Map params) {
        text(doRender(params).out.toString())
    }
    
    void text(CharSequence text) {
        textContent = text.toString()
    }

    void html(Map params) {
        html(doRender(params).out.toString())
    }
    
    void html(CharSequence text) {
        if (mimeCapable) {
            htmlContent = text.toString()
        } else {
            throw new GrailsMailException("mail sender is not mime capable, try configuring a JavaMailSender")
        }
    }
    
    void locale(String localeStr) {
        locale(new Locale(*localeStr.split('_', 3)))
    }

    void locale(Locale locale) {
        this.locale = locale
    }

    /**
     * @deprecated use attach(String, String, byte[])
     */
    void attachBytes(String fileName, String contentType, byte[] bytes) {
        attach(fileName, contentType, bytes)
    }

    void attach(String fileName, String contentType, byte[] bytes) {
        attach(fileName, contentType, new ByteArrayResource(bytes))
    }
    
    void attach(String fileName, String contentType, InputStreamSource source) {
        doAdd(fileName, contentType, source, true)
    }
    
    void inline(String contentId, String contentType, byte[] bytes) {
        inline(contentId, contentType, new ByteArrayResource(bytes))
    }
    
    void inline(String contentId, String contentType, InputStreamSource source) {
        inlines << new Inline(id: contentId, contentType: contentType, toAdd: source)
    }
    
    protected doAdd(String id, String contentType, toAdd, boolean isAttachment) {
        if (!mimeCapable) {
            throw new GrailsMailException("Message is not an instance of org.springframework.mail.javamail.MimeMessage, cannot attach bytes!")
        }
        
        assert multipart, "message is not marked as 'multipart'; use 'multipart true' as the first line in your builder DSL"

        def helper = getMessage().mimeMessageHelper
        if (isAttachment) {
            helper.addAttachment(MimeUtility.encodeWord(id), toAdd, contentType)
        } else {
            helper.addInline(MimeUtility.encodeWord(id), toAdd, contentType)
        }
    }
    
    boolean isMimeCapable() {
        mailSender instanceof JavaMailSender
    }
    
    protected String[] toDestinationAddresses(addresses) {
        if (overrideAddress) {
            addresses = addresses.collect { overrideAddress }
        }
    
        addresses.collect { it?.toString() } as String[]
    }
    
    protected getDescription(SimpleMailMessage message) {
        "[${message.subject}] from [${message.from}] to ${message.to}"
    }
    
    protected getDescription(Message message) {
        "[${message.subject}] from [${message.from}] to ${message.getRecipients(Message.RecipientType.TO)*.toString()}"
    }
    
    protected getDescription(MimeMailMessage message) {
        getDescription(message.mimeMessage)
    }
    
    MailMessage finishMessage() {
        def message = getMessage()

        if (htmlContent) {
            def helper = message.getMimeMessageHelper()
            if (textContent) {
                helper.setText(textContent, htmlContent)
            } else {
                helper.setText(htmlContent, false)
            }
        } else {
            if (!textContent) {
                throw new GrailsMailException("message has no content, use text(), html() or body() methods to set content")
            }
            
            message.text = textContent
        }
        
        inlines.each {
            doAdd(it.id, it.contentType, it.toAdd, false)
        }
        
        message.sentDate = new Date()
        
        if (mimeCapable) {
            message.mimeMessage.saveChanges()
        }
        
        message
    }
    
}
