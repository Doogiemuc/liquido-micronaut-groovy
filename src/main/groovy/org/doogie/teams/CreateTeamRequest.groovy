package org.doogie.teams

import io.micronaut.core.annotation.Introspected

import javax.validation.constraints.NotBlank

@Introspected
class CreateTeamRequest {
	@NotBlank	String teamName
	@NotBlank String adminName
	@NotBlank String adminEmail
}
