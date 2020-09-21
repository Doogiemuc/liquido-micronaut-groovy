package org.doogie

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import org.doogie.team.Team
import org.grails.datastore.mapping.mongo.MongoDatastore
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

@Slf4j
class HttpApiTest extends Specification {

	@Shared
	@AutoCleanup
	EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer)

	//MAYBE: Declarative client (via simple interface) https://piotrminkowski.com/2019/11/12/micronaut-tutorial-reactive/

	@Shared
	@AutoCleanup
	BlockingHttpClient client = HttpClient.create(embeddedServer.URL).toBlocking()

	def setupSpec() {
		def datastore = new MongoDatastore(Team.class)

	}

	static String teamName
	static String inviteCode
	static String jwt

	void "API is available"() {
		when:
		HttpResponse res = client.exchange("/")

		then:
		res.status.code == 200
	}

	void "create team"() {
		given:
		long now = System.currentTimeMillis() % 10000;
		JsonBuilder newTeamJson = new JsonBuilder()
		this.teamName = "Teamname_"+now
		newTeamJson(
			teamName: teamName,
			adminName: "Admin Name_"+now,
			adminEmail: "admin" + now + "@liquido.me"
		)

		when:
		HttpResponse res = client.exchange(HttpRequest.POST('/team', newTeamJson.toString()), Map.class)
		def body = res.body.get()
		inviteCode = body.get("team").get("inviteCode")
		this.jwt = body.get("jwt")
		println "[TEST OK] Created team: " + body.get("team").toString()

		then:
		this.jwt
		res.status.code == 200
		res.body.get().get("team").get("name") == teamName    // Groovies type inference at runtime is dark voodoo magic :-)
	}

	void "join Team"() {
		given:
		long now = System.currentTimeMillis() % 10000;
		JsonBuilder joinTeamRequest = new JsonBuilder()
		joinTeamRequest(
				inviteCode: inviteCode,
				userName: "User Name_"+now,
				userEmail: "user" + now + "@liquido.me"
		)

		when:
		HttpResponse res = client.exchange(HttpRequest.PUT('/joinTeam', joinTeamRequest.toString()), String.class)
		String body = res.body()

		then:
		res.status.code == 200
		body.contains(teamName)
		body.contains('"inviteCode":"'+inviteCode+'"')
	}


	void "GET info about own team"() {
		assert jwt : "Need JWT to GET info about own team"

		when:
		HttpResponse res = client.exchange(HttpRequest.GET('/team').bearerAuth(jwt), String.class)

		then:
		res.status.code == 200
		res.body.toString().contains(teamName)
	}







	//
	// ============ Negative test cases ===============
	//

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




	void "create Team should return 400 when invalid request"() {
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

}
