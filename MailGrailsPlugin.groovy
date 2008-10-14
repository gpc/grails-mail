import org.springframework.mail.javamail.JavaMailSenderImpl

class MailGrailsPlugin {

    def observe = ['controllers']
    def version = "0.5"
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
