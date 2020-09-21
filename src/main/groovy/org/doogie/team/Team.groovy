package org.doogie.team

import grails.gorm.annotation.Entity
import io.micronaut.core.annotation.Introspected
import org.doogie.user.User;

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull;

@Introspected
@Entity
class Team {

	@NotBlank
	String name

	List<User> members

	@NotBlank
	String inviteCode

	static embedded = ['members']

	Team() { }

	/** Constructor for a Team. Will automatically generate an inviteCode */
	Team(@NotNull String name, @NotNull String adminName, @NotNull String adminEmail) {
		this.name = name
		this.members = new ArrayList<>()
		this.members.push(new User(adminName, adminEmail))
		this.inviteCode = name.md5().substring(0,8).toUpperCase()
	}

	@Override
	String toString() {
		String mems = members.stream().limit(10).collect { it.toString() }.join(', ')
		return '{"name":"' + name + '", "inviteCode":"'+inviteCode+'", "members":[' + mems +  ']}'
	}
}
