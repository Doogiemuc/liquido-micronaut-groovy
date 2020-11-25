package org.doogie

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.context.env.Environment
import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.runtime.server.EmbeddedServer

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Perform some sanity checks on micronaut startup
 */
@Singleton
@Slf4j
class StartupEventListener {

	@Value('${liquido.server.voterTokenSecret}')
	String voterTokenSecret

	@Value('${micronaut.server.context-path}')
	String contextPath

	@Value('${mongodb.uri}')
	String mongoDbUri


	@Inject
	EmbeddedServer embeddedServer

	@Inject
	Environment env

	@EventListener
	public void onStartupEvent(StartupEvent event) {
		assert !io.micronaut.core.util.StringUtils.isEmpty(voterTokenSecret) : "Need liquido.server.voterTokenSecret in application-<env>.yml"
		//TODO: create typesafe org.doogie.liquido.LiquidoConfig with annotations

		//TODO: sanity check DB ...
		log.info("==========================================================================")
		log.info("LIQUIDO backend is running at " + embeddedServer.getURL() + contextPath)
		log.info("Connected to Database " + mongoDbUri)
		log.info("==========================================================================")

	}
}
