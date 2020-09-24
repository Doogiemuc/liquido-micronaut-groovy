package org.doogie.polls

import com.mongodb.lang.NonNull
import grails.gorm.annotation.Entity
import io.micronaut.core.annotation.Introspected
import io.micronaut.validation.Validated
import org.doogie.teams.Team
import org.doogie.teams.User

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size;

@Introspected
@Entity				// Be careful this is @grails.gorm.annotation.Entity and not @javax.persistence.Entity
class Poll {

	enum Status {
		ELABORATION,
		VOTING,
		FINISHED
	}

	class Proposal {
		@NotBlank	@Size(min=10) String title   //TODO: @NonNull  mongodb
		@NotBlank @Size(min=10) String description
		@NotNull  User createdBy
		List<User> supporters
		long getNumSupporters() {
			supporters != null ? supporters.size() : 0
		}
	}

	class Ballot {
		@NotBlank String right2Vote
		@NotNull  List<Proposal> voteOrder
	}

	@NotNull  Team team
	@NotBlank @Size(min=10) String title
	@NotNull  Status status
	List<Proposal> proposals
	List<Ballot> ballots
	static embedded = ['proposals', 'ballots']

	Poll() { }

	Poll(@NotNull Team team, @NotEmpty @NotNull @Size(min=10) String title) {
		this.team      = team
		this.title     = title
		this.status    = Status.ELABORATION
		this.proposals = new ArrayList<>()
		this.ballots   = new ArrayList<>()
	}

	@Override
	String toString() {
		int max = 10
		StringBuffer buf = new StringBuffer()
		buf.append('[')
		for (i in 0..Math.min(proposals.size()-1, max)) {
			buf.append('"' + this.proposals.get(i).title+ '"')
			if (i<max) buf.append(",")
		}
		if (this.proposals.size() > max) buf.append(', ...')
		buf.append(']')
		return [title: this.title, status: this.status, proposals: buf.toString(), numBallots: this.ballots.size()].toString()
	}
}
