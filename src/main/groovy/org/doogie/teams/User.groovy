package org.doogie.teams

import grails.gorm.annotation.Entity
import io.micronaut.core.annotation.Introspected
import org.bson.types.ObjectId

import javax.validation.constraints.NotBlank

/**
 * A User in a team.
 * This class `User` is its own GORM @Entity, but it is only used as an embedded document in a `Team`.
 * It is not its own collection in MongoDB. Users are only part of a given Team.
 */
@Introspected
@Entity
class User {

	/**
	 * Although users are embedded in a Team, they do have a uuid.
	 * This is used when referencing a User for example as the creator of a proposal.
	 */
	ObjectId id

	@NotBlank
	String name;

	@NotBlank
	String email;

	Boolean isAdmin = false  //MAYBE: roles  Do I need more than two roles?

	User() {}

	User(String name, String email) {
		this.id = new ObjectId()
		this.name = name
		this.email = email
	}

	@Override
	String toString() {
		return '{"name":"' + name + '","email":"'+email+'"}'
	}
}
