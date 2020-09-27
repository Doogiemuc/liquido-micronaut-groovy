package org.doogie.security

import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTParser
import edu.umd.cs.findbugs.annotations.Nullable
import groovy.util.logging.Slf4j
import io.micronaut.core.order.Ordered
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.authentication.AuthenticationUserDetailsAdapter
import io.micronaut.security.authentication.UserDetails
import io.micronaut.security.token.config.TokenConfiguration
import io.micronaut.security.token.validator.TokenValidator
import io.reactivex.Flowable
import org.doogie.teams.Team
import org.doogie.teams.User
import org.reactivestreams.Publisher

import javax.inject.Singleton
import java.text.ParseException

/**
 * Validate a given JWT and return an {@link Authentication} that contains the {@link UserDetails}.
 *
 * @See JwtTokenValidator
 * @See DefaultJwtAuthenticationFactory
 */
@Singleton
@Slf4j
class LiquidoTokenValidator implements TokenValidator {

	static final String TEAM_NAME_ATTR         = "teamName"
	static final String CURRENT_USER_ATTR      = "currentUser"
	static final String TEAM_ATTRIBUTE         = "team"
	static final String LIQUIDO_ROLE_USER      = "LIQUIDO_ROLE_USER"
	static final String LIQUIDO_ROLE_ADMIN     = "LIQUIDO_ROLE_ADMIN"

	/** MUST set order higher then Micronaut's JwtTokenValidator, otherwise this LiquidoTokenValidator will not be called at all! */
	@Override
	int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE
	}

	@Override
	@Deprecated
	Publisher<Authentication> validateToken(String token) {
		throw new DeprecationException("This method is deprecated!")
	}

	/**
	 * Validate the given JWT token.
	 * Token must contain a 'sub' claim, which will be used as username.
	 * Token must contain a 'teamName' claim.
	 * @param token encoded JWT token value
	 * @param request the HTTP request
	 * @return Authentication with UserDetails in it, if token is valid. Flowable.empty() otherwise.
	 */
	@Override
	Publisher<Authentication> validateToken(String token, @Nullable HttpRequest<?> request) {
		JWT jwt
		try {
			jwt = JWTParser.parse(token)
		} catch (ParseException e) {
			log.debug("Cannot parse JWT: " + e.getMessage())
			return Flowable.empty()   // not authenticated
		}

		// Get subject from JWT == email
		String email = jwt.getJWTClaimsSet().getSubject()
		if (!email) {
			log.debug("Cannot extract username from JWT claim 'sub'")
			return Flowable.empty()
		}

		// get teamname from JWT
		String teamName = jwt.getJWTClaimsSet().getStringClaim(TEAM_NAME_ATTR)
		if (!teamName) {
			log.debug("JWT is invalid. It has no claim "+TEAM_NAME_ATTR)
			return Flowable.empty()
		}

		// lookup team in DB
		Team team = Team.findByName(teamName)
		if (!team) {
			log.debug("Team '"+teamName+"' not found. => Unauthorized")
			return Flowable.empty()
		}

		// check if User with that email is member of Team
		User user = team.members.find {it.email == email }
		if (!user) {
			log.debug("User with .email='"+email+"' is not part of team '"+teamName+"' => Unauthorized")
			return Flowable.empty()
		}

		// Put the teamName and the whole team into the attributes of the UserDetails
		Map<String, Object> attributes = new HashMap<>()
		attributes[TEAM_NAME_ATTR]    = teamName
		attributes[TEAM_ATTRIBUTE]    = team
		attributes[CURRENT_USER_ATTR] = user

		//TODO: load user's roles from DB  _OR_  add role admin if given in JWT
		Collection<String> roles = [LIQUIDO_ROLE_USER, LIQUIDO_ROLE_ADMIN]															// Groovy I like! :-) Easily create lists

		UserDetails userDetails = new UserDetails(email, roles, attributes)
		Authentication auth = new AuthenticationUserDetailsAdapter(userDetails, TokenConfiguration.DEFAULT_ROLES_NAME, TokenConfiguration.DEFAULT_NAME_KEY)

		//TODO: implement a LiquidoAuthenticationAdapter that contains all the info we currently put into UserDetailAttributes

		return Flowable.just(auth)
	}


}
