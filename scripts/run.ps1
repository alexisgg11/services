$ErrorActionPreference = 'Stop'
$moduleDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../abastecimiento'))
Set-Location -LiteralPath $moduleDirectory
$mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
if ($mavenCommand) { $mavenExecutable = $mavenCommand.Source }
else {
    $mavenHomeDirectory = Join-Path $env:USERPROFILE '.maven'
    $localMaven = Get-ChildItem -LiteralPath $mavenHomeDirectory -Directory -Filter 'maven-*' -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | ForEach-Object { Join-Path $_.FullName 'bin/mvn.cmd' } |
        Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if (-not $localMaven) { throw 'Instala Maven 3.9 y agrega mvn al PATH.' }
    $mavenExecutable = $localMaven
}
$javaSettings = & java -XshowSettings:properties -version 2>&1
$javaHomeLine = $javaSettings | Select-String 'java.home ='
if (-not $javaHomeLine) { throw 'No se encontró una instalación de Java.' }
if (-not ($javaSettings | Select-String 'java.specification.version = 25$')) { throw 'Este proyecto requiere Java 25 en PATH.' }
$runtimeJavaHome = ($javaHomeLine.ToString() -split '=', 2)[1].Trim()
$previousJavaHome = $env:JAVA_HOME
$env:JAVA_HOME = $runtimeJavaHome
try {
    & $mavenExecutable clean package dependency:copy-dependencies '-DincludeScope=runtime'
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación o las pruebas.' }
    $socketDirectory = Join-Path $moduleDirectory 'target'
    & java "-Djdk.net.unixdomain.tmpdir=$socketDirectory" -cp 'target/abastecimiento-1.0-SNAPSHOT.jar;target/dependency/*' com.alexis.App
    if ($LASTEXITCODE -ne 0) { throw 'La aplicación no pudo arrancar. Revisa .env y PostgreSQL.' }
} finally { $env:JAVA_HOME = $previousJavaHome }
