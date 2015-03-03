/*
 * Copyright 2015 the original author or authors.
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
 */
package grails.plugins.mail

import grails.compiler.traits.TraitInjector
import groovy.transform.CompileStatic
import org.grails.core.DefaultGrailsControllerClass
import org.grails.core.DefaultGrailsServiceClass

@CompileStatic
class SendMailTraitInjector implements TraitInjector {

    @Override
    Class getTrait() {
        SendMail
    }

    @Override
    String[] getArtefactTypes() {
        [DefaultGrailsServiceClass.SERVICE, DefaultGrailsControllerClass.CONTROLLER] as String[]
    }
}
