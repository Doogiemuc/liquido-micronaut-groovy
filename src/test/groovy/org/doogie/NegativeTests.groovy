package org.doogie

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.uri.UriBuilder
import io.micronaut.runtime.server.EmbeddedServer
import org.doogie.polls.Poll
import org.doogie.teams.Team
import org.grails.datastore.mapping.mongo.MongoDatastore
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * Negative test cases that test for expected Exceptions or expected error codes in responses.
 */
@Slf4j
class NegativeTests extends Specification {

	@Shared
	@AutoCleanup
	EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer)

	//MAYBE: Declarative client (via simple interface) https://piotrminkowski.com/2019/11/12/micronaut-tutorial-reactive/

	@Shared
	@AutoCleanup
	BlockingHttpClient client = HttpClient.create(embeddedServer.URL).toBlocking()

	/* setup embedded mongoDB server => outdated, we are now testing against a locally installed mongoDB
	@Shared
	@AutoCleanup
	MongoDatastore datastore = new MongoDatastore(Team.class)
	*/


	def setupSpec() {
		log.info "=========== Setting up tests "

	}

	long now = System.currentTimeMillis() % 100000;
	static String teamName
	static String adminEmail
	static String adminJwt
	static String userEmail
	static String userJwt
	static String inviteCode


	void "devLogin"() {
		given:
		this.now = System.currentTimeMillis() % 10000;
		this.teamName = "Negative Team "+now
		this.adminEmail = "admin_negative"+now+"@liquido.me"
		this.userEmail  = "negative_user"+now+"@liquido.me"

		when: "GET JWT for user"

		URI uri = UriBuilder.of("/devLogin").queryParam("email", userEmail).queryParam("teamName", teamName).build()
		Map res1 = client.retrieve(HttpRequest.GET(uri), Map.class)
		this.userJwt = res1.get('jwt')
		then:
		this.userJwt

		when: "get JWT for admin"
		Map res2 = client.retrieve(HttpRequest.GET("/devLogin?email="+adminEmail+"&teamName="+teamName), Map.class)
		this.adminJwt = res2.get('jwt')
		then:
		this.adminJwt

	}


	void "create Team should return 400 when teamName is missing"() {
		given:
		long now = System.currentTimeMillis() % 10000;
		JsonBuilder newTeamJson = new JsonBuilder()
		newTeamJson(
				//teamName: "Teamname_"+now,    // <== missing teamname
				adminName: "Amind Name_"+now,
				adminEmail: "admin" + now + "@liquido.me"
		)

		when:
		HttpResponse res = client.exchange(HttpRequest.POST('/team', newTeamJson.toString()), String.class)

		then:
		HttpClientResponseException e = thrown(HttpClientResponseException)
		e.status.code == 400
	}

	void "join Team - with invalid inviteCode should return 400"() {
		given:
		long now = System.currentTimeMillis() % 10000;
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

	// TODO: create poll with too short title should fail!   NEEDS FIX!


}
