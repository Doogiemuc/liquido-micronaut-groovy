package org.doogie

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.context.env.Environment
import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.event.annotation.EventListener

import javax.inject.Inject
import javax.inject.Singleton

// Bugfix!!!! Important!!!

/**
 * Perform some sanity checks on micronaut startup
 */
@Singleton
@Slf4j
class StartupEventListener {

	@Value('${liquido.server.voterTokenSecret}')
	String voterTokenSecret

	@Inject
	Environment env

	@EventListener
	public void onStartupEvent(StartupEvent event) {
		assert !io.micronaut.core.util.StringUtils.isEmpty(voterTokenSecret) : "Need liquido.server.voterTokenSecret in application-<env>.yml"

		//TODO: sanity check DB ...


	}
}
