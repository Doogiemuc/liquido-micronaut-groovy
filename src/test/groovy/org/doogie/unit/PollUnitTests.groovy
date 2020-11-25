package org.doogie.unit

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.uri.UriBuilder
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import org.doogie.polls.Poll
import org.doogie.polls.Proposal
import org.doogie.teams.Team
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import javax.inject.Inject

@MicronautTest  //(application = org.doogie.Application.class, packages = "org.doogie" /* environments = ["test", "test-happy-case"] */)
//@Stepwise
@Slf4j
class PollUnitTest extends Specification {

    @Inject
    EmbeddedServer embeddedServer

    /** Inject the shared reactive RxHttpClient */
    @Inject
    @Shared
    @Client('${micronaut.server.context-path}')
    HttpClient rxClient

    /** We only need a BlockingHttpClient in our tests. This field MUST be static */
    static BlockingHttpClient client

    @Value('${micronaut.server.context-path}')
    String apiPrefix

    @Value('${mongodb.uri}')
    String mongoDbUri

    @Shared
    JsonSlurper slurper = new JsonSlurper()

    def setupSpec() {
        log.info "=================================================================="
        log.info "================= RUNNING HAPPY CASE TESTs ======================="
        log.info "=================================================================="
        client = rxClient.toBlocking()
    }

    /* Static and shared values that can be accessed in every testcase */
    @Shared
    long now = System.currentTimeMillis() % 10000;

    /**
     * Basic static parameters for all unit tests
     */
    static String teamName = "TeamDoogie"

    static String inviteCode

    static String adminEmail
    static String adminJwt

    static String userEmail
    static String userJwt
    static Team team
    static Poll poll
    static String voterToken1   // voter token of user1

    void "devLogin"() {
        log.info "against backend at "+embeddedServer.URL
        log.info("mongodb.uri = "+this.mongoDbUri)
        log.info "=================================================================="

        given:
        this.teamName = "Unit Test Team "+now
        this.adminEmail = "admin_unit"+now+"@liquido.me"
        this.userEmail  = "user_unit"+now+"@liquido.me"

        when: "GET JWT for user"
        URI uri1 = UriBuilder.of("/devLogin").queryParam("userEmail", userEmail).queryParam("teamName", teamName).build()
        Map res1 = client.retrieve(HttpRequest.GET(uri1), Map.class)
        this.userJwt = res1.get('jwt')
        then:
        this.userJwt

        when: "get JWT for admin"
        URI uri2 = UriBuilder.of("/devLogin").queryParam("userEmail", adminEmail).queryParam("teamName", teamName).build()
        Map res2 = client.retrieve(HttpRequest.GET(uri2), Map.class)
        this.adminJwt = res2.get('jwt')
        then:
        this.adminJwt
    }

    void "getPolls"() {
        log.info "against backend at " + embeddedServer.URL + this.apiPrefix
        log.info("mongodb.uri = " + this.mongoDbUri)

        when:
        HttpResponse res = client.exchange(HttpRequest.GET("/polls").bearerAuth(adminJwt))
        log.info(res.body())

        then:
        res.status.code == 200
    }
}