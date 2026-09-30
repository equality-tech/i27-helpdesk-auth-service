# Auth Service — Docker Guide

This document explains the `Dockerfile` in this folder: what it does, why it's built the
way it is, and how to build/run/troubleshoot it. It complements the local (non-Docker)
setup in [`../revision-final.md`](../revision-final.md).

## What the Dockerfile does

It's a **two-stage** build:

1. **`build` stage** (`maven:3.9.9-eclipse-temurin-17`) — compiles the app and packages it
   with Maven, then explodes the resulting fat jar into Spring Boot's "layers"
   (`dependencies`, `spring-boot-loader`, `snapshot-dependencies`, `application`) using
   `java -Djarmode=layertools -jar app.jar extract`.
2. **`runtime` stage** (`eclipse-temurin:17-jre-jammy`) — a JRE-only image (no Maven, no
   JDK compiler, no source code) that copies in just those four layers and runs the app
   as a non-root user.

Only the final `runtime` stage ends up in the image you ship — the `build` stage and
everything in it (Maven cache, source tree, JDK) is discarded. This keeps the shipped
image small and avoids leaking source code or build tooling into production.

### Why layered copy instead of `COPY target/*.jar app.jar`

A plain "copy the fat jar" Dockerfile means **every** code change invalidates and
re-uploads the entire jar (app code + every third-party dependency) as one Docker layer.
With layertools, dependencies (which rarely change) sit in their own layer and stay
cached across builds/deploys; only the small `application` layer (your compiled classes)
changes when you ship a code change. This meaningfully speeds up CI image builds and
registry pushes.

### Why a non-root user

The image creates a dedicated `spring` user (uid/gid `1000`) with no login shell and runs
the JVM as that user, not `root`. If the app or a dependency is ever compromised, the
process has no elevated privileges inside the container. This is a baseline
container-security expectation (also required by most Kubernetes `PodSecurityStandards`
"restricted" policies via `runAsNonRoot`).

### Why `eclipse-temurin` instead of the old `openjdk` images

The official `openjdk` Docker images are deprecated/unmaintained. `eclipse-temurin` is the
actively maintained, security-patched OpenJDK distribution and is the current
recommended base for JVM images.

### Health check

The app already exposes an unauthenticated `GET /healthz` endpoint
(`HealthController`), permitted by `SecurityConfig` (`anyRequest().permitAll()`). The
Dockerfile's `HEALTHCHECK` polls that endpoint every 30s so `docker ps` / orchestrators
can see container health without needing credentials.

> Note: `SecurityConfig` currently permits **all** requests, not just `/healthz`. That's
> a pre-existing app-level decision, not something this Dockerfile changes — call it out
> to the team if it's not intentional for endpoints like `/admin/**`.

## Required environment variables

`application.yml` pulls all runtime config from environment variables — the image has
**no** database credentials or secrets baked in. You must supply these at `docker run` /
orchestrator level:

| Variable          | Example                                                     | Notes                          |
|--------------------|--------------------------------------------------------------|----------------------------------|
| `SERVER_PORT`      | `8081`                                                        | Must match `EXPOSE`/health check port |
| `DB_JDBC_URL`      | `jdbc:mysql://136.65.46.225:3306/helpdesk_dev`                | |
| `DB_USERNAME`      | `helpdesk_user`                                               | |
| `DB_PASSWORD`      | `********`                                                    | Pass as a secret, not plain `-e` in shared shells |
| `JWT_SECRET`       | `********` (32+ chars)                                        | Must match the value used by the Gateway and Ticket service |
| `JWT_EXPIRY_MILLIS`| `3600000`                                                     | |

Optional:

| Variable    | Default in image                                                     | Purpose |
|-------------|------------------------------------------------------------------------|---------|
| `JAVA_OPTS` | `-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom` | JVM tuning; override per environment |

## Building the image

```bash
docker build -t i27-helpdesk-auth-service:local .
```

BuildKit cache mounts are used for the Maven `~/.m2` repo, so repeated local builds are
fast. If your Docker doesn't have BuildKit on by default:

```bash
DOCKER_BUILDKIT=1 docker build -t i27-helpdesk-auth-service:local .
```

## Running the image

```bash
docker run --rm -p 8081:8081 \
  -e SERVER_PORT=8081 \
  -e DB_JDBC_URL="jdbc:mysql://136.65.46.225:3306/helpdesk_dev" \
  -e DB_USERNAME="helpdesk_user" \
  -e DB_PASSWORD="Gcp@2024" \
  -e JWT_SECRET="i27academy-secret-key-which-is-32chars" \
  -e JWT_EXPIRY_MILLIS=3600000 \
  i27-helpdesk-auth-service:local
```

Check it came up healthy:

```bash
docker ps            # STATUS column should show "healthy" after ~40s
curl http://localhost:8081/healthz
```

### docker-compose snippet

```yaml
services:
  auth-service:
    build: ./i27-helpdesk-auth-service
    image: i27-helpdesk-auth-service:local
    ports:
      - "8081:8081"
    environment:
      SERVER_PORT: 8081
      DB_JDBC_URL: jdbc:mysql://136.65.46.225:3306/helpdesk_dev
      DB_USERNAME: helpdesk_user
      DB_PASSWORD: ${DB_PASSWORD}          # from an untracked .env / secret store
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRY_MILLIS: 3600000
    restart: unless-stopped
```

Never commit real `DB_PASSWORD`/`JWT_SECRET` values into a compose file — inject them via
`.env` (gitignored), your orchestrator's secret store, or CI/CD secret variables.

## Pushing to a registry

```bash
docker tag i27-helpdesk-auth-service:local <registry>/<namespace>/i27-helpdesk-auth-service:<tag>
docker push <registry>/<namespace>/i27-helpdesk-auth-service:<tag>
```

Prefer tagging with an immutable identifier (git SHA or semantic version) over `latest`
for anything deployed, so rollbacks are unambiguous.

## Production hardening checklist

- [x] Multi-stage build — no JDK/Maven/source in the shipped image
- [x] Runs as non-root (`spring`, uid 1000)
- [x] No secrets baked into the image — all via env vars at runtime
- [x] `HEALTHCHECK` wired to a real endpoint
- [x] Layered jar copy for faster incremental builds/pulls
- [ ] Set container resource `limits`/`requests` (CPU/memory) in your orchestrator —
      `JAVA_OPTS`'s `MaxRAMPercentage` only helps if a memory limit is actually set
- [ ] Terminate TLS in front of this service (ingress/load balancer) — the app itself
      serves plain HTTP
- [ ] Scan the built image (`docker scout`, `trivy`, or your registry's built-in scanner)
      in CI before deploying
- [ ] Consider adding `spring-boot-starter-actuator` with `management.endpoint.health.probes`
      for richer liveness/readiness distinction (`/actuator/health/liveness` vs
      `/readiness`) if this ever runs on Kubernetes — the current `/healthz`/`/readyz`
      endpoints are a reasonable hand-rolled equivalent but `/readyz` doesn't check the DB
      yet (see the `// later we can add DB check here` TODO in `HealthController`)

## Troubleshooting

- **Container exits immediately** — almost always a missing required env var; check
  `docker logs <container>` for a Spring `Failed to bind properties`/placeholder error.
- **`HEALTHCHECK` never turns healthy** — confirm `SERVER_PORT` matches what you mapped
  with `-p`, and that the DB is reachable (`/healthz` doesn't check the DB, but the app
  won't finish starting up if it can't connect, per `spring.jpa.hibernate.ddl-auto: update`
  needing a live connection at boot).
- **Build is slow every time** — confirm BuildKit is enabled so the `--mount=type=cache`
  Maven cache actually persists between builds.
