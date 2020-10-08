package org.doogie.polls

import com.fasterxml.jackson.annotation.JsonIgnore
import grails.gorm.annotation.Entity
import io.micronaut.core.annotation.Introspected
import org.bson.types.ObjectId

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * A proposal in a Poll.
 * Proposal does not have its own MongoDB collections. The list of proposals is embedded in a Poll.
 */
@Introspected
@Entity
class Proposal {

	ObjectId id

	@NotBlank	@Size(min=10) String title
	@NotBlank @Size(min=10) String description
	@NotNull  ObjectId createdById

	@JsonIgnore
	List<UUID> supporters = new ArrayList<>()

	Proposal() { }

	Proposal(String title, String description, ObjectId createdById) {
		this.id = new ObjectId()
		this.title = title
		this.description = description
		this.createdById = createdById
	}

	long getNumSupporters() {
		supporters != null ? supporters.size() : 0
	}
}