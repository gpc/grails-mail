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
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMailMessage
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.support.WebApplicationContextUtils
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU


/**
 * The builder that implements the mail DSL.
 */
class MailMessageBuilder {
    private MailMessage message

    static PATH_TO_MAILVIEWS = "/WEB-INF/grails-app/views"
    static HTML_CONTENTTYPES = ['text/html', 'text/xhtml']

    MailSender mailSender
    MailService mailService

    MailMessageBuilder(MailService svc, MailSender mailSender) {
        this.mailSender = mailSender
        this.mailService = svc
    }

    private MailMessage getMessage() {
        if(!message) {
            if(mailSender instanceof JavaMailSender) {
                message = new MimeMailMessage(mailSender.createMimeMessage() )
            }
            else {
                message = new SimpleMailMessage()
            }
            message.from = ConfigurationHolder.config.grails.mail.default.from
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
    void to(List args) {
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
    void headers(Map hdrs) {
        def msg = getMessage()

        // The message must be of type MimeMailMessage to add headers.
        if (!(msg instanceof MimeMailMessage)) {
            throw new GrailsMailException("You must use a JavaMailSender to customise the headers.")
        }

        msg = msg.mimeMessageHelper.mimeMessage
        hdrs.each { String name, String value ->
            msg.setHeader(name, value)
        }
    }
    void body(body) {
        text(body)
    }
    void body(Map params) {
        if (params.view) {
            // Here need to render it first, establish content type of virtual response / contentType model param
            renderMailView(params.view, params.model, params.plugin)
        } else {
            text(body)
        }
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
    void bcc(List args) {
        getMessage().bcc = args as String[]
    }
    void cc(String cc) {
        getMessage().cc = [cc] as String[]
    }
    void cc(Object[] args) {
        getMessage().cc = args as String[]
    }
    void cc(List args) {
        getMessage().cc = args as String[]
    }

    void replyTo(String replyTo) {
        getMessage().replyTo = replyTo
    }
    void from(String from) {
        getMessage().from = from
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
            // Caution, this uses views/ always, whereas our app view resolution uses the PATH_TO_MAILVIEWS which may in future be orthogonal!
            def plugin = PluginManagerHolder.pluginManager.getGrailsPlugin(pluginName)
            String pathToView
            if (plugin) {
                pathToView = '/plugins/'+GCU.getScriptName(plugin.name)+'-'+plugin.version+'/'+GrailsResourceUtils.GRAILS_APP_DIR+'/views'+templateName
            }

            if (pathToView != null) {
                uri = GrailsResourceUtils.WEB_INF +pathToView +templateName+".gsp";
                r = engine.getResourceForUri(uri)
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
