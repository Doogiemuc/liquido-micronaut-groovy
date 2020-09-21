package org.doogie.team

import io.micronaut.core.annotation.Introspected

import javax.validation.constraints.NotBlank

@Introspected
class JoinTeamRequest {
	@NotBlank	String inviteCode
	@NotBlank String userName
	@NotBlank String userEmail
}
