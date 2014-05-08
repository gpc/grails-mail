grails.project.repos.grailsCentral.username = System.getenv("GRAILS_CENTRAL_USERNAME")
grails.project.repos.grailsCentral.password = System.getenv("GRAILS_CENTRAL_PASSWORD")

grails.project.work.dir = "target"

grails.project.dependency.resolver = "maven"
grails.project.dependency.resolution = {

    inherits("global")

    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
    }

    dependencies {
		compile "javax.mail:javax.mail-api:1.5.1"
        runtime "com.sun.mail:javax.mail:1.5.1"
    }

    plugins {
        test (":greenmail:1.3.4") {
            export = false
        }
        build ":tomcat:7.0.52.1", ':release:3.0.1', ':rest-client-builder:2.0.1', {
            export = false
        }
    }
}

if (appName == "grails-mail") {
    // use for testing view resolution from plugins
    grails.plugin.location.'for-plugin-view-resolution' = 'plugins/for-plugin-view-resolution'
}

grails.release.scm.enabled = false
