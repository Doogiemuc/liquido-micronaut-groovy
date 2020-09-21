package org.doogie.team

import io.micronaut.core.annotation.Introspected
import org.doogie.user.User

import javax.validation.constraints.NotBlank

@Introspected
class CreateTeamRequest {
	@NotBlank	String teamName
	@NotBlank String adminName
	@NotBlank String adminEmail
}
