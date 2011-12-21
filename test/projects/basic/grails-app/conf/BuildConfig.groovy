grails.plugin.location.mail = "../../.."

grails.project.work.dir = "target"

grails.project.dependency.resolution = {
    inherits("global")
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'

    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenCentral()
    }
    plugins {
        test ":geb:0.6.1"
    }
    dependencies {
        test "org.seleniumhq.selenium:selenium-htmlunit-driver:2.15.0", {
            excludes "xml-apis"
        }
        test "org.codehaus.geb:geb-spock:0.6.1"
    }
}
