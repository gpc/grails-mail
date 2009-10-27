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

import grails.util.GrailsWebUtil
import javax.servlet.http.HttpServletRequest
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.GrailsResourceUtils
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes
import org.springframework.mail.MailMessage
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.*
import org.springframework.core.io.*
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.support.WebApplicationContextUtils
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.apache.commons.logging.LogFactory

/**
 * The builder that implements the mail DSL.
 */
class MailMessageBuilder {
    private MailMessage message

    static PATH_TO_MAILVIEWS = "/WEB-INF/grails-app/views"
    static HTML_CONTENTTYPES = ['text/html', 'text/xhtml']

    private log = LogFactory.getLog(MailMessageBuilder.class);
    
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
            message.from = ConfigurationHolder.config.grails.mail.default.from
            if (ConfigurationHolder.config.grails.mail.overrideAddress)
                message.from = ConfigurationHolder.config.grails.mail.overrideAddress
        }
        return message
    }

    MailMessage createMessage() { getMessage() }


    void multipart(boolean multipart) {
        this.multipart = multipart
    }

    void to(recip) {
        if(recip) {
            if (ConfigurationHolder.config.grails.mail.overrideAddress)
                recip = ConfigurationHolder.config.grails.mail.overrideAddress
            getMessage().setTo([recip.toString()] as String[])
        }
    }

	void attachBytes(String fileName, String contentType, byte[] bytes) {
		def msg = getMessage()
		if(msg instanceof MimeMailMessage) {
            assert multipart, "message is not marked as 'multipart'; use 'multipart true' as the first line in your builder DSL"
            msg.mimeMessageHelper.addAttachment(fileName, new ByteArrayResource(bytes), contentType)
		}
		else {
			throw new IllegalStateException("Message is not an instance of org.springframework.mail.javamail.MimeMessage, cannot attach bytes!")
		}
	}

    void to(Object[] args) {
        if(args) {
			if (ConfigurationHolder.config.grails.mail.overrideAddress)
			   args = args.collect { ConfigurationHolder.config.grails.mail.overrideAddress }.toArray()

            getMessage().setTo((args.collect { it?.toString() }) as String[])
        }
    }
    void to(List args) {
        if(args) {
			if (ConfigurationHolder.config.grails.mail.overrideAddress)
			   args = args.collect { ConfigurationHolder.config.grails.mail.overrideAddress }	
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
            renderMailView(params.view, params.model, params.plugin)
        } 
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
	    if (ConfigurationHolder.config.grails.mail.overrideAddress)
            bcc = ConfigurationHolder.config.grails.mail.overrideAddress
    
        getMessage().setBcc([bcc?.toString()] as String[])
    }
    void bcc(Object[] args) {
		if (ConfigurationHolder.config.grails.mail.overrideAddress)
		   args = args.collect { ConfigurationHolder.config.grails.mail.overrideAddress }.toArray()
	
        getMessage().setBcc((args.collect { it?.toString() }) as String[])
    }
    void bcc(List args) {
		if (ConfigurationHolder.config.grails.mail.overrideAddress)
		   args = args.collect { ConfigurationHolder.config.grails.mail.overrideAddress }
	
        getMessage().setBcc((args.collect { it?.toString() }) as String[])
    }
    void cc(cc) {
	    if (ConfigurationHolder.config.grails.mail.overrideAddress)
            cc = ConfigurationHolder.config.grails.mail.overrideAddress
	
        getMessage().setCc([cc?.toString()] as String[])
    }
    void cc(Object[] args) {
		if (ConfigurationHolder.config.grails.mail.overrideAddress)
		   args = args.collect { ConfigurationHolder.config.grails.mail.overrideAddress }.toArray()
	
        getMessage().setCc((args.collect { it?.toString() }) as String[])
    }
    void cc(List args) {
		if (ConfigurationHolder.config.grails.mail.overrideAddress)
		   args = args.collect { ConfigurationHolder.config.grails.mail.overrideAddress }
	
        getMessage().setCc((args.collect { it?.toString() }) as String[])
    }

    void replyTo(replyTo) {
        getMessage().replyTo = replyTo?.toString()
    }
    void from(from) {
        getMessage().from = from?.toString()
    }

	protected renderMailView(templateName, model, pluginName = null) {
        if(!mailService.groovyPagesTemplateEngine) throw new IllegalStateException("Property [groovyPagesTemplateEngine] must be set!")
        assert templateName

        def engine = mailService.groovyPagesTemplateEngine
        def requestAttributes = RequestContextHolder.getRequestAttributes()
		boolean unbindRequest = false

		// outside of an executing request, establish a mock version
		if(!requestAttributes) {
			def servletContext  = ServletContextHolder.getServletContext()
			def applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext)
			requestAttributes = GrailsWebUtil.bindMockWebRequest(applicationContext)
			unbindRequest = true
		}
		def servletContext = requestAttributes.request.servletContext
		def request = requestAttributes.request

        def grailsAttributes = new DefaultGrailsApplicationAttributes(servletContext);
        // See if the application has the view for it
        def uri = getMailViewUri(templateName, request)

        def r = engine.getResourceForUri(uri)
        // Try plugin view if not found in application
        if (!r || !r.exists()) {
            if (log.debugEnabled) {
                log.debug "Could not locate email view ${templateName} at ${uri}, trying plugin"
            }
            if (pluginName) {
                // Caution, this uses views/ always, whereas our app view resolution uses the PATH_TO_MAILVIEWS which may in future be orthogonal!
                def plugin = PluginManagerHolder.pluginManager.getGrailsPlugin(pluginName)
                String pathToView = null
                if (plugin) {
                    pathToView = '/plugins/'+GCU.getScriptName(plugin.name)+'-'+plugin.version+'/'+GrailsResourceUtils.GRAILS_APP_DIR+'/views'
                }

                if (pathToView != null) {
                    uri = GrailsResourceUtils.WEB_INF +pathToView +templateName+".gsp";
                    r = engine.getResourceForUri(uri)
                } else {
                    if (log.errorEnabled) {
                        log.error "Could not locate email view ${templateName} in plugin [$pluginName]"
                    }
                    throw new IllegalArgumentException("Could not locate email view ${templateName} in plugin [$pluginName]")
                }
            } else {
                if (log.errorEnabled) {
                    log.error "Could not locate email view ${templateName} at ${uri}, no pluginName specified so couldn't look there"
                }
                throw new IllegalArgumentException("Could not locate mail body ${templateName}. Is it in a plugin? If so you must pass the plugin name in the [plugin] variable")
            }
        }
        def t = engine.createTemplate( r )

        def out = new StringWriter();
        def originalOut = requestAttributes.getOut()
        requestAttributes.setOut(out)
        try {
            if(model instanceof Map) {
                t.make( model ).writeTo(out)
            }
    		else {
    			t.make().writeTo(out)
    		}
	    }
	    finally {
	        requestAttributes.setOut(originalOut)
			if(unbindRequest) {
				RequestContextHolder.setRequestAttributes(null)
			}
	    }

	    if (HTML_CONTENTTYPES.contains(t.metaInfo.contentType)) {
	        html(out.toString()) // @todo Spring mail helper will not set correct mime type if we give it XHTML
        } else {
            text(out)
        }
    }

	protected String getMailViewUri(String viewName, HttpServletRequest request) {

        def buf = new StringBuilder(PATH_TO_MAILVIEWS)
		
        if(viewName.startsWith("/")) {
           def tmp = viewName[1..-1]
           if(tmp.indexOf('/') > -1) {
			   def i = tmp.lastIndexOf('/')
        	   buf << "/${tmp[0..i]}/${tmp[(i+1)..-1]}"
           }
           else {
        	   buf << "/${viewName[1..-1]}"
           }
        }
        else {
           if (!request) throw new IllegalArgumentException(
               "Mail views cannot be loaded from relative view paths where there is no current HTTP request")
           def grailsAttributes = new DefaultGrailsApplicationAttributes(request.servletContext)
           buf << "${grailsAttributes.getControllerUri(request)}/${viewName}"

        }
        return buf.append(".gsp").toString()
	}
}
