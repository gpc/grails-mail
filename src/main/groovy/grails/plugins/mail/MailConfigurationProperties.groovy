package grails.plugins.mail

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(MailConfigurationProperties.PREFIX)
class MailConfigurationProperties {

    public static final String PREFIX = 'grails.mail'

    boolean disabled
    String overrideAddress
    Defaults defaults = new Defaults()

    Integer poolSize
    String encoding
    String jndiName
    String protocol
    String host
    Integer port
    String username
    String password
    Properties props

    /**
     * We can't use 'default' as a groovy property name as it is a reserved keyword.
     * But it is possible to define a getter for it to make it compatible.
     */
    Defaults getDefault() {
        return defaults
    }

    static class Defaults {
        String from
        String to
    }
}
