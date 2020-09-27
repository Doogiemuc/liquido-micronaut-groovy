package org.doogie.polls

import com.fasterxml.jackson.annotation.JsonBackReference
import grails.gorm.annotation.Entity
import io.micronaut.core.annotation.Introspected
import org.doogie.teams.Team

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Introspected
@Entity				// Be careful this is @grails.gorm.annotation.Entity and not @javax.persistence.Entity
class Poll {

	enum Status {
		ELABORATION,
		VOTING,
		FINISHED
	}

	@JsonBackReference // prevent endless recursion Poll -> Team -> team.polls -> ...
	@NotNull
	Team team

	@NotBlank @Size(min=10) String title
	@NotNull  Status status  = Status.ELABORATION
	List<Proposal> proposals = new ArrayList<>()
	List<Ballot> ballots     = new ArrayList<>()

	// MongoDB-GORM
	static embedded = ['proposals', 'ballots']
	static mapping = { collection: "polls" }

	Poll() { }

	Poll(@NotNull Team team, @NotEmpty @NotNull @Size(min=10) String title) {
		this.team      = team
		this.title     = title
	}

	@Override
	String toString() {
		int max = Math.min(proposals.size(), 10)
		StringBuffer buf = new StringBuffer()
		buf.append('[')
		for (int i = 0; i<max; i++) {
			buf.append('"' + this.proposals.get(i).title+ '"')
			if (i < max-1) buf.append(",")
		}
		if (this.proposals.size() > max) buf.append(', ...')
		buf.append(']')
		return [title: this.title, status: this.status, proposals: buf.toString(), numBallots: this.ballots.size()].toString()
	}
}
