package org.doogie.liquido

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.utils.SecurityService
import org.doogie.polls.Ballot
import org.doogie.polls.Poll
import org.doogie.polls.Right2Vote
import org.doogie.security.LiquidoTokenValidator
import org.doogie.teams.Team
import org.doogie.teams.User

import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.constraints.NotNull
import java.time.LocalDateTime

/**
 * Collection of LIQUIDO Tools that are used in multiple places.
 */
@Singleton
@Slf4j
class LiquidoUtils {

	@Value('${liquido.server.voterTokenSecret}')
	String voterTokenSecret

	@Inject
	SecurityService securityService;


	String getTeamName() {
		securityService.getAuthentication().get().getAttributes().get(LiquidoTokenValidator.TEAM_NAME_ATTR)
	}

	Team getTeamOfCurrentUser() {
		return (Team)securityService.getAuthentication().get().getAttributes().get(LiquidoTokenValidator.TEAM_ATTRIBUTE)
	}

	User getCurrentUser() {
		Authentication auth = securityService.getAuthentication().get()
		return auth.getAttributes().get(LiquidoTokenValidator.CURRENT_USER_ATTR)
	}

	/*
	String getCurrentUserEmail() {
		securityService.username().get()
	}
  */




	/**
	 * Calculate the user's digital representation of his right to vote.
	 * A user's right2Vote
	 *  - is secret
	 *  - must only be known to the user
	 *  - is calculated as the hash of the user's email, the teamid and some secret that only the server knows.
	 *  - is globally unique
	 *  - can only be created by the server.
	 *
	 * This method does not store the Right2Vote. @see createAndStoreRight2Vote
	 *
	 * @param email user's email address, used as seed
	 * @return the user's Right2Vote
	 */
	String calcVoterToken(String email, String teamId) {
		return (email+teamId+voterTokenSecret).md5()				//TODO: replace MD5 with BCRYPT hashes
	}

	private String calcHashedVoterToken(String voterToken) {
		return voterToken.md5()
	}

	/**
	 * Creates a voterToken for a given user and and stores its hash in right2Vote
	 * Only the user must know his voterToken! Each user has one voter token (in the team).
	 * With this voterToken a user can later cast votes.
	 * Will update TTL of existing Right2Vote if it already exists.
	 *
	 * @param seed e.g. a users email
	 * @return the voterToken that only the user must know
	 */
	String createVoterTokenAndStoreRightToVote(@NotNull String email, @NotNull String teamId) {
		String voterToken       = calcVoterToken(email, teamId)
		String hashedVoterToken = calcHashedVoterToken(voterToken)
		// upsert
		Right2Vote right2Vote = Right2Vote.findByHashedVoterToken(hashedVoterToken)
		if (right2Vote) {
			right2Vote.setExpiresAt(LocalDateTime.now().plusDays(14))
		} else {
			right2Vote = new Right2Vote(hashedVoterToken, LocalDateTime.now().plusDays(14))
		}
		try {
			right2Vote.save(flush: true)
		} catch (Exception e) {
			log.error("Cannot create Right2Vote for "+email+ " in team.id="+teamId, e)
		}
		return voterToken
	}


	/**
	 * Check if voterToken hashes to a valid Right2Vote.
	 * Right2Vote must have been created and stored before.
	 * @param voterToken a user's voterToken
	 * @return Right2Vote or Optional.empty() if unknown.
	 */
	Optional<Right2Vote> isVoterTokenValid(String voterToken) {
		String hashedVoterToken = calcHashedVoterToken(voterToken)
		Right2Vote right2Vote   = Right2Vote.findByHashedVoterToken(hashedVoterToken)
		return Optional.ofNullable(right2Vote)
	}

	/**
	 * Get a user's ballot, if he already voted in this poll
	 * @param poll a Poll with ballots
	 * @param user currently logged in user
	 * @return the user's ballot if he already voted or otherwise Option.empty()
	 */
	Optional<Ballot> getBallotOfUser(Poll poll, User user) {
		String voterToken = calcVoterToken(user.email, poll.team.id.toString())
		Optional<Right2Vote> right2Vote = isVoterTokenValid(voterToken)
		if (!right2Vote.isPresent()) return Optional.empty()
		Ballot ballot = poll.getBallots().find { it.right2Vote == right2Vote.get()}			//TODO: TEST: Does equals work here? compare by Ballot.ObjectID?
		return Optional.ofNullable(ballot)
	}
}
