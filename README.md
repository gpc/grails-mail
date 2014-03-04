The Grails mail plugin provides a convenient DSL for _sending_ email. It supports plain text, html, attachments, inline resources and i18n among other features.
[![Build Status](https://api.travis-ci.org/gpc/grails-mail.png)](http://travis-ci.org/gpc/grails-mail)

Mail can be sent using the @mailService@ via the @sendMail@ method. Here is an example…

    mailService.sendMail {
       to "fred@gmail.com","ginger@gmail.com"
       from "john@gmail.com"
       cc "marge@gmail.com", "ed@gmail.com"
       bcc "joe@gmail.com"
       subject "Hello John"
       text 'this is some text'
    }

Please see the [User Guide](http://gpc.github.com/grails-mail/ "Grails Mail Plugin @ GitHub") for more information.

The plugin is released under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html "Apache License, Version 2.0 - The Apache Software Foundation") and is produced under the [Grails Plugin Collective](http://gpc.github.com/).

## Issues

Issues can be raised via the [Codehaus Jira](http://jira.codehaus.org/browse/GRAILSPLUGINS/component/13340 "Grails Plugins: Grails-Mail - jira.codehaus.org").

## Contributing

Pull requests are the preferred method for submitting contributions. Please open an issue via that issue tracker link above and create an issue describing what your contribution addresses.

If you are contributing documentation, raising an issue is not necessary.

