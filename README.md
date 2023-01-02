The Grails mail plugin provides a convenient DSL for _sending_ email. It supports plain text, html, attachments, inline resources and i18n among other features.
[![Build Status](https://travis-ci.org/grails3-plugins/mail.svg?branch=master)](https://travis-ci.org/grails3-plugins/mail)

Mail can be sent using the @mailService@ via the @sendMail@ method. Here is an example…

    mailService.sendMail {
       to "fred@gmail.com","ginger@gmail.com"
       from "john@gmail.com"
       cc "marge@gmail.com", "ed@gmail.com"
       bcc "joe@gmail.com"
       subject "Hello John"
       text 'this is some text'
    }

Please see the [User Guide](https://grails3-plugins.github.io/mail/ "Grails Mail Plugin @ GitHub") for more information.

The plugin is released under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html "Apache License, Version 2.0 - The Apache Software Foundation").

## Versions

* 3.x - Compatible with Grails 4
* 2.x - Compatible with Grails 3
* 1.x - Compatible with Grails 2

## Issues

Issues can be raised via  [GitHub Issues](https://github.com/grails3-plugins/mail/issues).

## Contributing

Pull requests are the preferred method for submitting contributions. Please open an issue via that issue tracker link above and create an issue describing what your contribution addresses.

If you are contributing documentation, raising an issue is not necessary.

