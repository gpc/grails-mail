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

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap

import javax.servlet.ServletContext
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse



import grails.util.GrailsWebUtil
import groovy.text.Template
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.pages.GroovyPagesUriService
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.servlet.WrappedResponseHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.i18n.FixedLocaleResolver
import org.springframework.web.servlet.support.RequestContextUtils

/**
 * Renders a GSP into the content of a mail message.
 */
class MailMessageContentRenderer {

    static final String PATH_TO_MAILVIEWS = "/WEB-INF/grails-app/views"

    private static final Logger log = LoggerFactory.getLogger(MailMessageContentRenderer.class)

    GroovyPagesTemplateEngine groovyPagesTemplateEngine
    GroovyPagesUriService groovyPagesUriService
    GrailsApplication grailsApplication
    GrailsPluginManager pluginManager

    MailMessageContentRender render(Writer out, String templateName, model, locale, String pluginName = null) {
        RenderEnvironment.with(grailsApplication.mainContext, out, locale) { env ->
            Template template = createTemplate(templateName, env.controllerName, pluginName)
            if (model instanceof Map) {
                template.make(model).writeTo(out)
            } else {
                template.make().writeTo(out)
            }

            new MailMessageContentRender(out, template.metaInfo.contentType)
        }
    }

    protected Template createTemplate(String templateName, String controllerName, String pluginName) {
        if (templateName.startsWith("/")) {
            if (!controllerName) {
                controllerName = ""
            }
        } else {
            if (!controllerName) {
                throw new IllegalArgumentException("Mail views cannot be loaded from relative view paths where there is no current HTTP request")
            }
        }

        String contextPath = getContextPath(pluginName)

        String templateUri
        if (contextPath) {
            templateUri = contextPath + groovyPagesUriService.getViewURI(controllerName, templateName)
        } else {
            templateUri = groovyPagesUriService.getDeployedViewURI(controllerName, templateName)
        }

        Template template = groovyPagesTemplateEngine.createTemplateForUri(templateUri)

        if (!template) {
            if (pluginName) {
                throw new IllegalArgumentException("Could not locate email view ${templateName} in plugin [$pluginName]")
            } else {
                throw new IllegalArgumentException("Could not locate mail body ${templateName}. Is it in a plugin? If so you must pass the plugin name in the [plugin] variable")
            }
        }

        template
    }

    protected String getContextPath(String pluginName) {
        String contextPath = ""

        if (pluginName) {
            def plugin = pluginManager.getGrailsPlugin(pluginName)
            if (plugin && !plugin.isBasePlugin()) {
                contextPath = plugin.pluginPath + "/grails-app/views"
            }
        }

        contextPath
    }

    private static class RenderEnvironment {
        final Writer out
        final Locale locale
        final ApplicationContext applicationContext
		final LinkGenerator grailsLinkGenerator

        private originalRequestAttributes
        private renderRequestAttributes

        RenderEnvironment(ApplicationContext applicationContext, Writer out, Locale locale = null) {
            this.out = out
            this.locale = locale
            this.applicationContext = applicationContext
			this.grailsLinkGenerator = applicationContext.getBean('grailsLinkGenerator', LinkGenerator.class)
        }

        private void init() {
            originalRequestAttributes = RequestContextHolder.getRequestAttributes()

            def renderLocale
            if (locale) {
                renderLocale = locale
            } else if (originalRequestAttributes) {
                renderLocale = RequestContextUtils.getLocale(originalRequestAttributes.request)
            }

            renderRequestAttributes = new GrailsWebRequest(PageRenderRequestCreator.createInstance(grailsLinkGenerator.serverBaseURL, "/mail/render", renderLocale),
                PageRenderResponseCreator.createInstance(out instanceof PrintWriter ? out : new PrintWriter(out), renderLocale), null, applicationContext)

            if (originalRequestAttributes) {
                renderRequestAttributes.controllerName = originalRequestAttributes.controllerName
            }            

            RequestContextHolder.setRequestAttributes(renderRequestAttributes) 

            renderRequestAttributes.request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new FixedLocaleResolver(defaultLocale: renderLocale))
            renderRequestAttributes.setOut(out)
            WrappedResponseHolder.wrappedResponse = renderRequestAttributes.currentResponse
        }

        private void close() {
            RequestContextHolder.setRequestAttributes(originalRequestAttributes) // null ok
            WrappedResponseHolder.wrappedResponse = originalRequestAttributes?.currentResponse
        }

        /**
         * Establish an environment inheriting the locale of the current request if there is one
         */
        static with(ApplicationContext applicationContext, Writer out, Closure block) {
            with(applicationContext, out, null, block)
        }

        /**
         * Establish an environment with a specific locale
         */        
         static with(ApplicationContext applicationContext, Writer out, Locale locale, Closure block) {
            def env = new RenderEnvironment(applicationContext, out, locale)
            env.init()
            try {
                block(env)
            } finally {
                env.close()
            }
        }

        String getControllerName() {
            renderRequestAttributes.controllerName
        }
    }

   /*
     * Creates the request object used during the GSP rendering pipeline for render operations outside a web request.
     * Created dynamically to avoid issues with different servlet API spec versions.
     */
    static class PageRenderRequestCreator {

        static HttpServletRequest createInstance(final String serverBaseURL, final String requestURI, Locale localeToUse = Locale.getDefault()) {
            final URI serverBaseURI = new URI(serverBaseURL)

            def params = new ConcurrentHashMap()
            def attributes = new ConcurrentHashMap()

            String contentType = null
            String characterEncoding = "UTF-8"

            (HttpServletRequest)Proxy.newProxyInstance(HttpServletRequest.classLoader, [HttpServletRequest] as Class[], new InvocationHandler() {
                Object invoke(proxy, Method method, Object[] args) {

                    String methodName = method.name

                    if (methodName == 'getContentType') {
                        return contentType
                    }
                    if (methodName == 'setContentType') {
                        contentType = args[0]
                        return null
                    }
                    if (methodName == 'getCharacterEncoding') {
                        return characterEncoding
                    }
                    if (methodName == 'setCharacterEncoding') {
                        characterEncoding = args[0]
                    }

                    if (methodName == 'getRealPath') {
                        return requestURI
                    }
                    if (methodName == 'getLocalName') {
                        return "localhost"
                    }
                    if (methodName == 'getLocalAddr') {
                        return "127.0.0.1"
                    }
                    if (methodName == 'getLocalPort') {
                        return 80
                    }

                    if (methodName == 'getCookies') {
                        return ([] as Cookie[])
                    }
                    if (methodName == 'getDateHeader' || methodName == 'getIntHeader') {
                        return -1
                    }
                    if (methodName == 'getMethod') {
                        return 'GET'
                    }
                    if (methodName == 'getContextPath' || methodName == 'getServletPath') {
                        return '/'
                    }

                    if (methodName in ['getPathInfo', 'getPathTranslated', 'getQueryString']) {
                        return ''
                    }

                    if (methodName == 'getRequestURL') {
                        return new StringBuffer(requestURI)
                    }
                    if (methodName == 'getRequestURI') {
                        return requestURI
                    }

                    if (methodName == 'isRequestedSessionIdValid') {
                        return true
                    }
                    if (methodName in [
                        'isRequestedSessionIdFromCookie', 'isRequestedSessionIdFromURL', 'isRequestedSessionIdFromUrl',
                        'authenticate', 'isUserInRole', 'isSecure', 'isAsyncStarted', 'isAsyncSupported']) {
                        return false
                    }

                    if (methodName == 'getSession') {
                        throw new UnsupportedOperationException("You cannot use the session in non-request rendering operations")
                    }
                    if (methodName == 'getInputStream') {
                        throw new UnsupportedOperationException("You cannot read the input stream in non-request rendering operations")
                    }
                    if (methodName == 'getProtocol') {
                        throw new UnsupportedOperationException("You cannot read the protocol in non-request rendering operations")
                    }
                    if (methodName == 'getScheme') {
                        return serverBaseURI.scheme
                    }
                    if (methodName == 'getServerName') {
                        return serverBaseURI.host
                    }
                    if (methodName == 'getServerPort') {
                        int port = serverBaseURI.port
                        if(port == -1){
                        switch(serverBaseURI.scheme?.toLowerCase()){
								case 'http':
									port = 80
									break
								case 'https':
									port = 443
									break
							}
						}
						return port
                    }
                    if (methodName == 'getReader') {
                        throw new UnsupportedOperationException("You cannot read input in non-request rendering operations")
                    }
                    if (methodName == 'getRemoteAddr') {
                        throw new UnsupportedOperationException("You cannot read the remote address in non-request rendering operations")
                    }
                    if (methodName == 'getRemoteHost') {
                        throw new UnsupportedOperationException("You cannot read the remote host in non-request rendering operations")
                    }
                    if (methodName == 'getRequestDispatcher') {
                        throw new UnsupportedOperationException("You cannot use the request dispatcher in non-request rendering operations")
                    }
                    if (methodName == 'getRemotePort') {
                        throw new UnsupportedOperationException("You cannot read the remote port in non-request rendering operations")
                    }

                    if (methodName == 'getParts') {
                        return []
                    }

                    if (methodName == 'getAttribute') {
                        return attributes[args[0]]
                    }
                    if (methodName == 'getAttributeNames') {
                        return attributes.keys()
                    }
                    if (methodName == 'setAttribute') {
                        String name = args[0]
                        Object o = args[1]
                        if (o == null) {
                            attributes.remove name
                        } else {
                            attributes[name] = o
                        }
                        return null
                    }
                    if (methodName == 'removeAttribute') {
                        attributes.remove args[0]
                        return null
                    }

                    if (methodName == 'getLocale') {
                        return localeToUse
                    }
                    if (methodName == 'getLocales') {
                        def iterator = [localeToUse].iterator()
                        PageRenderRequestCreator.iteratorAsEnumeration(iterator)
                    }

                    if (methodName == 'getParameter') {
                        return params[args[0]]
                    }
                    if (methodName == 'getParameterNames') {
                        return params.keys()
                    }
                    if (methodName == 'getParameterValues') {
                        return [] as String[]
                    }
                    if (methodName == 'getParameterMap') {
                        return params
                    }

                    if (methodName == 'getContentLength') {
                        return 0
                    }

                    if ('getHeaderNames'.equals(methodName) || 'getHeaders'.equals(methodName)) {
                        return Collections.enumeration(Collections.emptySet())
                    }

                    return null
                }
            })
        }
        
        private static Enumeration iteratorAsEnumeration(Iterator iterator) {
            new Enumeration() {
                @Override
                boolean hasMoreElements() {
                    iterator.hasNext()
                }

                @Override
                Object nextElement() {
                    iterator.next()
                }
            }
        }
    }

    static class PageRenderResponseCreator {

        static HttpServletResponse createInstance(final PrintWriter writer, Locale localeToUse = Locale.getDefault()) {

            String characterEncoding = "UTF-8"
            String contentType = null
            int bufferSize = 0

            (HttpServletResponse)Proxy.newProxyInstance(HttpServletResponse.classLoader, [HttpServletResponse] as Class[], new InvocationHandler() {
                Object invoke(proxy, Method method, Object[] args) {

                    String methodName = method.name

                    if (methodName == 'getContentType') {
                        return contentType
                    }
                    if (methodName == 'setContentType') {
                        contentType = args[0]
                        return null
                    }
                    if (methodName == 'getCharacterEncoding') {
                        return characterEncoding
                    }
                    if (methodName == 'setCharacterEncoding') {
                        characterEncoding = args[0]
                        return null
                    }
                    if (methodName == 'getBufferSize') {
                        return bufferSize
                    }
                    if (methodName == 'setBufferSize') {
                        bufferSize = args[0]
                        return null
                    }

                    if (methodName == 'containsHeader' || methodName == 'isCommitted') {
                        return false
                    }

                    if (methodName in ['encodeURL', 'encodeRedirectURL', 'encodeUrl', 'encodeRedirectUrl']) {
                        return args[0]
                    }

                    if (methodName == 'getWriter') {
                        writer
                    }

                    if (methodName == 'getOutputStream') {
                        throw new UnsupportedOperationException("You cannot use the OutputStream in non-request rendering operations. Use getWriter() instead")
                    }

                    if (methodName == 'getHeaderNames') {
                        return []
                    }

                    if (methodName == 'getLocale') {
                        return localeToUse
                    }

                    if (methodName == 'getStatus') {
                        return 0
                    }

                    return null
                }
            })
        }
    }    
}
