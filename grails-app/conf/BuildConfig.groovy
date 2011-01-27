grails.project.work.dir = "target"

grails.project.dependency.resolution = {
    inherits("global")
    repositories {
        grailsHome()
        mavenRepo "http://download.java.net/maven/2/"
    }
    dependencies {
        compile "javax.mail:mail:1.4.3"
        
        // Potential problem here in that we may end up with a different version
        // during war deployment to product, but there is no solution right now.
        // 
        // See: http://jira.codehaus.org/browse/GRAILS-7192
        // See: http://grails.1312388.n4.nabble.com/handling-Spring-dependencies-td3226874.html
        runtime "org.springframework:spring-test:latest.release"
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
