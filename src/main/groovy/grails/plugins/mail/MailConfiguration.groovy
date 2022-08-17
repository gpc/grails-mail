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
        JndiObjectFactoryBean factory = new JndiObjectFactoryBean()
        factory.jndiName = mailProperties.jndiName
        return factory
    }

    @Bean
    JavaMailSenderImpl mailSender(
            @Autowired(required = false) @Qualifier('mailSession') Session mailSession,
            MailConfigurationProperties mailProperties) {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl()
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
        MailMessageBuilderFactory factory = new MailMessageBuilderFactory()
        factory.mailSender = mailSender
        factory.mailMessageContentRenderer = mailMessageContentRenderer
        return factory
    }

    @Bean
    MailMessageContentRenderer mailMessageContentRenderer(
            GroovyPagesTemplateEngine groovyPagesTemplateEngine,
            GroovyPagesUriService groovyPagesUriService,
            GrailsApplication grailsApplication,
            GrailsPluginManager pluginManager) {
        MailMessageContentRenderer renderer = new MailMessageContentRenderer()
        renderer.groovyPagesTemplateEngine = groovyPagesTemplateEngine
        renderer.groovyPagesUriService = groovyPagesUriService
        renderer.grailsApplication = grailsApplication
        renderer.pluginManager = pluginManager
        return renderer
    }
}
