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
package grails.plugins.mail

import grails.plugins.Plugin
import org.springframework.jndi.JndiObjectFactoryBean
import org.springframework.mail.javamail.JavaMailSenderImpl
import groovy.util.logging.Commons

@Commons
class MailGrailsPlugin extends Plugin {
    def grailsVersion = "3.0 > *"

    def author = "Grails Plugin Collective"
    def authorEmail = "grails.plugin.collective@gmail.com"
    def title = "Provides Mail support to a running Grails application"
    def description = '''\
This plug-in provides a MailService class as well as configuring the necessary beans within
the Spring ApplicationContext.

It also adds a "sendMail" method to all controller classes. A typical example usage is:

sendMail {
    to "fred@g2one.com","ginger@g2one.com"
    from "john@g2one.com"
    cc "marge@g2one.com", "ed@g2one.com"
    bcc "joe@g2one.com"
    subject "Hello John"
    text "this is some text"
}

'''
    def documentation = "http://grails.org/plugins/mail"

    def license = "APACHE"
    def organization = [ name: "Grails Plugin Collective", url: "http://github.com/gpc" ]
    def developers = [
        [ name: "Craig Andrews", email: "candrews@integralblue.com" ],
        [ name: "Luke Daley", email: "ld@ldaley.com" ],
        [ name: "Peter Ledbrook", email: "p.ledbrook@cacoethes.co.uk" ],
        [ name: "Jeff Brown", email: "brownj@ociweb.com" ],
        [ name: "Graeme Rocher", email: "rocherg@ociweb.com" ],
        [ name: "Marc Palmer", email: "marc@grailsrocks.com" ],
        [ name: "Søren Berg Glasius", email: "glasiuss@ociweb.com" ],

    ]

    def issueManagement = [ system: "GitHub", url: "https://github.com/gpc/mail/issues" ]
    def scm = [ url: "http://github.com/gpc/grails-mail" ]

    def observe = ['controllers','services']

     def pluginExcludes = [
            "grails-app/views/_testemails/*.gsp"
    ]

    Integer mailConfigHash
    MailConfig mailConfig
    boolean createdSession = false


     Closure doWithSpring() { 
        {->
            mailConfig = new MailConfig(grailsApplication.config)
            mailConfigHash = mailConfig.hashCode()

            configureMailSender(delegate, mailConfig)

            mailMessageBuilderFactory(MailMessageBuilderFactory) {
                it.autowire = true
            }

            mailMessageContentRenderer(MailMessageContentRenderer) {
                it.autowire = true
            }
        } 
    }
    



    void onConfigChange(Map<String, Object> event) {
        MailConfig newMailConfig = new MailConfig(event.source)
        Integer newMailConfigHash = newMailConfig.hashCode()

        if (newMailConfigHash != mailConfigHash) {
            if (createdSession) {
                event.ctx.removeBeanDefinition("mailSession")
            }

            event.ctx.removeBeanDefinition("mailSender")

            mailConfig = newMailConfig
            mailConfigHash = newMailConfigHash

			event.ctx.getBean(MailService.class).setPoolSize(mailConfig.poolSize?:null)

            beans {
                configureMailSender(delegate, mailConfig)
            }
        }
    }

    def configureMailSender(builder, MailConfig config) {
        builder.with {
            if (config.jndiName && !springConfig.containsBean("mailSession")) {
                mailSession(JndiObjectFactoryBean) {
                    jndiName = config.jndiName
                }
                createdSession = true
            } else {
                createdSession = false
            }

            mailSender(JavaMailSenderImpl) {
                if (config.host) {
                    host = config.host
                } else if (!config.jndiName) {
                    def envHost = System.getenv()['SMTP_HOST']
                    if (envHost) {
                        host = envHost
                    } else {
                        host = "localhost"
                    }
                }

                if (config.encoding) {
                    defaultEncoding = config.encoding
                } else if (!config.jndiName) {
                    defaultEncoding = "utf-8"
                }

                if (config.jndiName)
                    session = ref('mailSession')
                if (config.port)
                    port = config.port
                if (config.username)
                    username = config.username
                if (config.password)
                    password = config.password
                if (config.protocol)
                    protocol = config.protocol
                if (config.props)
                    javaMailProperties = config.props
            }
        }
    }

}
