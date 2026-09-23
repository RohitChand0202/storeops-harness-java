# Deployment

**Target:** local Docker, via `docker compose`.

**Why.** StoreOps uses in-memory repositories by design, so there is no database, no migration step
and no connection string — the only runtime configuration it has is a demo auth token. A container
is therefore the whole deployment surface, and a compose file is an honest description of it rather
than a thin wrapper over something more complicated. The multi-stage build below also lets the image
be built with `-Pharness-gates`, which is the property worth having: the deployment artefact and the
harness verdict cannot disagree, because the image will not build from code that fails a hard gate.

A cloud target is the stronger choice where secrets management and service configuration are the
point; the last section of this file has the shortest path to one from the same Dockerfile.

> STATUS: the Evidence section below is pending the demonstration run. Everything above it is
> reproducible now.

---

## Steps taken

### 1. Dockerfile

Multi-stage, so the runtime image carries no Maven and no build cache. Place at the repository root.

```dockerfile
# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Dependencies first so a source change does not re-download the world
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
COPY .harness/checks ./.harness/checks

# Build with the same gate profile the harness and CI use. If the harness passed, this passes.
RUN mvn -B -q clean package -Pharness-gates

# ---- runtime ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root. The container has no business running as root to serve an in-memory API.
RUN addgroup -S storeops && adduser -S storeops -G storeops
USER storeops

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

Building with `-Pharness-gates` is deliberate. It means the image cannot be built from code that
fails a hard gate, so the deployment artefact and the harness verdict cannot disagree. If you would
rather keep image builds fast, drop the profile and rely on CI — but then say so here, because it is a
governance decision, not a build detail.

### 2. Compose

```yaml
services:
  storeops-api:
    build: .
    image: storeops-api:harness-demo
    container_name: storeops-api
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      STOREOPS_AUTH_DEMO_TOKEN: ${STOREOPS_AUTH_DEMO_TOKEN}
    restart: unless-stopped
```

The demo token comes from the environment, never from the compose file. `.env` is gitignored. There
is no database service because StoreOps uses in-memory repositories by design — do not add Postgres
to make the deployment look more substantial.

### 3. Run

```bash
echo "STOREOPS_AUTH_DEMO_TOKEN=$(openssl rand -hex 16)" > .env
docker compose up --build -d
docker compose ps
docker compose logs -f storeops-api    # wait for "Started StoreOpsApplication"
```

### 4. Verify the baseline

```bash
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/activities | jq
```

### 5. Verify the harness-generated endpoint

This is the call that matters: the endpoint the harness produced, not one of the nine base
endpoints. A base endpoint responding proves the container starts; this proves the governed feature
reached a running application.

```bash
curl -i -X PATCH http://localhost:8080/api/activities/bulk-status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "updates": [
      { "taskId": "a-1041", "targetStatus": "DONE",    "note": "restock complete" },
      { "taskId": "a-1042", "targetStatus": "BLOCKED", "note": "pallet missing" },
      { "taskId": "a-9999", "targetStatus": "DONE" }
    ]
  }'
```

Expected — `207 Multi-Status`, because one id does not exist:

```json
{
  "requestedCount": 3,
  "succeededCount": 2,
  "failedCount": 1,
  "succeeded": [
    { "taskId": "a-1041", "previousStatus": "IN_PROGRESS", "newStatus": "DONE" },
    { "taskId": "a-1042", "previousStatus": "TODO", "newStatus": "BLOCKED" }
  ],
  "failed": [
    { "taskId": "a-9999", "reason": "NOT_FOUND" }
  ]
}
```

Send the partial-failure case, not the all-success case. A `200` with everything succeeding
demonstrates that the endpoint exists. A `207` demonstrates that the partial-failure semantics the
sprint contract specified actually work in a running container, which is the thing the harness was
built to govern.

Also worth capturing, since it is one curl and it evidences ERR-1 end to end:

```bash
curl -i -X PATCH http://localhost:8080/api/activities/bulk-status \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"updates":[]}'
# → 400, {"code":"VALIDATION_FAILED", ...} — a typed code, not a stack trace
```

---

## Evidence

> STATUS: TO COMPLETE after the demonstration run. Three things go here — the `docker compose ps`
> output, two committed screenshots, and the image size. A screenshot referenced but not committed
> reads as a missing artefact, so commit the images alongside this file.

**Container running:**

```
[paste `docker compose ps` output here]
```

**Screenshot:** `docs/deployment-container.png` — the running container
**Screenshot:** `docs/deployment-bulk-status-207.png` — the `PATCH /api/activities/bulk-status` response

**Image size:** `docker images storeops-api:harness-demo` → [fill in]

---

## Deploying to a cloud target instead

Cloud forces real decisions about configuration and secrets that a compose file does not, which is
the reason to prefer it. The shortest path from the Dockerfile above, with no code changes:

**GCP Cloud Run**
```bash
gcloud artifacts repositories create storeops --repository-format=docker --location=asia-south1
gcloud builds submit --tag asia-south1-docker.pkg.dev/$PROJECT/storeops/api:harness-demo
gcloud run deploy storeops-api \
  --image asia-south1-docker.pkg.dev/$PROJECT/storeops/api:harness-demo \
  --region asia-south1 --port 8080 --allow-unauthenticated \
  --set-secrets STOREOPS_AUTH_DEMO_TOKEN=storeops-demo-token:latest
```

**Azure Container Apps**
```bash
az containerapp up --name storeops-api --resource-group storeops-rg \
  --source . --ingress external --target-port 8080
az containerapp secret set --name storeops-api --resource-group storeops-rg \
  --secrets demo-token=$TOKEN
```

**AWS** — ECR push plus an ECS Fargate service, or Elastic Beanstalk with a `Dockerrun` config.
More steps than either of the above for the same result.

On a cloud target, three things belong in the Evidence section above alongside the URL, because they
are the decisions the deployment actually forced:

1. **Where the token lives** — a Secret Manager or Key Vault reference, not an environment variable
   in the service definition.
2. **The minimum instance count, and what it costs.** Cloud Run at `min-instances=0` means a cold
   start on the first request, which anyone hitting the live URL will see.
3. **Whether the endpoint is public, and what that accepts.** StoreOps has demo auth and in-memory
   data, so public is defensible for a reference codebase — but state it rather than leave it
   inferred.

The live URL and the same two curl calls go in the Evidence section above.
