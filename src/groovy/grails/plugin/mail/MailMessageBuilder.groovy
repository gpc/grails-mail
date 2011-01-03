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

import org.springframework.util.Assert

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
    private MimeMessageHelper helper
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
                helper = new MimeMessageHelper(mailSender.createMimeMessage(), multipart)
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
        Assert.notNull(hdrs, "headers cannot be null")
         
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
        Assert.notNull(args, "to cannot be null")
        
        getMessage().setTo(toDestinationAddresses(args))
    }
    
    void to(List args) {
        Assert.notNull(args, "to cannot be null")
        
        to(*args)
    }
    
    void bcc(Object[] args) {
        Assert.notNull(args, "bcc cannot be null")
        
        getMessage().setBcc(toDestinationAddresses(args))
    }
    
    void bcc(List args) {
        Assert.notNull(args, "bcc cannot be null")
        
        bcc(*args)
    }
        
    void cc(Object[] args) {
        Assert.notNull(args, "cc cannot be null")
        
        getMessage().setCc(toDestinationAddresses(args))
    }
    
    void cc(List args) {
        Assert.notNull(args, "cc cannot be null")
        
        cc(*args)
    }

    void replyTo(replyTo) {
        Assert.notNull(replyTo, "replyTo cannot be null")
        
        getMessage().replyTo = replyTo?.toString()
    }
    
    void from(from) {
        Assert.notNull(from, "from cannot be null")
        
        getMessage().from = from?.toString()
    }
    
    void title(title) {
        Assert.notNull(title, "title cannot be null")
        
        subject(title)
    }
    
    void subject(title) {
        Assert.notNull(title, "subject cannot be null")
        
        getMessage().subject = title?.toString()
    }
        
    void body(CharSequence body) {
        Assert.notNull(body, "body cannot be null")
        
        text(body)
    }
    
    void body(Map params) {
        Assert.notNull(params, "body cannot be null")
        
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
        Assert.notNull(params, "text cannot be null")
        
        text(doRender(params).out.toString())
    }
    
    void text(CharSequence text) {
        Assert.notNull(text, "text cannot be null")
        
        textContent = text.toString()
    }

    void html(Map params) {
        Assert.notNull(params, "html cannot be null")
        
        html(doRender(params).out.toString())
    }
    
    void html(CharSequence text) {
        Assert.notNull(text, "html cannot be null")
        
        if (mimeCapable) {
            htmlContent = text.toString()
        } else {
            throw new GrailsMailException("mail sender is not mime capable, try configuring a JavaMailSender")
        }
    }
    
    void locale(String localeStr) {
        Assert.notNull(localeStr, "locale cannot be null")
        
        locale(new Locale(*localeStr.split('_', 3)))
    }

    void locale(Locale locale) {
        Assert.notNull(locale, "locale cannot be null")
        
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

    void attach(File file) {
        attach(file.name, file)
    }
    
    void attach(String fileName, File file) {
        if (!mimeCapable) {
            throw new GrailsMailException("Message is not an instance of org.springframework.mail.javamail.MimeMessage, cannot attach bytes!")
        }
        
        attach(fileName, helper.fileTypeMap.getContentType(file), file)
    }
    
    void attach(String fileName, String contentType, File file) {
        if (!file.exists()) {
            throw new FileNotFoundException("cannot use $file as an attachment as it does not exist")
        }
        
        attach(fileName, contentType, new FileSystemResource(file))
    }
    
    void attach(String fileName, String contentType, InputStreamSource source) {
        doAdd(fileName, contentType, source, true)
    }
    
    void inline(String contentId, String contentType, byte[] bytes) {
        inline(contentId, contentType, new ByteArrayResource(bytes))
    }

    void inline(File file) {
        inline(file.name, file)
    }
    
    void inline(String fileName, File file) {
        if (!mimeCapable) {
            throw new GrailsMailException("Message is not an instance of org.springframework.mail.javamail.MimeMessage, cannot attach bytes!")
        }
        
        inline(fileName, helper.fileTypeMap.getContentType(file), file)
    }

    void inline(String contentId, String contentType, File file) {
        if (!file.exists()) {
            throw new FileNotFoundException("cannot use $file as an attachment as it does not exist")
        }
        
        inline(contentId, contentType, new FileSystemResource(file))
    }
    
    void inline(String contentId, String contentType, InputStreamSource source) {
        inlines << new Inline(id: contentId, contentType: contentType, toAdd: source)
    }
    
    protected doAdd(String id, String contentType, toAdd, boolean isAttachment) {
        if (!mimeCapable) {
            throw new GrailsMailException("Message is not an instance of org.springframework.mail.javamail.MimeMessage, cannot attach bytes!")
        }
        
        assert multipart, "message is not marked as 'multipart'; use 'multipart true' as the first line in your builder DSL"

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
/*            message.mimeMessage.saveChanges()*/
        }
        
        message
    }
    
}
