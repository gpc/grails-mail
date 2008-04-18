import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSenderImpl

class MailGrailsPlugin {
    def version = 0.1
    def dependsOn = [:]
    def observe = ["controllers"]
    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithApplicationContext = {applicationContext ->
        // TODO Implement post initialization spring config (optional)		
    }

    def doWithWebDescriptor = {xml ->
        // TODO Implement additions to web.xml (optional)
    }

    def doWithDynamicMethods = {ctx ->
        setupSendMethod(application)
    }

    def onChange = {event ->
        setupSendMethod(application)
    }

    def onApplicationChange = {event ->
        // TODO Implement code that is executed when any class in a GrailsApplication changes
        // the event contain: event.source, event.application and event.applicationContext objects
    }

    private def setupSendMethod(application) {
        // TODO obviously just a first step...
        application.controllerClasses.each {controllerClass ->
            controllerClass.metaClass.send = {cfg, closure ->
                def mailMessage = new SimpleMailMessage()
                closure.delegate = mailMessage
                closure()

                def mailSender = new JavaMailSenderImpl();

                def senderCfg = cfg.sender
                senderCfg.each {key, val ->
                    mailSender."${key}" = val
                }
                mailSender.send(mailMessage);
            }
        }
    }

}
