param(
    [string]$BaseUrl = 'http://localhost:8080'
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$required = @(
    'STOREOPS_BOOTSTRAP_TOKEN',
    'STOREOPS_BOOTSTRAP_STORE_ID',
    'STOREOPS_BOOTSTRAP_INCOMING_LEAD_TOKEN',
    'STOREOPS_BOOTSTRAP_INCOMING_LEAD_USER_ID'
)
foreach ($name in $required) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Missing required environment variable: $name"
    }
}

$health = Invoke-RestMethod -Method Get -Uri "$BaseUrl/actuator/health"
if ($health.status -ne 'UP') { throw "Health status was $($health.status)" }

$outgoing = @{ Authorization = "Bearer $env:STOREOPS_BOOTSTRAP_TOKEN" }
$incoming = @{ Authorization = "Bearer $env:STOREOPS_BOOTSTRAP_INCOMING_LEAD_TOKEN" }
$createBody = @{
    title = 'Restock aisle 4'
    storeId = $env:STOREOPS_BOOTSTRAP_STORE_ID
    priority = 'HIGH'
    category = 'RESTOCKING'
} | ConvertTo-Json -Compress
$createdResponse = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$BaseUrl/api/activities" `
    -Headers $outgoing -ContentType 'application/json' -Body $createBody
if ([int]$createdResponse.StatusCode -ne 201) { throw 'Activity creation did not return 201' }
$created = $createdResponse.Content | ConvertFrom-Json

$missingId = 'missing-' + [guid]::NewGuid().ToString('N')
$bulkBody = @{ updates = @(
    @{ taskId = $created.id; targetStatus = 'DONE'; note = 'Restock complete' },
    @{ taskId = $missingId; targetStatus = 'DONE' }
) } | ConvertTo-Json -Depth 5 -Compress
$bulkResponse = Invoke-WebRequest -UseBasicParsing -Method Patch `
    -Uri "$BaseUrl/api/activities/bulk-status" -Headers $outgoing `
    -ContentType 'application/json' -Body $bulkBody
$bulk = $bulkResponse.Content | ConvertFrom-Json
if ([int]$bulkResponse.StatusCode -ne 207 -or $bulk.requestedCount -ne 2 `
        -or $bulk.succeededCount -ne 1 -or $bulk.failedCount -ne 1 `
        -or $bulk.succeeded[0].taskId -ne $created.id `
        -or $bulk.failed[0].taskId -ne $missingId `
        -or $bulk.failed[0].reason -ne 'NOT_FOUND') {
    throw "Unexpected bulk result: $($bulkResponse.Content)"
}

$alerts = @(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/alerts" -Headers $incoming)
$handoverAlerts = @($alerts | Where-Object {
    $_.alertType -eq 'SHIFT_HANDOVER' -and $_.channel -eq 'IN_APP' `
        -and $_.userId -eq $env:STOREOPS_BOOTSTRAP_INCOMING_LEAD_USER_ID `
        -and $_.payload -like "*$env:STOREOPS_BOOTSTRAP_STORE_ID*"
})
if ($handoverAlerts.Count -ne 1) {
    throw "Expected one incoming-lead handover alert, found $($handoverAlerts.Count)"
}

[pscustomobject]@{
    health = $health.status
    createdStatus = [int]$createdResponse.StatusCode
    bulkStatus = [int]$bulkResponse.StatusCode
    requested = $bulk.requestedCount
    succeeded = $bulk.succeededCount
    failed = $bulk.failedCount
    failureReason = $bulk.failed[0].reason
    handoverAlerts = $handoverAlerts.Count
} | Format-List
