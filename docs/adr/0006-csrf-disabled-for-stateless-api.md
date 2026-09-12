# 0006. CSRF protection disabled for the stateless JWT API

Status: Accepted

## Context

Spring Security enables CSRF protection by default. CSRF attacks work by
having a victim's browser *automatically* attach ambient, browser-managed
credentials (a session cookie) to a forged cross-site request, so the server
ends up trusting a request it never meant to authorize. `core-service`
carries no session cookie and no server-side session at all
(`SessionCreationPolicy.STATELESS`): every request is authenticated by a JWT
that the client must explicitly read from its own storage and attach itself
via the `Authorization: Bearer <token>` header.

## Decision

Disable CSRF protection (`http.csrf(AbstractHttpConfigurer::disable)`) in
`SecurityConfig`.

## Consequences

- Safe specifically *because* there is no cookie/session for a browser to
  attach automatically — the CSRF threat model doesn't apply here. A browser
  has no ambient credential for this API to be tricked into sending.
- This decision is tied to the auth mechanism, not permanent: if a
  cookie-based or session-based auth flow is ever added alongside JWT (e.g.
  a first-party web client using httpOnly cookies), CSRF protection must be
  re-enabled for that flow.
- The client is responsible for JWT storage and never relies on the browser
  to send it automatically — an XSS vulnerability in a client is a bigger
  risk to token theft than CSRF ever was here, and is out of this API's
  control.
