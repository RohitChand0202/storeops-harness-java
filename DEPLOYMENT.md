# Local deployment and verification

StoreOps uses in-memory repositories. The app needs Java 17; the Docker image builds the same Maven gate profile as CI and runs as a non-root user. A restart clears all activities, programmes, users, and alerts.

## Configure a demo session

There are no default credentials. Supply a token and account details as environment variables before launching the app. The optional incoming lead account makes the handover alert visible through a second authenticated session.

```powershell
$env:STOREOPS_BOOTSTRAP_TOKEN = [guid]::NewGuid().ToString('N')
$env:STOREOPS_BOOTSTRAP_USER_ID = 'outgoing-lead'
$env:STOREOPS_BOOTSTRAP_EMAIL = 'outgoing@example.test'
$env:STOREOPS_BOOTSTRAP_NAME = 'Outgoing Lead'
$env:STOREOPS_BOOTSTRAP_ROLE = 'DEPARTMENT_LEAD'
$env:STOREOPS_BOOTSTRAP_STORE_ID = 'store-1'
$env:STOREOPS_BOOTSTRAP_REGION_ID = 'region-1'
$env:STOREOPS_BOOTSTRAP_INCOMING_LEAD_TOKEN = [guid]::NewGuid().ToString('N')
$env:STOREOPS_BOOTSTRAP_INCOMING_LEAD_USER_ID = 'incoming-lead'
$env:STOREOPS_BOOTSTRAP_INCOMING_LEAD_EMAIL = 'incoming@example.test'
$env:STOREOPS_BOOTSTRAP_INCOMING_LEAD_NAME = 'Incoming Lead'
```

Both demo tokens expire after 28,800 seconds by default; set `STOREOPS_BOOTSTRAP_TOKEN_TTL_SECONDS` to a positive number to change that. The incoming lead token, user ID, email, and name must be supplied together. The incoming account is a `DEPARTMENT_LEAD` in the outgoing user's store and region. Keep the tokens out of source control and shell transcripts that will be shared.

## Start

With Docker Compose installed:

```powershell
docker compose up --build -d
docker compose ps
```

The build stage runs `mvn -B clean verify -Pharness-gates` before producing the runtime image. The health check requests `/actuator/health`. The Compose file requires all demo variables, including the second lead, so the alert can be verified. Run `docker compose down` when finished.

The same app can be run without Docker after installing JDK 17:

```powershell
.\mvnw.cmd clean verify -Pharness-gates
java -jar target/storeops-0.0.1-SNAPSHOT.jar
```

The JDK and Maven wrapper are build requirements. The packaged jar needs only a Java 17 runtime. If running without Docker, set the environment variables in the same shell before starting the jar.

## Exercise the governed feature

After `docker compose up -d`, run these calls in the same PowerShell shell so the token variables remain available. If you started the jar in the foreground, repeat the environment assignments in a second shell before making the calls. First create an activity, because the repositories start empty:

```powershell
$base = 'http://localhost:8080'
$outgoing = @{ Authorization = "Bearer $env:STOREOPS_BOOTSTRAP_TOKEN" }
$created = Invoke-RestMethod -Method Post -Uri "$base/api/activities" -Headers $outgoing -ContentType 'application/json' -Body '{"title":"Restock aisle 4","storeId":"store-1","priority":"HIGH","category":"RESTOCKING"}'
$created.id
```

Then complete that activity and include a nonexistent ID to exercise independent item results:

```powershell
$body = @{ updates = @(
  @{ taskId = $created.id; targetStatus = 'DONE'; note = 'Restock complete' },
  @{ taskId = 'missing-activity'; targetStatus = 'DONE' }
) } | ConvertTo-Json -Depth 5
$response = Invoke-WebRequest -Method Patch -Uri "$base/api/activities/bulk-status" -Headers $outgoing -ContentType 'application/json' -Body $body
$response.StatusCode
$response.Content
```

The response status should be `207`. Its body should report one success, one `NOT_FOUND` failure, and counts of two requested, one succeeded, and one failed. A request with only valid transitions returns `200`. An empty `updates` list returns `400` with a typed validation error.

The handover event should create one alert for the incoming lead:

```powershell
$incoming = @{ Authorization = "Bearer $env:STOREOPS_BOOTSTRAP_INCOMING_LEAD_TOKEN" }
Invoke-RestMethod -Method Get -Uri "$base/api/alerts" -Headers $incoming | ConvertTo-Json -Depth 5
```

`scripts/smoke.ps1` runs the health, create, mixed-result handover, and alert checks as one repeatable command:

```powershell
.\scripts\smoke.ps1
```

An unauthenticated `/api/activities` call returns `401`. `/actuator/health` is public. The API enforces the caller's store for activities and programmes; `/api/alerts` returns the caller's own alerts.

## Verification record

The Java build and HTTP smoke results for this checkout are recorded in `docs/local-smoke.md`. The Dockerfile and Compose file are provided for a Docker-capable host. The current Windows workspace has no Docker executable, so no container start, image size, or container screenshot is claimed here.
