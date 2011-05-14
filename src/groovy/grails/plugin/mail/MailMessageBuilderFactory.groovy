/*
 * Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * File modified by Vithun (original source obtained from github.com/gpc/grails-mail).
 */
package grails.plugin.mail

import org.springframework.mail.MailSender
import org.springframework.mail.javamail.JavaMailSender

/**
 * Responsible for creating builder instances, which have dependencies and
 * are not threadsafe.
 */
class MailMessageBuilderFactory {

    def mailSender
    def mailMessageContentRenderer

    MailMessageBuilder createBuilder(MailSender mailSender, ConfigObject config) {
        new MailMessageBuilder((mailSender ? mailSender : this.mailSender), config, mailMessageContentRenderer)
    }
    
    boolean isMimeCapable() {
        mailSender instanceof JavaMailSender
    }
}
