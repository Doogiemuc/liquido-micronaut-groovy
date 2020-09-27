package org.doogie.teams

import grails.gorm.annotation.Entity
import io.micronaut.core.annotation.Introspected

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull;

@Introspected
@Entity
class Team {

	@NotBlank
	String name

	/** Keep members as "embedded documents" in MongoDB Teams collection */
	List<User> members = new ArrayList<>()
	static embedded = ['members']

	//the SQL way would be to design a one-to-many relation:  static hasMany = [members:User]

	@NotBlank
	String inviteCode

	Team() { }

	/** Constructor for a Team. Will automatically generate an inviteCode */
	Team(@NotNull String name, @NotNull String adminName, @NotNull String adminEmail) {
		this.name = name
		this.members.push(new User(adminName, adminEmail))
		this.inviteCode = name.md5().substring(0,8).toUpperCase()
	}

	/**
	 * Fetch member of this team by email
	 * @param email members email
	 * @return User with that email or Optional.empty() if that email is not a member of this team
	 */
	Optional<User> getUserByEmail(String email) {
		return Optional.ofNullable(this.members.find({it.email == email}))
	}

	@Override
	String toString() {
		String mems = members.stream().limit(10).collect { it.toString() }.join(', ')
		return '{"name":"' + name + '", "inviteCode":"'+inviteCode+'", "members":[' + mems +  ']}'
	}
}
