/*
 * Copyright 2022-2024 the original author or authors.
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

import grails.core.GrailsApplication
import grails.plugins.GrailsPluginManager
import grails.web.pages.GroovyPagesUriService
import groovy.transform.CompileStatic
import org.grails.gsp.GroovyPagesTemplateEngine
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jndi.JndiObjectFactoryBean
import org.springframework.mail.MailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

import javax.mail.Session

@Configuration
@CompileStatic
class MailConfiguration {

    @Bean
    MailConfigurationProperties mailConfigurationProperties() {
        return new MailConfigurationProperties()
    }

    @Bean
    @ConditionalOnMissingBean(name = 'mailSession')
    @ConditionalOnProperty(prefix = 'grails.mail', name = 'jndiName')
    JndiObjectFactoryBean mailSession(MailConfigurationProperties mailProperties) {
        def factory = new JndiObjectFactoryBean()
        factory.jndiName = mailProperties.jndiName
        return factory
    }

    @Bean
    JavaMailSenderImpl mailSender(
            @Autowired(required = false) @Qualifier('mailSession') Session mailSession,
            MailConfigurationProperties mailProperties) {

        def mailSender = new JavaMailSenderImpl()
        if (mailProperties.host) {
            mailSender.host = mailProperties.host
        } else if (!mailProperties.jndiName) {
            def envHost = System.getenv()['SMTP_HOST']
            if (envHost) {
                mailSender.host = envHost
            } else {
                mailSender.host = 'localhost'
            }
        }
        if (mailProperties.encoding) {
            mailSender.defaultEncoding = mailProperties.encoding
        } else if (!mailProperties.jndiName) {
            mailSender.defaultEncoding = 'utf-8'
        }
        if (mailSession != null) {
            mailSender.session = mailSession
        }
        if (mailProperties.port) {
            mailSender.port = mailProperties.port
        }
        if (mailProperties.username) {
            mailSender.username = mailProperties.username
        }
        if (mailProperties.password) {
            mailSender.password = mailProperties.password
        }
        if (mailProperties.protocol) {
            mailSender.protocol = mailProperties.protocol
        }
        if (mailProperties.props) {
            mailSender.javaMailProperties = mailProperties.props
        }
        return mailSender
    }

    @Bean
    MailMessageBuilderFactory mailMessageBuilderFactory(
            MailSender mailSender,
            MailMessageContentRenderer mailMessageContentRenderer) {
        return new MailMessageBuilderFactory(mailSender, mailMessageContentRenderer)
    }

    @Bean
    MailMessageContentRenderer mailMessageContentRenderer(
            GroovyPagesTemplateEngine groovyPagesTemplateEngine,
            GroovyPagesUriService groovyPagesUriService,
            GrailsApplication grailsApplication,
            GrailsPluginManager pluginManager) {
        return new MailMessageContentRenderer(groovyPagesTemplateEngine, groovyPagesUriService, grailsApplication, pluginManager)
    }
}
