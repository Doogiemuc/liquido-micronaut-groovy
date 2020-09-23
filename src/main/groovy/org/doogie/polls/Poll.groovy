package org.doogie.polls

import grails.gorm.annotation.Entity
import io.micronaut.core.annotation.Introspected
import org.doogie.teams.Team
import org.doogie.teams.User

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull;

@Introspected
@Entity
class Poll {

	enum Status {
		ELABORATION,
		VOTING,
		FINISHED
	}

	class Proposal {
		@NotBlank	String title
		@NotBlank String description
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
	@NotBlank String title
	@NotNull  Status status
	List<Proposal> proposals
	List<Ballot> ballots
	static embedded = ['proposals', 'ballots']

	Poll() { }

	Poll(@NotNull Team team, @NotEmpty String title) {
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
		for (i in 0..Math.min(proposals.size(), max)) {
			buf.append('"' + this.proposals.get(i).title+ '"')
			if (i<max) buf.append(",")
		}
		if (this.proposals.size() > max) buf.append(', ...')
		buf.append(']')
		return [title: this.title, status: this.status, proposals: buf.toString(), numBallots: this.ballots.size()].toString()
	}
}
