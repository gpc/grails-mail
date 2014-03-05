grails.project.repos.grailsCentral.username = System.getenv("GRAILS_CENTRAL_USERNAME")
grails.project.repos.grailsCentral.password = System.getenv("GRAILS_CENTRAL_PASSWORD")

grails.project.work.dir = "target"

grails.project.dependency.resolution = {

    inherits("global")

    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        compile "javax.mail:mail:1.4.5"

        runtime("org.springframework:spring-test:3.1.0.RELEASE") {
            transitive = false
        }
    }

    plugins {
        test (":greenmail:1.3.0") {
            export = false
        }
        build(":release:2.0.4", ":rest-client-builder:1.0.2") {
            export = false
        }
    }
}

if (appName == "grails-mail") {
    // use for testing view resolution from plugins
    grails.plugin.location.'for-plugin-view-resolution' = 'plugins/for-plugin-view-resolution'
}

grails.release.scm.enabled = false
