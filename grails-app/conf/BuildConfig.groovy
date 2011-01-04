grails.project.work.dir = "target"

grails.project.dependency.resolution = {
    inherits("global")
    repositories {
        grailsHome()
        mavenRepo "http://download.java.net/maven/2/"
    }
    dependencies {
        compile "javax.mail:mail:1.4.1"
        runtime "org.springframework:org.springframework.test:3.0.3.RELEASE"
    }
    plugins {
        test (":greenmail:1.2.2") {
            export = false
        }
    }
}

if (appName == "grails-mail") {
    // use for testing view resolution from plugins
    grails.plugin.location.'for-plugin-view-resolution' = 'plugins/for-plugin-view-resolution'
}
