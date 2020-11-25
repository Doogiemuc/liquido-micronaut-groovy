package org.doogie.teams

import grails.gorm.annotation.Entity
import io.micronaut.core.annotation.Introspected
import org.bson.types.ObjectId

import javax.validation.constraints.NotBlank

/**
 * A User in a team.
 *
 * This class `User` is its own GORM @Entity, but it is only used as an embedded document in a `Team`.
 * It is not its own collection in MongoDB. Users are only part of a given Team.
 *
 * Email is not globally unique! One `email` can be a member of several teams. But each of these `User` will have a different user.id
 */
@Introspected
@Entity
class User {

	/**
	 * Users are embedded in a Team. Each user has a unique id.
	 * This user.id is for example used when referencing a User as the creator of a proposal.
	 */
	ObjectId id

	@NotBlank
	String name;

	@NotBlank
	String email;

	String pictureUrl;

	Boolean isAdmin = false  //MAYBE: roles  Do I need more than two roles?

	User() {}

	/** Create a normal (non-admin) User */
	User(String name, String email) {
		this.id = new ObjectId()
		this.name = name
		this.email = email
	}

	/** Create an admin user */
	static User asAdmin(String name, String email) {
		User admin = new User(name, email)
		admin.setIsAdmin(true)
		return admin
	}

	@Override
	String toString() {
		return 'User' + [id: id, name: name, email: email]
	}
}
