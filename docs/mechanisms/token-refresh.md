# Token refresh

How the session keeps a valid access token without signing the user out when requests race.

The HTTP session lives in Redis through Liberty's session cache and Redisson, under `jakarta-ee-api:session`, and ends
after ten hours without a request. Access tokens live five minutes, and the session's token is refreshed when less than
30 seconds of it is left.

> [!IMPORTANT]
>
> Keycloak rotates refresh tokens: every refresh returns a new one and invalidates the old one, and replaying a spent
> one ends the session. Two requests of one session refreshing at once would therefore sign the user out.

A refresh therefore runs once per refresh token:

- within one instance, `KeycloakTokenClient` lets the first request refresh and hands its result to the others;
- across instances, `SharedRefreshes` takes a lock in Redis; the instance holding it refreshes and leaves the new tokens
  in Redis for 30 seconds, where the others pick them up.

When Keycloak refuses a refresh the session is ended, and the request goes on without a caller: a blocked account or a
revoked session stops working within five minutes.
