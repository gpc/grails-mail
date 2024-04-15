# Grails Mail Plugin

[![Maven Central](https://img.shields.io/maven-central/v/org.grails.plugins/mail.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.grails.plugins/mail)
[![Java CI](https://github.com/grails/grails-mail/actions/workflows/gradle.yml/badge.svg?event=push)](https://github.com/grails/grails-mail/actions/workflows/gradle.yml)

## About

The Grails mail plugin provides a convenient DSL for _sending_ email. It supports plain text, html, attachments, inline resources and i18n among other features.

Mail can be sent using the `mailService.sendMail` method. Here is an example…
```groovy
mailService.sendMail {
   to 'fred@gmail.com', 'ginger@gmail.com'
   from 'john@gmail.com'
   cc 'marge@gmail.com', 'ed@gmail.com'
   bcc 'joe@gmail.com'
   subject 'Hello John'
   text 'this is some text'
}
```

## Documentation

[Latest documentation](https://grails.github.io/grails-mail/latest/) and [snapshots](https://grails.github.io/grails-mail/snapshot/) are available.

## Versions

| Branch | Grails Version |
|--------|----------------|
| 1.x    | 2              |
| 2.x    | 3              |
| 3.x    | 4-5            |
| 4.x    | 6              |

## Issues

Issues can be raised via [GitHub Issues](https://github.com/grails/grails-mail/issues).

## Contributing

Pull requests are the preferred method for submitting contributions. Please open an issue via that issue tracker link above and create an issue describing what your contribution addresses.

If you are contributing documentation, raising an issue is not necessary.