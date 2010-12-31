/*
 * Copyright 2010 the original author or authors.
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

import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.commons.GrailsResourceUtils
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.support.WebApplicationContextUtils
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.i18n.FixedLocaleResolver

import org.apache.commons.logging.LogFactory

import javax.servlet.http.HttpServletRequest

/**
 * Responsible for rendering a GSP into the content of a mail message.
 */
class MailMessageContentRenderer {

    static PATH_TO_MAILVIEWS = "/WEB-INF/grails-app/views"
    
    private log = LogFactory.getLog(MailMessageContentRenderer)
    
    def groovyPagesTemplateEngine
        
    MailMessageContentRender render(Writer out, templateName, model, locale, pluginName = null) {
        assert templateName

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
        if (locale) {
            request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,new FixedLocaleResolver(defaultLocale:locale))
        }

        def grailsAttributes = new DefaultGrailsApplicationAttributes(servletContext);
        // See if the application has the view for it
        def uri = getMailViewUri(templateName, request)

        def r = groovyPagesTemplateEngine.getResourceForUri(uri)
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
                    r = groovyPagesTemplateEngine.getResourceForUri(uri)
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
        def t = groovyPagesTemplateEngine.createTemplate( r )

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

        new MailMessageContentRender(out, t.metaInfo.contentType)
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