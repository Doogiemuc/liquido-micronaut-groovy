package org.doogie.polls

import grails.gorm.annotation.Entity
import io.micronaut.core.annotation.Introspected
import org.bson.types.ObjectId

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

/**
 * A Ballot contains the anonymous right2vote and the casted voteOrder.
 * A Ballot does not have its own MongoDB collection. The list of ballots is embedded in a Poll.
 */
@Introspected
@Entity
class Ballot {
	ObjectId id
	@NotBlank String right2Vote
	@NotNull  List<Proposal> voteOrder
	Ballot() { }

}
