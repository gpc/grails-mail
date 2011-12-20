grails.project.work.dir = "target"

def springTestDepsByGrailsVersion = [
    '1.3.7':':org.springframework.test:3.0.5.RELEASE',
    '2.0':'org.springframework:spring-test:3.1.0.RELEASE'
]
def springTestDep = springTestDepsByGrailsVersion[grailsVersion]

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
