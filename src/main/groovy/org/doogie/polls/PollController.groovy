package org.doogie.polls

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.utils.SecurityService
import io.micronaut.validation.Validated
import org.doogie.security.LiquidoTokenValidator
import org.doogie.teams.Team

import javax.inject.Inject

@Validated
@Controller
@Slf4j
class PollController {

	static final int CANNOT_GET_POLLS = 414

	/**
	 * Handle LiquidoRestExceptions
	 * @return nice JSON

	@Error
	HttpResponse<JsonError> handleException(HttpRequest request, LiquidoRestException lex) {
		log.debug("LiquidoException in PollController: "+request.getUri() + " => " + lex.toString())

		//JsonError error = new JsonError(ex.getMessage())
		//		.link(Link.SELF, Link.of(request.getUri()))

		return HttpResponse.badRequest().body(lex)			//TODO: get returned status from lex
	}
	*/


	@Inject
	SecurityService securityService;

	private Team getTeamFormJWT() {
		return securityService.getAuthentication().map(auth -> {
			auth.getAttributes().get(LiquidoTokenValidator.TEAM_NAME_CLAIM)
		}).map(teamName -> {
			Team.findByName(teamName)				// This DB finder method is only called when teamName != null
		}).orElseThrow(
				() -> new HttpStatusException(HttpStatus.UNAUTHORIZED, "Cannot find Team from JWT")
		)
	}

	/**
	 * Get current polls of team
	 * @param auth authenticated user
	 * @return all polls of this team
	 */
	@Get("/polls")
	@Secured([LiquidoTokenValidator.LIQUIDO_ROLE_USER])
	HttpResponse getPollsOfTeam() {
		Team team = getTeamFormJWT()				// may throw unauthorized exception
		List<Poll> polls = Poll.findByTeam(team.name)	// may return <null> !
		HttpResponse.ok(polls ?: [])
	}

	@Post("/polls")
	@Secured([LiquidoTokenValidator.LIQUIDO_ROLE_USER])
	HttpResponse createPoll() {
		Team team = getTeamFormJWT()
		Poll poll = new Poll(team, "A poll title")
		poll.save(flush: true)
		log.info "=========== Admin created new Poll"  // +poll.toString()
		return HttpResponse.ok()
	}


}
