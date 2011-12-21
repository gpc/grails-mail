/*grails.plugin.location.mail = "../../.."*/

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
        compile ":greenmail:1.3.1-SNAPSHOT"
        test ":spock:0.5-groovy-1.7"
        compile ":hibernate:$grailsVersion"
        compile ":tomcat:$grailsVersion"
        compile ":mail:1.0-SNAPSHOT"
    }
    dependencies {
        test "org.seleniumhq.selenium:selenium-firefox-driver:2.15.0", {
            excludes "xml-apis"
        }
        test "org.codehaus.geb:geb-spock:0.6.1"
    }
}
