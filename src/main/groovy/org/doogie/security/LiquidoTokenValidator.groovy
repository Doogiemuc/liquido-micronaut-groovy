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
import org.reactivestreams.Publisher

import javax.inject.Singleton
import java.text.ParseException
import java.util.concurrent.Flow

/**
 * Validate a given JWT and return an {@link Authentication} that contains the {@link UserDetails}.
 *
 * @See JwtTokenValidator
 * @See DefaultJwtAuthenticationFactory
 */
@Singleton
@Slf4j
class LiquidoTokenValidator implements TokenValidator {

	static final String TEAM_NAME_CLAIM = "teamName"
	static final String LIQUIDO_ROLE_USER = "LIQUIDO_ROLE_USER"
	static final String LIQUIDO_ROLE_ADMIN = "LIQUIDO_ROLE_ADMIN"

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

		String username = jwt.getJWTClaimsSet().getSubject()
		if (!username) {
			log.debug("Cannot extract username from JWT claim 'sub'")
			return Flowable.empty()
		}

		String teamName = jwt.getJWTClaimsSet().getStringClaim(TEAM_NAME_CLAIM)
		if (!teamName) {
			log.debug("JWT is invalid. It has no claim "+TEAM_NAME_CLAIM)
			return Flowable.empty()
		}


		//TODO: Should I check that team exists?

		Map<String, Object> attributes = new HashMap<>()
		attributes[TEAM_NAME_CLAIM] = teamName
		//TODO: load user's roles from DB.Def
		Collection<String> roles = [LIQUIDO_ROLE_USER]															// Groovy I like! :-) Easily create lists

		UserDetails userDetails = new UserDetails(username, roles, attributes)
		Authentication auth = new AuthenticationUserDetailsAdapter(userDetails, TokenConfiguration.DEFAULT_ROLES_NAME, TokenConfiguration.DEFAULT_NAME_KEY)

		//TODO: cache the response

		return Flowable.just(auth)
	}


}
