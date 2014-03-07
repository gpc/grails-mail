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
		compile "javax.mail:javax.mail-api:1.5.1"
        runtime "com.sun.mail:javax.mail:1.5.1"

        compile("org.springframework:spring-test:3.1.4.RELEASE") {
            // Grails 2.3 and higher can use Aether/Maven instead of Ivy
            // which does not support:
            // transitive = false
            // so explicitly list the exclusions
            excludes 'com.jayway.jsonpath:json-path',
                'hsqldb:hsqldb',
                'javax.activation:activation',
                'javax.el:el-api',
                'javax.inject:javax.inject',
                'javax.persistence:persistence-api',
                'javax.portlet:portlet-api',
                'javax.servlet.jsp:jsp-api',
                'javax.servlet:javax.servlet-api',
                'javax.servlet:jstl',
                'junit:junit',
                'org.apache.geronimo.specs:geronimo-jta_1.1_spec',
                'org.hibernate:hibernate-cglib-repack',
                'org.hibernate:hibernate-core',
                'org.aspectj:aspectjweaver',
                'org.eclipse.persistence:javax.persistence',
                'org.hamcrest:hamcrest-core',
                'org.springframework:spring-beans',
                'org.springframework:spring-context',
                'org.springframework:spring-core',
                'org.springframework:spring-jdbc',
                'org.springframework:spring-orm',
                'org.springframework:spring-tx',
                'org.springframework:spring-web',
                'org.springframework:spring-webmvc',
                'org.springframework:spring-webmvc-portlet',
                'org.testng:testng',
                'xmlunit:xmlunit',
                'taglibs:standard'
        }
    }

    plugins {
        test (":greenmail:1.3.0") {
            export = false
        }
        build ":tomcat:$grailsVersion", ':release:2.2.1', ':rest-client-builder:1.0.3', {
            export = false
        }
    }
}

if (appName == "grails-mail") {
    // use for testing view resolution from plugins
    grails.plugin.location.'for-plugin-view-resolution' = 'plugins/for-plugin-view-resolution'
}

grails.release.scm.enabled = false
