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
 import io.micronaut.http.client.annotation.Client
 import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
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

//	@Shared
//	@AutoCleanup
//	EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer)

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

	@Shared
	JsonSlurper slurper = new JsonSlurper()


	//TODO: Use declarative HTTP client (via simple interface) https://piotrminkowski.com/2019/11/12/micronaut-tutorial-reactive/

	/**
	 * Connect GORM to MongoDB.
	 * Make sure that there is at least one Team that we can test against.
	 */
	def setupSpec() {
		log.info "=================================================================="
		log.info "================= RUNNING HAPPY CASE TESTs ======================="
		log.info "=================================================================="
		client = rxClient.toBlocking()
	}

	/* Static and shared values that can be accessed in every testcase */
	@Shared
	long now = System.currentTimeMillis() % 10000;

	static String teamName
	static String inviteCode
	static String adminJwt
	static String userJwt
	static Team team
	static Poll poll
	static String voterToken1   // voter token of user1


	void "LIQUIDO backend API and mongoDB are available"() {
		// see doc http://gorm.grails.org/latest/mongodb/manual/#_basic_setup
		log.info "against backend at "+embeddedServer.URL
		log.info("mongodb.uri = "+this.mongoDbUri)
		long teamCount = Team.count()
		long pollCount = Poll.count()
		log.info "Got $teamCount Teams and $pollCount Polls in the DB"
		log.info "=================================================================="

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
		json.voterToken						// admin also has a voterToken. We'll use the one returned to the user in the next test step.
		(this.adminJwt = json.jwt) != null
		(this.inviteCode = json.team.inviteCode) != null
	}

	void "Join Team"() {
		assert inviteCode : "Need invite code to join Team"

		given:
		def joinTeamRequest = [
				inviteCode: inviteCode,
				userName: "User Name_"+now,
				userEmail: "user" + now + "@liquido.me"
		]

		when:
		HttpResponse res = client.exchange(HttpRequest.PUT('/joinTeam', joinTeamRequest), String.class)
		def json = slurper.parseText(res.body())

		then: "joinTeam returned JWT and info about team"
		res.status.code == 200
		(this.userJwt = json.jwt) != null
		(this.voterToken1 = json.voterToken) != null
		json.team.name == teamName

	}

	void "Get info about own team"() {
		assert userJwt : "Need JWT to GET info about own team"

		when:
		this.team = client.retrieve(HttpRequest.GET('/team').bearerAuth(userJwt), Team.class)

		then:
		this.team.name == teamName
	}

	/*
	void "Get own user with own voterToken"() {
		assert userJwt : "Need JWT to get voterToken"

		when:
		voterToken1 = client.retrieve(HttpRequest.GET("/my/voterToken").bearerAuth(userJwt), String.class)

		then: "user received a voterToken"
		voterToken1.length() > 10
	}
	*/


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
		poll = client.retrieve(HttpRequest.GET("/polls/${poll.id}").bearerAuth(userJwt), Poll.class)

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
		assert poll.proposals.size() > 1 : "Poll must have at least two proposals"

		when: "admin starts voting phase"
		poll = client.retrieve(HttpRequest.PUT("/polls/${poll.id}/startVoting", "").bearerAuth(adminJwt), Poll.class)

		then: "returned poll has status VOTING"
		poll.status == Poll.Status.VOTING
	}

	void "Cast vote"() {
		assert userJwt : "Need JWT to cast vote"
		assert poll : "Need poll to cast vote"
		assert poll.status == Poll.Status.VOTING: "Need poll in status VOTING"

		given:
		// voteOrder must be passed as list of Strings: each one is a HEX representation of a Mongo ObjectId
		def castVoteRequest = [
			voterToken: voterToken1,
			voteOrder: [poll.proposals.get(0).id.toHexString(), poll.proposals.get(1).id.toHexString()]
		]

		when: "user casts a vote"
		HttpResponse res = client.exchange(HttpRequest.POST("/polls/${poll.id}/vote", castVoteRequest).bearerAuth(userJwt))

		then: "returned ballot is created(201)"
		res.status == HttpStatus.CREATED
	}

	void "Update own ballot"() {
		assert userJwt : "Need JWT to cast vote"
		assert poll : "Need poll to cast vote"
		assert poll.status == Poll.Status.VOTING: "Need poll in status VOTING"

		given:
		// different request than above. Vote only for one proposal here
		def castVoteRequest = [
			voterToken: voterToken1,
			voteOrder: [poll.proposals.get(0).id.toHexString()]
		]

		when: "user updates his ballot"
		HttpResponse res = client.exchange(HttpRequest.POST("/polls/${poll.id}/vote", castVoteRequest).bearerAuth(userJwt))

		then: "returned ballot is accepted(202)"
		res.status == HttpStatus.CREATED
	}

	/**
	 * Make test repeatable and cleanup after themselves
	 * (This cannot be done in a cleanupSpec() method, because the mongoDatastore
	 * is already closed there.)
	 */
	void "Cleanup DB"() {
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
