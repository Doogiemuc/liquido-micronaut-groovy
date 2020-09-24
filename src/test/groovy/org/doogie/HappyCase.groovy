package org.doogie

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import org.doogie.polls.Poll
import org.doogie.teams.Team
import org.grails.datastore.mapping.mongo.MongoDatastore
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import javax.inject.Inject

@MicronautTest  //(application = org.doogie.Application.class, packages = "org.doogie" /* environments = ["test", "test-happy-case"] */)
@Stepwise
@Slf4j
class HappyCase extends Specification {

	@Shared
	@AutoCleanup
	EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer)

	@Shared
	@AutoCleanup
	BlockingHttpClient client = HttpClient.create(embeddedServer.URL).toBlocking()

	/*  DIRECTLY injecting doesn't seem to work.  So we have to create this stuff ourselfe
	@Inject
	@AutoCleanup
	EmbeddedServer embeddedServer

	@Inject
	@Client("/")
	HttpClient rxClient
	*/


	@Value('${mongodb.uri}')
	String mongoDbUri

	@Inject
	ApplicationContext ctx


	//TODO: USe declarative HTTP client (via simple interface) https://piotrminkowski.com/2019/11/12/micronaut-tutorial-reactive/

	/**
	 * Connect GORM to MongoDB.
	 * Make sure that there is at least one Team that we can test against.
	 */
	def setupSpec() {
		log.info "=============================================================="
		log.info "====================== RUNNING HAPPY CASE TEST ==============="
		log.info "=============================================================="

		// see doc http://gorm.grails.org/latest/mongodb/manual/#_basic_setup
		MongoDatastore datastore = new MongoDatastore(Team.class, Poll.class)
		// MongoDbHost = datastore.getMongoClient().getClusterDescription().clusterSettings.hosts[0]
		log.info "Running tests against MongoDatastore.getDefaultDatabase:() ==" + datastore.getDefaultDatabase()

		for (String dbName : datastore.getMongoClient().listDatabaseNames()) {
			log.info "dbName: " + dbName
		}
		long teamCount = Team.count()
		long pollCount = Poll.count()
		log.info "Got $teamCount Teams and $pollCount Polls in the DB"

		def teams = Team.list(offset: 0, max:1)
		def team = Team.find()

		log.info ""
	}

	long now = System.currentTimeMillis() % 10000;
	static String teamName
	static String inviteCode
	static String adminJwt
	static String userJwt


	void "LIQUIDO backend API is available"() {
		given:
		log.info("mongodb.uri = "+this.mongoDbUri)

		when:
		HttpResponse res = client.exchange("/")

		then:
		res.status.code == 200
	}

	void "create team"() {
		given:
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
		this.adminJwt = body.get("jwt")
		println "[TEST OK] Created team: " + body.get("team").toString()

		then:
		this.adminJwt
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
		HttpResponse res = client.exchange(HttpRequest.PUT('/joinTeam', joinTeamRequest.toString()), Map.class)
		this.userJwt = res.body.get().get("jwt")

		then:
		res.status.code == 200
		this.userJwt
		res.body.get().get("team").get("name") == teamName
	}




	void "GET info about own team"() {
		assert userJwt : "Need JWT to GET info about own team"

		when:
		HttpResponse res = client.exchange(HttpRequest.GET('/team').bearerAuth(userJwt), String.class)

		then:
		res.status.code == 200
		res.body.toString().contains(teamName)
	}

	void "Create new poll and then get polls of team"() {
		assert adminJwt : "Need JWT to GET polls of team"

		given:
		String title = 'a' // 'Poll from Happy Case '+now

		when:
		Poll poll = client.retrieve(HttpRequest.POST('/polls', '{"title":"'+title+'"}').bearerAuth(adminJwt), Poll.class)

		then:
		poll.title == title
		poll.status == Poll.Status.ELABORATION

		when:
		HttpResponse res2 = client.exchange(HttpRequest.GET('/polls').bearerAuth(adminJwt), String.class)
		log.info "========= polls of team \n" + res2.body()

		then:
		res2.status.code == 200
	}


}
