param(
    [string]$HostName = 'localhost',
    [int]$Port = 5432,
    [string]$Database = 'jetvam',
    [string]$Username = 'jetvam',
    [string]$Password = 'jetvam',
    [string]$Schema = 'jetvam',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

if (-not $Force) {
    throw 'This command deletes every table and row in the selected schema. Run again with -Force after checking the parameters.'
}
if ($Schema -notmatch '^[a-zA-Z_][a-zA-Z0-9_]*$') {
    throw 'Schema must be a simple PostgreSQL identifier.'
}
$psqlCommand = Get-Command psql -ErrorAction SilentlyContinue
if ($psqlCommand) {
    $psqlPath = $psqlCommand.Source
}
else {
    $postgresRoot = Join-Path $env:ProgramFiles 'PostgreSQL'
    $psqlPath = Get-ChildItem -LiteralPath $postgresRoot -Filter psql.exe -Recurse -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $psqlPath) {
    throw 'psql was not found in PATH or under Program Files\PostgreSQL. Install PostgreSQL client tools.'
}

$previousPassword = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $Password
    $statement = "DROP SCHEMA IF EXISTS `"$Schema`" CASCADE; CREATE SCHEMA `"$Schema`" AUTHORIZATION `"$Username`";"
    & $psqlPath --host $HostName --port $Port --username $Username --dbname $Database --set ON_ERROR_STOP=1 --command $statement
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host "Schema '$Schema' was recreated. Start UAA, Services and Jobs so Flyway builds the clean baseline." -ForegroundColor Green
}
finally {
    $env:PGPASSWORD = $previousPassword
}
