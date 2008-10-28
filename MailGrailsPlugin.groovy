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
import org.springframework.mail.javamail.JavaMailSenderImpl

class MailGrailsPlugin {

    def observe = ['controllers']
    def version = "0.6-SNAPSHOT"
    def author = "Graeme Rocher"
    def authorEmail = "graeme@g2one.com"
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

    // URL to the plugin's documentation
    def documentation = "http://grails.org/Mail+Plugin"

    def doWithSpring = {
        def config = application.config.grails.mail
        mailSender(JavaMailSenderImpl) {
            host = config.host ?: "localhost"
            defaultEncoding = config.encoding ?: "utf-8"
            if(config.port)
                port = config.port
            if(config.username)
                username = config.username
            if(config.password)
                password = config.password
            if(config.protocol)
                protocol = config.protocol
            if(config.props instanceof Map && config.props)
                javaMailProperties = config.props
        }
    }
   
    def doWithApplicationContext = { applicationContext ->
        configureSendMail(application, applicationContext)
    }

    def onChange = {event ->
        configureSendMail(event.application, event.ctx)
    }

    def configureSendMail(application, applicationContext) {
        application.controllerClasses*.metaClass*.sendMail = {Closure callable ->
            applicationContext.mailService?.sendMail(callable)
        }
    }
}
