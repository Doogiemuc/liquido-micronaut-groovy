package org.doogie.teams

import io.micronaut.core.annotation.Introspected

import javax.validation.constraints.NotBlank

@Introspected
class JoinTeamRequest {
	@NotBlank String inviteCode
	@NotBlank String userName
	@NotBlank String userEmail

	JoinTeamRequest(String inviteCode, String userName, String userEmail) {
		this.inviteCode = inviteCode
		this.userName = userName
		this.userEmail = userEmail
	}
}
