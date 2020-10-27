package org.doogie.teams

import grails.gorm.annotation.Entity
import io.micronaut.context.annotation.Value
import io.micronaut.core.annotation.Introspected
import io.micronaut.validation.Validated
import org.doogie.liquido.LiquidoConfig

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

/**
 * A Team in LIQUIDO is the topmost entity.
 * Each team consists of a list of members, which is embedded in the MongoDB document.
 * With the inviteCode further users can be invited to the team.
 */
@Introspected
@Entity
@Validated
class Team {

	@NotBlank
	@Size(min = 5, message = "Team.name must have at least 5 characters")
	String name

	/**
	 * The member's of a team are users.
	 * Every team MUST have at least one admin. He is the first User in this array.
	 */
	@NotEmpty
	List<User> members = new ArrayList<>()
	static embedded = ['members']				// Keep members as "embedded documents" in MongoDB Teams collection
																			// The "old" relation-db way would be to design a one-to-many relation:  static hasMany = [members:User]

	@NotBlank
	String inviteCode

	Team() { }

	/** Constructor for a Team. Will automatically generate an inviteCode */
	Team(@NotBlank String name, @NotBlank String adminName, @NotBlank String adminEmail, String inviteCode) {
		this.name = name
		this.members.push(User.asAdmin(adminName, adminEmail))
		this.inviteCode = inviteCode
	}

	/**
	 * Fetch member of this team by email
	 * @param email members email
	 * @return User with that email or Optional.empty() if that email is not a member of this team
	 */
	Optional<User> getUserByEmail(String email) {
		return Optional.ofNullable(this.members.find({it.email == email}))
	}

	// These getters will be returned in JSON

	User getAdmin() {
		return members.get(0)
	}

	@Override
	String toString() {
		String mems = members.stream().limit(10).collect { it.toString() }.join(', ')
		return '{"name":"' + name + '", "inviteCode":"'+inviteCode+'", "members":[' + mems +  ']}'
	}
}
