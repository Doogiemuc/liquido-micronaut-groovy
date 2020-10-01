package org.doogie.polls

import grails.gorm.annotation.Entity
import io.micronaut.core.annotation.Introspected

import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import java.time.LocalDateTime

/**
 * This class is the digital representation of a user's right to vote in LIQUIDO.
 * Votes are anonymous. So we need a way to authenticate a voter anonymously.
 * LIQUIDO solves this by giving the user a voterToken. And we locally store the <b>hashed</b> value
 * of that voterToken as a "Right2Vote". <b>The voterToken is confidential!</b> Only the user must know his voterToken.
 *
 * Now when the user cast's his vote, he sends an anonymous request with his voterToken. The server calculates the hashedVoterToken
 * and looks it up in the list of stored Right2Votes. If found, then this anonymous voter is allowed to vote.
 */
@Introspected
@Entity
class Right2Vote {
	@NotEmpty String hashedVoterToken
	@NotNull  LocalDateTime expiresAt
	//TODO: @Nullable ObjectId publicProxyId

	static mapping = {
		hashedVoterToken index:true, indexAttributes: [unique:true]
	}

	Right2Vote() { }

	private Right2Vote(String hashedVoterToken, LocalDateTime expiresAt) {
		this.hashedVoterToken = hashedVoterToken
		this.expiresAt = expiresAt
	}

	/**
	 * Create a Right2Vote for the given voterToken
	 * @param voterToken a voters token
	 * @return the Right2Vote with the hashedVoterToken. Expires in 14 days.
	 */
	static Right2Vote fromVoterToken(String voterToken) {
		return new Right2Vote(voterToken.md5(), LocalDateTime.now().plusDays(14))
	}
}
