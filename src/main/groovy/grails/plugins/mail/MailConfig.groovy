package grails.plugins.mail

import grails.config.Config
import groovy.transform.EqualsAndHashCode
import org.grails.config.NavigableMap

@EqualsAndHashCode
class MailConfig {

    final boolean disabled
    final String overrideAddress
    final String from
    final String to

    final Integer poolSize
    final String encoding
    final String jndiName
    final String protocol
    final String host
    final Integer port
    final String username
    final String password
    final Properties props

    MailConfig(Config config) {
        disabled = config.getProperty('grails.mail.disabled', Boolean, false)
        overrideAddress = config.getProperty('grails.mail.overrideAddress')
        from = config.getProperty('grails.mail.default.from')
        to = config.getProperty('grails.mail.default.to')
        poolSize = config.getProperty('grails.mail.poolSize', Integer)
        encoding = config.getProperty('grails.mail.encoding')
        jndiName = config.getProperty('grails.mail.jndiName')
        protocol = config.getProperty('grails.mail.protocol')
        host = config.getProperty('grails.mail.host')
        port = config.getProperty('grails.mail.port', Integer)
        username = config.getProperty('grails.mail.username')
        password = config.getProperty('grails.mail.password')

        Map props = config.getProperty('grails.mail.props', Map)
        if (props != null) {
            if (props instanceof NavigableMap) {
                this.props = props.toProperties()
            } else {
                this.props = new Properties()
                this.props.putAll(props)
            }
        } else {
            this.props = null
        }
    }
}
