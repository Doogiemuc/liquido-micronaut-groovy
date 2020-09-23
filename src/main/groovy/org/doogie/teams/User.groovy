package org.doogie.teams

import grails.gorm.annotation.Entity
import io.micronaut.core.annotation.Introspected;

import javax.validation.constraints.NotBlank;

@Introspected
@Entity
class User {

	@NotBlank
	String name;

	@NotBlank
	String email;

	User() {}

	User(String name, String email) {
		this.name = name
		this.email = email
	}

	@Override
	String toString() {
		return '{"name":"' + name + '","email":"'+email+'"}'
	}
}
