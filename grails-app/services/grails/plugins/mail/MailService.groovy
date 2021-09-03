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
 */
package grails.plugins.mail

import grails.config.Config
import grails.core.support.GrailsConfigurationAware
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.mail.MailMessage

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
/**
 * Provides the entry point to the mail sending API.
 */
@Slf4j
@CompileStatic
class MailService implements InitializingBean, DisposableBean, GrailsConfigurationAware {

    static transactional = false

    MailConfig configuration
    MailMessageBuilderFactory mailMessageBuilderFactory
	ThreadPoolExecutor mailExecutorService

	private static final Integer DEFAULT_POOL_SIZE = 5

    MailMessage sendMail(MailConfig config, @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = MailMessageBuilder) Closure callable) {
        if (isDisabled()) {
            log.warn("Sending emails disabled by configuration option")
            return
        }

        MailMessageBuilder messageBuilder = mailMessageBuilderFactory.createBuilder(config)
        callable.delegate = messageBuilder
        callable.resolveStrategy = Closure.DELEGATE_FIRST
        callable.call(messageBuilder)

        return messageBuilder.sendMessage(mailExecutorService)
    }

	MailMessage sendMail(Config config, @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = MailMessageBuilder) Closure callable) {
		return sendMail(new MailConfig(config), callable)
	}

    MailMessage sendMail(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = MailMessageBuilder) Closure callable) {
        return sendMail(configuration, callable)
    }


    boolean isDisabled() {
        configuration.disabled
    }

	void setPoolSize(Integer poolSize){
		mailExecutorService.setMaximumPoolSize(poolSize ?: DEFAULT_POOL_SIZE)
		mailExecutorService.setCorePoolSize(poolSize ?: DEFAULT_POOL_SIZE)
	}

	@Override
	void setConfiguration(Config config) {
		configuration = new MailConfig(config)
	}

	@Override
	public void destroy() throws Exception {
		mailExecutorService.shutdown()
		mailExecutorService.awaitTermination(10, TimeUnit.SECONDS);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		mailExecutorService = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>())

		Integer poolSize = configuration.poolSize
		try{
			((ThreadPoolExecutor)mailExecutorService).allowCoreThreadTimeOut(true)
		}catch(MissingMethodException e){
			log.info("ThreadPoolExecutor.allowCoreThreadTimeOut method is missing; Java < 6 must be running. The thread pool size will never go below ${poolSize}, which isn't harmful, just a tiny bit wasteful of resources.", e)
		}
		setPoolSize(poolSize)
	}
}
