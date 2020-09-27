package org.doogie.polls

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.utils.SecurityService
import io.micronaut.validation.Validated
import org.bson.types.ObjectId
import org.doogie.security.LiquidoTokenValidator
import org.doogie.teams.Team
import org.doogie.teams.User

import javax.inject.Inject
import javax.validation.Valid

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

	private String getTeamName() {
		securityService.getAuthentication().get().getAttributes().get(LiquidoTokenValidator.TEAM_NAME_ATTR)
	}

	Team getTeamOfCurrentUser() {
		return (Team)securityService.getAuthentication().get().getAttributes().get(LiquidoTokenValidator.TEAM_ATTRIBUTE)
	}

	User getCurrentUser() {
		Authentication auth = securityService.getAuthentication().get()
		return auth.getAttributes().get(LiquidoTokenValidator.CURRENT_USER_ATTR)
	}

	private String getCurrentUserEmail() {
		securityService.username().get()
	}

	/**
	 * Get current polls of team
	 * @param auth authenticated user
	 * @return all polls of this team
	 */
	@Get("/polls")
	@Secured([LiquidoTokenValidator.LIQUIDO_ROLE_USER])
	HttpResponse getPollsOfTeam() {
		List<Poll> polls = Poll.findByTeam(getTeamName())			// may return <null> !
		HttpResponse.ok(polls ?: [])
	}

	/**
	 * Get one poll by id
	 * @param pollId
	 * @return the poll with its list of proposals
	 */
	@Get("/polls/{pollId}")
	@Secured([LiquidoTokenValidator.LIQUIDO_ROLE_USER])
	HttpResponse getProposalsOfPoll(@PathVariable("pollId") Long pollId) {
		Poll poll = Poll.findById(pollId)
		if (!poll) return HttpResponse.notFound("Poll not found.")
		HttpResponse.ok(poll)
	}

	/**
	 * Create a new poll
	 * @param createPollReq must contain title
	 * @param auth injected Authentication
	 * @return HTTP ok with info about poll
	 */
	@Post("/polls")
	@Secured([LiquidoTokenValidator.LIQUIDO_ROLE_ADMIN])
	HttpResponse createPoll(@Body Map<String, String> createPollReq, Authentication auth) {
		Team team = getTeamOfCurrentUser()
		Poll poll = new @Valid Poll(team, createPollReq.get("title"))
		poll.save(flush: true)
		log.info "Admin of team '"+team.name+"' created new Poll"+poll
		return HttpResponse.ok(poll)
	}

	@Post("/polls/{pollId}/proposals")
	@Secured([LiquidoTokenValidator.LIQUIDO_ROLE_USER])
	HttpResponse createProposal(@PathVariable("pollId") Long pollId, @Body Map<String, String> createProposalReq) {
		Poll poll = Poll.findById(pollId)
		if (!poll) return HttpResponse.notFound([msg: "Cannot add Proposal. Cannot find poll.id="+pollId])
		Proposal proposal = new Proposal(createProposalReq.get("title"), createProposalReq.get("description"), getCurrentUser().id)
		poll.proposals.push(proposal)
		poll.save(flush: true)
		return HttpResponse.created(proposal)
	}

}
