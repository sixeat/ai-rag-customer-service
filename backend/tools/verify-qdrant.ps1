param(
    [string]$BaseUrl = $env:VECTOR_STORE_BASE_URL,
    [string]$Collection = $env:VECTOR_STORE_COLLECTION
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($BaseUrl) -or [string]::IsNullOrWhiteSpace($Collection)) {
    throw 'Set VECTOR_STORE_BASE_URL and VECTOR_STORE_COLLECTION, or pass -BaseUrl and -Collection.'
}

$uri = '{0}/collections/{1}' -f $BaseUrl.TrimEnd('/'), [uri]::EscapeDataString($Collection)
$headers = @{}
if (-not [string]::IsNullOrWhiteSpace($env:VECTOR_STORE_API_KEY)) {
    $headers['api-key'] = $env:VECTOR_STORE_API_KEY
}
$result = Invoke-RestMethod -Uri $uri -Headers $headers -TimeoutSec 10
if ($result.status -ne 'ok' -or -not $result.result) {
    throw "Qdrant collection check failed: $uri"
}

Write-Output "Qdrant collection is available: $Collection"
Write-Output "Points: $($result.result.points_count)"
