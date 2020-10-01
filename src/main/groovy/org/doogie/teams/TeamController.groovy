package org.doogie.teams


import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.http.server.exceptions.HttpServerException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import io.micronaut.security.token.jwt.generator.JwtTokenGenerator
import io.micronaut.validation.Validated
import io.micronaut.validation.validator.Validator
import org.doogie.polls.Right2Vote
import org.doogie.security.LiquidoTokenValidator
import org.springframework.context.annotation.Profile

import javax.inject.Inject
import javax.validation.Valid

@Validated
@Controller
@Slf4j
class TeamController {

	@Get("/")
	@Secured(SecurityRule.IS_ANONYMOUS)
	index() {
		return '{ "msg": "LIQUIDO Mobile REST API", "version": "0.1.0", "status": "ok" }'
	}

	@Inject
	JwtTokenGenerator tokenGenerator

	@Get("/devLogin")
	@Secured(SecurityRule.IS_ANONYMOUS)
	@Profile("test")
	HttpResponse devLogin(@QueryValue String email, @QueryValue String teamName) {
		log.debug "TEST: devLogin for "+email + " in team " + teamName
		String token = tokenGenerator.generateToken("sub": email, "teamName": teamName).orElseThrow(() -> new HttpServerException("cannot generate JWT in devLogin"))
		return HttpResponse.ok([jwt: token])
	}

	@Inject Validator validator

	@Value('${liquido.server.voterTokenSecret}')
	String voterTokenSecret

	/**
	 * Creates a voterToken and and stores it in a Right2Vote
	 * Only the user must know his voterToken!
	 * With this voterToken a user can later cast votes.
	 *
	 * @param seed e.g. a users email
	 * @return the voterToken that only the user must know
	 */
	private String createAndStoreVoterToken(String seed) {
		String voterToken = (seed+voterTokenSecret).md5()				//TODO: replace MD5 bis BCRYPT hashes
		Right2Vote right2Vote = Right2Vote.fromVoterToken(voterToken)
		right2Vote.save(flush: true)
		return voterToken
	}

	/**
	 * Create a new team.
	 * The response contains a JSON Web Token (JWT) which has the userEmail as 'sub' claim and also the teamName in a claim.
	 * @param req a CreateTeamRequest
	 * @return Info about the new team and the JWT for future requests
	 */
	@Post("/team")
	@Secured(SecurityRule.IS_ANONYMOUS)
	HttpResponse createTeam(@Body @Valid CreateTeamRequest req) {
		Team newTeam = new @Valid Team(req.teamName, req.adminName, req.adminEmail)

		// for some reason we must manually run validation. But this is actually nice, because it gives us the opportunity to return a nicely formatted error message
		def constraintViolations = validator.validate(newTeam)
		if (constraintViolations.size() > 0) {
			def messages = constraintViolations.collect {it.getMessage() }
			return HttpResponse.badRequest([
				err: "Invalid create team request",
				details: messages
			])
		}

		newTeam.save(flush: true)
		log.info("New team created: "+newTeam.name)

		// Generate a signed JWT and return it with the team in the response. A JWT MUST contain a "sub" claim!
		String jwt = tokenGenerator.generateToken("sub": req.adminEmail, "teamName": newTeam.name, "roles": ["ROLE_FROM_CUSTOM_JWT"])
		  .orElseThrow(() -> new HttpServerException("Cannot generate JWT."))

		// Generate a LIQUIDO voterToken for the admin
		String voterToken = createAndStoreVoterToken(req.adminEmail)

		return HttpResponse.ok([
			msg: "New team created successfully",
			team: newTeam,
			jwt: jwt,
			voterToken: voterToken
		]  )
	}

	@Put("/joinTeam")
	@Secured(SecurityRule.IS_ANONYMOUS)
	HttpResponse joinTeam(@Body @Valid JoinTeamRequest req) {
		Team team = Team.findByInviteCode(req.inviteCode)            // GORM I LIKE :-)
		if (!team) return HttpResponse.badRequest([err:"Cannot find a team with this inviteCode!"])
		team.members.push(new User(req.userName, req.userEmail))
		team.save(flush: true)

		String jwt = tokenGenerator.generateToken("sub": req.userEmail, "teamName": team.name)
			.orElseThrow(() -> new HttpServerException("cannot generate JWT"))
		String voterToken = createAndStoreVoterToken(req.userEmail)

		return HttpResponse.ok([
			msg: "Joined team",
			team: team,
			jwt: jwt,
			voterToken: voterToken
		])
	}

	//TODO: @Post("/login")  with email and OTT in req.body also for normal users. Also return user's voterToken


	@Get("/team")
	//@Secured(SecurityRule.IS_AUTHENTICATED)		// MUST pass a valid JWT with teamName in "sub" claim
	@Secured([LiquidoTokenValidator.LIQUIDO_ROLE_USER])							// AND user must have this role
	HttpResponse getTeam(Authentication authentication) {
		String userEmail = authentication.getName()
		String teamName = authentication.getAttributes().get(LiquidoTokenValidator.TEAM_NAME_ATTR)

		if (!teamName) return HttpResponse.unauthorized().body([err: "Not authenticated with valid teamName!", teamName: teamName])

		Team team = Team.findByName(teamName)											// this finder method is automatically generated by GORM
		if (!team) return HttpResponse.notFound([err: "Team '+teamName+' found."])
		if (!team.members.find {it.email == userEmail }) {
			return HttpResponse.unauthorized().body([err: "User is not a member of team!", userEmail: userEmail, teamName: teamName])
		}

		return HttpResponse.ok(team)
	}


}
