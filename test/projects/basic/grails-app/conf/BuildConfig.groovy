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
        test ":geb:0.4", {
            excludes "geb-grails"
        }
    }
    dependencies {
        test "org.codehaus.geb:geb-grails:0.5-SNAPSHOT"

        test "org.seleniumhq.selenium:selenium-htmlunit-driver:latest.integration", {
            excludes "xml-apis"
        }
        test "net.sourceforge.htmlunit:htmlunit:2.8", {
            excludes "xml-apis", "commons-logging"
        }
    }
}
