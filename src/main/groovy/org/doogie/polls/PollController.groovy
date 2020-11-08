package org.doogie.polls


import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.validation.Validated
import org.doogie.liquido.LiquidoUtils
import org.doogie.security.LiquidoTokenValidator
import org.doogie.teams.Team

import javax.inject.Inject
import javax.validation.Valid
import javax.validation.constraints.NotNull
import java.time.LocalDateTime

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

		return HttpResponse.badRequest().body(lex)
	}
	*/

	@Inject
	LiquidoUtils liquidoUtils


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
	HttpResponse getPoll(@PathVariable("pollId") Long pollId) {
		Poll poll = Poll.findById(pollId)
		if (!poll) return HttpResponse.notFound("Poll not found.")
		//Do not just simply return the Poll @Entity. Do not expose ballots!
		def res = [
			id: poll.id,
		  title: poll.title,
			status: poll.status,
			proposals: poll.proposals,
			teamId: poll.team.id,
			//Do NOT expose poll.ballots!
			ballotOfUser: liquidoUtils.getBallotOfUser(poll, liquidoUtils.getCurrentUser())  // might be null if user did not vote yet
		]
		HttpResponse.ok(res)
	}

	/**
	 * Create a new poll
	 * @param createPollReq must contain title
	 * @param auth injected Authentication
	 * @return HTTP ok with info about poll
	 */
	@Post("/polls")
	@Secured([LiquidoTokenValidator.LIQUIDO_ROLE_ADMIN])
	HttpResponse createPoll(@Body Map<String, String> createPollReq) {
		Team team = liquidoUtils.getTeamOfCurrentUser()
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
		if (!poll.status == Poll.Status.ELABORATION) return HttpResponse.badRequest([msg: "Cannot add Proposal. Poll must be in status ELABORATION but poll.status = "+poll.status])
		Proposal proposal = new Proposal(createProposalReq.get("title"), createProposalReq.get("description"), liquidoUtils.getCurrentUser().id)
		poll.proposals.push(proposal)
		poll.save(flush: true)
		return HttpResponse.created(proposal)
	}

	@Put("/polls/{pollId}/startVoting")
	@Secured([LiquidoTokenValidator.LIQUIDO_ROLE_ADMIN])
	HttpResponse startVotingPhase(@PathVariable("pollId") Long pollId) {
		Poll poll = Poll.findById(pollId)
		if (!poll) return HttpResponse.notFound([msg: "Cannot start voting phase. Cannot find poll.id="+pollId])
		if (!poll.status == Poll.Status.VOTING) return HttpResponse.ok([msg: "Poll was already in status VOTING"])   // PUT is idempotent. If called multiple times, this is ok. Poll will stay in status voting
		if (!poll.status == Poll.Status.ELABORATION) return HttpResponse.badRequest([msg: "Cannot start voting phase. Poll must be in status ELABORATION but poll.status = "+poll.status])
		if (poll.proposals.size() < 2) return HttpResponse.badRequest([msg: "Cannot start voting phase. Poll must have at least two proposals."])
		poll.setStatus(Poll.Status.VOTING)
		poll.save(flush: true)
		return HttpResponse.created(poll)
	}

	@Post("/polls/{pollId}/vote")
	@Secured(SecurityRule.IS_ANONYMOUS)   			// Casting a vote is anonymous !!!
	HttpResponse castVote(@PathVariable("pollId") Long pollId, @Body @NotNull Map<String, Object> castVoteReq) {
		// Sanity checks: Simple and obvious ones first, before hitting the DB!
		String voterToken = castVoteReq.get("voterToken")
		List<String> voteOrder = (List<String>)castVoteReq.get("voteOrder")
		if (!voterToken || voterToken.length() < 10) return HttpResponse.badRequest([err: "Cannot cast vote. Need valid voterToken!"])
		if (!castVoteReq.get("voteOrder")) return HttpResponse.badRequest([err: "Cannot cast vote. Need voteOrder (as Array of IDs) in request!"])

		// Check if voterToken hashes to a valid right2Vote
		Right2Vote right2Vote   = liquidoUtils.isVoterTokenValid(voterToken)
		if (!right2Vote) return HttpResponse.badRequest([err: "Cannot cast vote. VoterToken is unknown."])
		if (LocalDateTime.now().isAfter(right2Vote.expiresAt)) return HttpResponse.badRequest([err: "Cannot cast vote. VoterToken is expired."])

		// Load poll from DB
		Poll poll = Poll.findById(pollId)
		if (!poll) return HttpResponse.notFound([err: "Cannot cast vote. Cannot find poll.id="+pollId])
		if (poll.status != Poll.Status.VOTING) return HttpResponse.badRequest([err: "Cannot cast vote. Poll must be in status VOTING but poll.status = "+poll.status])

		// update existing Ballot or add new Ballot
		Ballot ballot = poll.ballots.find {it.right2Vote == right2Vote }
		if (ballot) {													// update existing Ballot
			ballot.voteOrder = voteOrder
			log.info("Cast Vote in poll(id=$poll.id): voteOrder=$voteOrder (update Ballot)")
		} else {															// add new Ballot
			ballot = new Ballot(right2Vote, voteOrder)
			poll.ballots.push(ballot)
			log.info("Cast Vote in poll(id=$poll.id): voteOrder=$voteOrder")
		}

		poll.save(flush: true)
		return HttpResponse.created(ballot)   // Do NOT return the poll with all ballots!
	}

}
