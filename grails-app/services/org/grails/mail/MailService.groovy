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

import javax.servlet.http.HttpServletRequest
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import javax.mail.internet.MimeMessage
import org.springframework.mail.MailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMailMessage
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes
import org.codehaus.groovy.grails.commons.GrailsResourceUtils
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.springframework.mock.web.*
import org.springframework.web.context.support.*
import org.codehaus.groovy.grails.web.context.*

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
			requestAttributes = grails.util.GrailsWebUtil.bindMockWebRequest(applicationContext)
			unbindRequest = true
		}
		def servletContext = requestAttributes.request.servletContext 
		def request = requestAttributes.request 

        def grailsAttributes = new DefaultGrailsApplicationAttributes(servletContext);
        // See if the application has the view for it
        def uri = getMailViewUri(templateName, request)

        def r = engine.getResourceForUri(uri)
        // Try plugin view if not found in application
        if ((!r || !r.exists()) && controllerName) {
            // Caution, this uses views/ always, whereas our app view resolution uses the PATH_TO_MAILVIEWS which may in future be orthogonal!
            def plugin = PluginManagerHolder.pluginManager.getGrailsPlugin(pluginName)
            String pathToView
            if (plugin) {
                pathToView = '/plugins/'+plugin.name+'-'+plugin.version+'/'+GrailsResourceUtils.GRAILS_APP_DIR+'/views'+templateName
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

        StringBuffer buf = new StringBuffer(PATH_TO_MAILVIEWS);
	       
        if(viewName.startsWith("/")) {
           String tmp = viewName.substring(1,viewName.length());
           if(tmp.indexOf('/') > -1) {
        	   buf.append('/');
        	   buf.append(tmp.substring(0,tmp.lastIndexOf('/')));
        	   buf.append("/");
        	   buf.append(tmp.substring(tmp.lastIndexOf('/') + 1,tmp.length()));
           }
           else {
        	   buf.append("/");
        	   buf.append(viewName.substring(1,viewName.length()));
           }
        }
        else {
           if (!request) throw new IllegalArgumentException(
               "Mail views cannot be loaded from relative view paths where there is no current HTTP request")
           def grailsAttributes = new DefaultGrailsApplicationAttributes(request.servletContext);
           buf.append(grailsAttributes.getControllerUri(request))
                .append("/")
                .append(viewName);
   
        }
        return buf.append(".gsp").toString();
	}
}