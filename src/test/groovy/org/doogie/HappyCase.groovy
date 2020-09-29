 package org.doogie

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import org.doogie.polls.Ballot
import org.doogie.polls.Poll
import org.doogie.polls.Proposal
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

	JsonSlurper slurper = new JsonSlurper()


	//TODO: Use declarative HTTP client (via simple interface) https://piotrminkowski.com/2019/11/12/micronaut-tutorial-reactive/

	/**
	 * Connect GORM to MongoDB.
	 * Make sure that there is at least one Team that we can test against.
	 */
	def setupSpec() {
		log.info "=============================================================="
		log.info "====================== RUNNING HAPPY CASE TEST ==============="
		log.info "=============================================================="

		// see doc http://gorm.grails.org/latest/mongodb/manual/#_basic_setup
		Map mongoConfig= [:]
		mongoConfig[MongoDatastore.SETTING_DATABASE_NAME] = "HappyCaseDB"
		MongoDatastore datastore = new MongoDatastore(mongoConfig, Team.class, Poll.class)
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

	@Shared
	long now = System.currentTimeMillis() % 10000;

	static String teamName
	static String inviteCode
	static String adminJwt
	static String userJwt
	static Team team
	static Poll poll


	void "LIQUIDO backend API is available"() {
		given:
		log.info("mongodb.uri = "+this.mongoDbUri)

		when:
		HttpResponse res = client.exchange("/")

		then:
		res.status.code == 200
	}

	void "Create team"() {
		given:
		this.teamName = "HappyTeam_"+now
		def newTeam= [
			teamName: teamName,
			adminName: "Admin Name_"+now,
			adminEmail: "admin" + now + "@liquido.me"
		]

		when:
		HttpResponse res = client.exchange(HttpRequest.POST('/team', newTeam), String.class)
		def json = slurper.parseText(res.body())

		then:
		res.status.code == 200
		json.team.name == teamName
		(this.adminJwt = json.jwt) != null
		(this.inviteCode = json.team.inviteCode) != null
	}

	void "join Team"() {
		assert inviteCode : "Need invite code to join Team"

		given:
		JsonBuilder joinTeamRequest = new JsonBuilder()
		joinTeamRequest(
				inviteCode: inviteCode,
				userName: "User Name_"+now,
				userEmail: "user" + now + "@liquido.me"
		)

		when:
		HttpResponse res = client.exchange(HttpRequest.PUT('/joinTeam', joinTeamRequest.toString()), Map.class)
		this.userJwt = res.body.get().get("jwt")

		then: "joinTeam returned JWT and info about team"
		res.status.code == 200
		this.userJwt
		res.body.get().get("team").get("name") == teamName
	}

	void "Get info about own team"() {
		assert userJwt : "Need JWT to GET info about own team"

		when:
		this.team = client.retrieve(HttpRequest.GET('/team').bearerAuth(userJwt), Team.class)

		then:
		this.team.name == teamName
	}

	void "Create new poll and then get polls of team"() {
		assert adminJwt : "Need JWT to GET polls of team"

		given:
		String title = 'Poll from Happy Case '+now

		when:
		this.poll = client.retrieve(HttpRequest.POST('/polls', '{"title":"'+title+'"}').bearerAuth(adminJwt), Poll.class)

		then:
		poll.title == title
		poll.status == Poll.Status.ELABORATION

		when:
		HttpResponse res2 = client.exchange(HttpRequest.GET('/polls').bearerAuth(adminJwt), String.class)
		log.info "========= polls of team \n" + res2.body()

		then:
		res2.status.code == 200
	}

	void "Add proposals to poll and retrieve them back"() {
		assert userJwt : "Need JWT to add proposal"
		assert poll : "Need poll to add proposal"

		when: "adding a proposal"
		Proposal prop = client.retrieve(HttpRequest.POST("/polls/${poll.id}/proposals", newProposal).bearerAuth(userJwt), Proposal.class)

		then: "saved proposal has an ID and its title is correct"
		prop.id
		prop.title == newProposal.title

		when: "Getting poll with all its proposals"
		Poll poll = client.retrieve(HttpRequest.GET("/polls/${poll.id}").bearerAuth(userJwt), Poll.class)
		log.info "==============", poll

		then: "Proposal is now part of poll"
		poll.proposals.find({it.title == prop.title})

		where: "we add two proposals"
		newProposal << [
				[title: "Happy Proposal 1_"+now, description: "Proposal 1 created by Happy Case in Team "+teamName],
				[title: "Happy Proposal 2_"+now, description: "Proposal 2 created by Happy Case in Team "+teamName],
		]
	}

	void "Start voting phase (admin)"() {
		assert adminJwt : "Need JWT to start voting phase"
		assert poll : "Need poll to start voting phase"
		assert poll.status == Poll.Status.ELABORATION: "Poll must be in status ELABORATION"

		when: "admin starts voting phase"
		poll = client.retrieve(HttpRequest.PUT("/polls/${poll.id}/startVoting", null).bearerAuth(adminJwt), Poll.class)

		then: "returned poll has status VOTING"
		poll.status == Poll.Status.VOTING
	}

	void "Cast vote"() {
		assert userJwt : "Need JWT to cast vote"
		assert poll : "Need poll to cast vote"
		assert poll.status == Poll.Status.VOTING: "Need poll in status VOTING"

		given:
		def voteOrder = [poll.proposals.get(0), poll.proposals.get(1)]
		Ballot ballot = new Ballot("dummyRight2Vote", voteOrder)

		when: "user casts a vote"
		HttpResponse res = client.exchange(HttpRequest.POST("/polls/${poll.id}/vote", ballot).bearerAuth(userJwt))

		then: "returned ballot is accepted(202)"
		res.status == HttpStatus.ACCEPTED
	}



	/**
	 * Make test repeatable and cleanup after themselves
	 */
	def cleanupSpec() {
		log.info("======================== cleanup =====================")
		Team teamUnderTest = Team.findByName(teamName)
		if (teamUnderTest != null) {
			log.debug("Deleting team that was created by HappyCase test: "+teamName)
			teamUnderTest.delete(flush: true)
		} else {
			log.error("Cannot delete teamUnderTest")
		}
	}

}
