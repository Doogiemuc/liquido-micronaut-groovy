package org.doogie

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.uri.UriBuilder
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import org.grails.datastore.mapping.mongo.MongoDatastore
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

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


	void "devLogin"() {
		log.info "against backend at "+embeddedServer.URL
		log.info "=================================================================="

		given:
		this.teamName = "Negative Team "+now
		this.adminEmail = "admin_negative"+now+"@liquido.me"
		this.userEmail  = "negative_user"+now+"@liquido.me"

		when: "GET JWT for user"
		URI uri1 = UriBuilder.of("/devLogin").queryParam("email", userEmail).queryParam("teamName", teamName).build()
		Map res1 = client.retrieve(HttpRequest.GET(uri1), Map.class)
		this.userJwt = res1.get('jwt')
		then:
		this.userJwt

		when: "get JWT for admin"
		URI uri2 = UriBuilder.of("/devLogin").queryParam("email", adminEmail).queryParam("teamName", teamName).build()
		Map res2 = client.retrieve(HttpRequest.GET(uri2), Map.class)
		this.adminJwt = res2.get('jwt')
		then:
		this.adminJwt

	}

	void "create Team should return 400 when teamName is missing"() {
		given:
		JsonBuilder newTeamJson = new JsonBuilder()
		newTeamJson(
				//teamName: "Teamname_"+now,    // <== missing teamname
				adminName: "Admin Name_"+now,
				adminEmail: "admin" + now + "@liquido.me"
		)

		when:
		HttpResponse res = client.exchange(HttpRequest.POST('/team', newTeamJson.toString()), String.class)

		then:
		HttpClientResponseException e = thrown(HttpClientResponseException)
		e.status.code == 400
	}

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
