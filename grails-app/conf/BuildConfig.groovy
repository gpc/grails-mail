grails.project.work.dir = "target"

// Get the correct spring version
def springTestDepsByGrailsVersion = [
    '1.3.0':':org.springframework.test:3.0.0.RELEASE',
    '1.3.1':':org.springframework.test:3.0.2.RELEASE',
    '1.3.2':':org.springframework.test:3.0.3.RELEASE',
    '1.3.3':':org.springframework.test:3.0.3.RELEASE',
    '1.3.4':':org.springframework.test:3.0.3.RELEASE',
    '1.3.5':':org.springframework.test:3.0.3.RELEASE',
    '1.3.6':':org.springframework.test:3.0.5.RELEASE',
    '1.3.7':':org.springframework.test:3.0.5.RELEASE',
    '2.0':'org.springframework:spring-test:3.1.0.RELEASE'
]
// For Grails version newer than 2.0 we can find this out, so fall back to that
def springTestDep = springTestDepsByGrailsVersion[grailsVersion] ?: "org.springframework:spring-test:$grailsCoreDependencies.springVersion"

grails.project.dependency.resolution = {
    inherits("global")
    repositories {
        grailsHome()
        mavenRepo "http://download.java.net/maven/2/"
    }
    dependencies {
        compile "javax.mail:mail:1.4.3"
        
        runtime springTestDep
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
