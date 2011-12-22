grails.project.work.dir = "target"

grails.project.dependency.resolution = {
    inherits("global")
    repositories {
        grailsHome()
        mavenRepo "http://download.java.net/maven/2/"
    }
    dependencies {
        compile "javax.mail:mail:1.4.3"
        
        runtime("org.springframework:spring-test:3.1.0.RELEASE") {
            transitive = false
        }
    }
    plugins {
        if (appName == "grails-mail") {
            test (":greenmail:1.3.0") {
                export = false
            }
            build(":release:1.0.0") {
                export = false
            }
            build(":tomcat:$grailsVersion") {
                export = false
            }
        }
    }
}

if (appName == "grails-mail") {
    // use for testing view resolution from plugins
    grails.plugin.location.'for-plugin-view-resolution' = 'plugins/for-plugin-view-resolution'
}

grails.release.scm.enabled = false