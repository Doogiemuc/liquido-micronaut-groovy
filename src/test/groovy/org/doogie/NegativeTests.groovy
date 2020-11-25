package org.doogie

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.uri.UriBuilder
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Shared
import spock.lang.Specification

import javax.inject.Inject

/**
 * Negative test cases that test for expected Exceptions or expected error codes in responses.
 */
@MicronautTest //(application = org.doogie.Application.class, packages = "org.doogie" /* environments = ["test", "test-happy-case"] */)
@Slf4j
class NegativeTests extends Specification {

	@Inject
	EmbeddedServer embeddedServer

	/** Inject the shared reactive RxHttpClient */
	@Inject
	@Shared
	@Client("/")
	HttpClient rxClient

	/** We only need a BlockingHttpClient in our tests. This field MUST be static */
	static BlockingHttpClient client

	/** We can simply inject the MongoDatastore that has already been initialized by micronaut-mongo-gorm */
	//@Inject
	//@AutoCleanup   BUGFIX: No autocleanup. Otherwise mongo client will be closed too soon.
	//MongoDatastore mongoDatastore

	@Value('${mongodb.uri}')
	String mongoDbUri

	def setupSpec() {
		log.info "=================================================================="
		log.info "================= RUNNING NEGATIVE TESTs   ======================="
		log.info "=================================================================="
		client = rxClient.toBlocking()
	}

	@Shared
	long now = System.currentTimeMillis() % 100000;
	static String teamName
	static String adminEmail
	static String adminJwt
	static String userEmail
	static String userJwt
	static String inviteCode


	void "create Team should return 400 when teamName is too short"() {
		given:
		def newTeamJson = [
			teamName: "1",    // <== too short teamname
			adminName: "Admin Name_"+now,
			adminEmail: "admin" + now + "@liquido.me"
		]

		when:
		HttpResponse res = client.exchange(HttpRequest.POST('/team', newTeamJson), String.class)

		then:
		HttpClientResponseException e = thrown(HttpClientResponseException)
		e.status.code == 400
	}



	void "join Team - with invalid inviteCode should return 400"() {
		given:
		JsonBuilder joinTeamRequest = new JsonBuilder()
		joinTeamRequest(
				inviteCode: "WRONG_INVITE_CODE",
				userName: "Admin Name_"+now,
				userEmail: "admin" + now + "@liquido.me"
		)

		when:
		HttpResponse res = client.exchange(HttpRequest.PUT('/joinTeam', joinTeamRequest.toString()), String.class)

		then:
		HttpClientResponseException e = thrown(HttpClientResponseException)
		e.status.code == 400
	}

	//TODO: create poll with too short title should fail!   NEEDS FIX!

	//TODO: add proposal must not be allowed, if poll is not in status ELABORATION

	//TODO: start voting phase when there is only one proposal => fail

}
