# LIQUIDO Mobile REST API

Groovy backend for LIQUIDO mobile App (pwa) implemented with Micronaut

Implemented in Groovy with [Micronaut](https://docs.micronaut.io) and [GORM](https://gorm.grails.org/latest/mongodb/manual/index.html)


... WORK IN PROGRESS ...

# Security - Login with Json Web Token (JWT)

## Inital "registration"

 - => `POST /createTeam`  or `POST /joinTeam` with valid inviteCode
 - The custom LIQUIDO JWT is created in `TeamController.groovy`
 - <= HttpResponse.body then contains the JWT that must be sent in every future request

## How micronaute validates JWTs

  1. `TokenAuthenticationFetcher implements AuthenticationFetcher` Attempts to retrieve a token form the HttpRequest
  2. `DefaultTokenResolver implements TokenResolver` returns the first token found by the supplied token readers.
  3. `BearerTokenReader extends HttpHeaderTokenReader implements TokenReader` Reads JWT token from "Authorization: Bearer a3425c34e5..." header.
  4. `JwtTokenValidator implements TokenValidator` validates the token

For each of these you can provide custom implementations.


`mongodb://localhost:27017/test2?readPreference=primary&appname=Liquido&ssl=false&uuidRepresentation=standard`

# Resources & Links

 - [Micronaut-Mongo-Gorm-Example @ GitHub](https://github.com/jeantessier/micronaut-mongo-gorm-example)
 - [Micronaut Micronaut HTTP Client documentation](https://docs.micronaut.io/latest/guide/index.html#httpClient)
 - [Micronaut Micronaut Security JWT documentation](https://micronaut-projects.github.io/micronaut-security/latest/guide/index.html)
