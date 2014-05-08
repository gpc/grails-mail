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
package grails.plugin.mail

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.mail.MailMessage

/**
 * Provides the entry point to the mail sending API.
 */
class MailService implements InitializingBean, DisposableBean {

    static transactional = false

    GrailsApplication grailsApplication
    MailMessageBuilderFactory mailMessageBuilderFactory
	ThreadPoolExecutor mailExecutorService

	private static final int DEFAULT_POOL_SIZE = 5

    MailMessage sendMail(def config, Closure callable) {
        if (isDisabled()) {
            log.warn("Sending emails disabled by configuration option")
            return
        }


        MailMessageBuilder messageBuilder = mailMessageBuilderFactory.createBuilder(config)
        callable.delegate = messageBuilder
        callable.resolveStrategy = Closure.DELEGATE_FIRST
        callable.call(messageBuilder)

        messageBuilder.sendMessage(mailExecutorService)
    }

    MailMessage sendMail(Closure callable) {
        return sendMail(mailConfig, callable)
    }


    ConfigObject getMailConfig() {
        grailsApplication.config.grails.mail
    }

    boolean isDisabled() {
        mailConfig.disabled
    }

	void setPoolSize(Integer poolSize){
		if(poolSize == null) poolSize = DEFAULT_POOL_SIZE
		mailExecutorService.setCorePoolSize(poolSize)
		mailExecutorService.setMaximumPoolSize(poolSize)
	}

	@Override
	public void destroy() throws Exception {
		mailExecutorService.shutdown();
		mailExecutorService.awaitTermination(10, TimeUnit.SECONDS);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		mailExecutorService = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());
		try{
			((ThreadPoolExecutor)mailExecutorService).allowCoreThreadTimeOut(true)
		}catch(MissingMethodException e){
			log.info("ThreadPoolExecutor.allowCoreThreadTimeOut method is missing; Java < 6 must be running. The thread pool size will never go below ${poolSize}, which isn't harmful, just a tiny bit wasteful of resources.", e)
		}
		setPoolSize(mailConfig.poolSize?:null)
	}
}
