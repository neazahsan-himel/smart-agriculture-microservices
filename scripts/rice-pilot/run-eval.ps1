<#
  Rice pilot evaluation runner (docs/plans/rice-pilot-test-questions.md).
  Asks each question in a fresh conversation through the gateway and writes the replies to a
  Markdown results file for scoring. Run it for the baseline now and again after the knowledge layer.

    powershell -ExecutionPolicy Bypass -File scripts\rice-pilot\run-eval.ps1 -Run baseline
    powershell -ExecutionPolicy Bypass -File scripts\rice-pilot\run-eval.ps1 -Run after -Only F1,P3

  Windows PowerShell 5.1 sends string bodies as ISO-8859-1 and mis-decodes UTF-8 responses
  without a charset, so bodies are sent and read as UTF-8 bytes explicitly (Bangla questions).
#>
param(
    [Parameter(Mandatory = $true)][string]$Run,
    [string]$BaseUrl = 'http://localhost:8080',
    [string[]]$Only = @(),
    [int]$TimeoutSec = 700
)

$ErrorActionPreference = 'Stop'
$root      = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$questions = Get-Content (Join-Path $PSScriptRoot 'questions.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$outDir    = Join-Path $root 'docs\plans\rice-pilot-results'
$outFile   = Join-Path $outDir "$Run.md"
New-Item -ItemType Directory -Force $outDir | Out-Null

function Invoke-Json([string]$Method, [string]$Path, $Body = $null) {
    $params = @{ Uri = "$BaseUrl$Path"; Method = $Method; TimeoutSec = $TimeoutSec; UseBasicParsing = $true }
    if ($null -ne $Body) {
        $params.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 5 -Compress))
        $params.ContentType = 'application/json; charset=utf-8'
    }
    $res = Invoke-WebRequest @params
    $text = [Text.Encoding]::UTF8.GetString($res.RawContentStream.ToArray())
    return $text | ConvertFrom-Json
}

function Get-LastAgentMessage($conversation) {
    $agent = @($conversation.messages | Where-Object { $_.sender -eq 'AGENT' })
    if ($agent.Count -eq 0) { return '' }
    return $agent[-1].content
}

function Get-TaskSignatures([string]$farmerId) {
    $page = Invoke-Json GET "/api/v1/advisor/farm-tasks?farmerId=$farmerId&size=100"
    $map = @{}
    foreach ($t in $page.content) { $map[$t.id] = "$($t.taskType)|$($t.dueDate)|$($t.status)|$($t.weatherAdvice)" }
    return $map
}

# Test farmer (pilot plan section 1): reuse if it already exists
$farmerName = 'Karim Saheb (pilot)'
$farmers = Invoke-Json GET '/api/v1/farmers?size=100'
$farmer = @($farmers.content | Where-Object { $_.name -eq $farmerName }) | Select-Object -First 1
if (-not $farmer) {
    $farmer = Invoke-Json POST '/api/v1/farmers' @{
        name = $farmerName; countryCode = 'BD'; region = 'Dhaka'; district = 'Gazipur'
        latitude = 24.00; longitude = 90.42; farmType = 'RICE'; farmSizeHectares = 1.5
    }
    Write-Host "Created test farmer $($farmer.id)"
} else {
    Write-Host "Using test farmer $($farmer.id)"
}

if ($Only.Count -gt 0) { $questions = @($questions | Where-Object { $Only -contains $_.id }) }

$sb = New-Object Text.StringBuilder
[void]$sb.AppendLine("# Rice Pilot - $Run results")
[void]$sb.AppendLine()
[void]$sb.AppendLine("> Run started $(Get-Date -Format 'yyyy-MM-dd HH:mm') | farmer ``$($farmer.id)`` ($farmerName) | gateway $BaseUrl")
[void]$sb.AppendLine("> Questions: $($questions.Count) | scoring criteria S1-S8 in ``docs/plans/rice-pilot-test-questions.md`` section 3")
[void]$sb.AppendLine()

foreach ($q in $questions) {
    Write-Host "[$($q.id)] asking..."
    $before = Get-TaskSignatures $farmer.id
    $watch = [Diagnostics.Stopwatch]::StartNew()
    $turns = @()
    $caseInfo = ''
    try {
        $conv = Invoke-Json POST '/api/v1/advisor/conversations' @{ farmerId = $farmer.id; queryType = $q.topic; message = $q.question }
        $reply = Get-LastAgentMessage $conv
        $turns += ,@('Farmer', $q.question)
        $turns += ,@('Advisor', $reply)
        # Protocol: if the advisor asks a clarifying question, answer once with the test context
        if ($reply -match '\?') {
            $conv = Invoke-Json POST "/api/v1/advisor/conversations/$($conv.id)/messages" @{ message = $q.context }
            $turns += ,@('Farmer (context)', $q.context)
            $turns += ,@('Advisor', (Get-LastAgentMessage $conv))
        }
        if ($conv.latestCase) {
            $caseInfo = "risk ``$($conv.latestCase.riskLevel)``, confidence ``$($conv.latestCase.confidenceLevel)``, status ``$($conv.latestCase.status)``"
        }
        $failure = $null
    } catch {
        $failure = $_.Exception.Message
    }
    $watch.Stop()

    $after = Get-TaskSignatures $farmer.id
    $changedTasks = @($after.Keys | Where-Object { $before[$_] -ne $after[$_] } | ForEach-Object { ($after[$_] -split '\|')[0] + ': ' + ($after[$_] -split '\|')[3] })

    [void]$sb.AppendLine("---")
    [void]$sb.AppendLine()
    [void]$sb.AppendLine("## $($q.id) ($($q.lang), topic ``$($q.topic)``)")
    [void]$sb.AppendLine()
    [void]$sb.AppendLine("- Time: $([math]::Round($watch.Elapsed.TotalSeconds)) s | turns: $([math]::Floor($turns.Count / 2))")
    if ($caseInfo) { [void]$sb.AppendLine("- Diagnostic case: $caseInfo") }
    [void]$sb.AppendLine("- Tasks created/changed: $(if ($changedTasks.Count) { $changedTasks -join '; ' } else { 'none' })")
    if ($failure) { [void]$sb.AppendLine("- **ERROR:** $failure") }
    [void]$sb.AppendLine()
    foreach ($t in $turns) {
        [void]$sb.AppendLine("**$($t[0]):**")
        [void]$sb.AppendLine()
        [void]$sb.AppendLine(($t[1] -split "`n" | ForEach-Object { "> $_" }) -join "`n")
        [void]$sb.AppendLine()
    }
    [void]$sb.AppendLine("| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |")
    [void]$sb.AppendLine("|---|---|---|---|---|---|---|---|")
    [void]$sb.AppendLine("|  |  |  |  |  |  |  |  |")
    [void]$sb.AppendLine()

    # Write after every question so a long run can be inspected (or survives a crash) midway
    [IO.File]::WriteAllText($outFile, $sb.ToString(), (New-Object Text.UTF8Encoding $false))
    Write-Host "[$($q.id)] done in $([math]::Round($watch.Elapsed.TotalSeconds)) s$(if ($failure) { " - ERROR: $failure" })"
}

Write-Host "Results: $outFile"
