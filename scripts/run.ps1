param(
    [ValidateSet('Local', 'Supabase')]
    [string]$BaseDatos = 'Local'
)

$ErrorActionPreference = 'Stop'
$moduleDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../abastecimiento'))
$environmentFilename = if ($BaseDatos -eq 'Supabase') { '.env.supabase' } else { '.env' }
$environmentFile = Join-Path $moduleDirectory $environmentFilename
if (-not (Test-Path -LiteralPath $environmentFile -PathType Leaf)) {
    throw "Crea abastecimiento/$environmentFilename con la configuración de la base seleccionada."
}
Push-Location -LiteralPath $moduleDirectory
try {
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
    $javaExecutable = (Get-Command java -ErrorAction Stop).Source
    $javaProbe = New-Object System.Diagnostics.Process
    $javaProbe.StartInfo.FileName = $javaExecutable
    $javaProbe.StartInfo.Arguments = '-XshowSettings:properties -version'
    $javaProbe.StartInfo.UseShellExecute = $false
    $javaProbe.StartInfo.CreateNoWindow = $true
    $javaProbe.StartInfo.RedirectStandardError = $true
    try {
        $null = $javaProbe.Start()
        $javaSettings = $javaProbe.StandardError.ReadToEnd()
        $javaProbe.WaitForExit()
        if ($javaProbe.ExitCode -ne 0) { throw 'No se pudo comprobar la instalación de Java.' }
    } finally { $javaProbe.Dispose() }
    $javaSettings = $javaSettings -split '\r?\n'
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
        & $javaExecutable "-Djdk.net.unixdomain.tmpdir=$socketDirectory" "-Dapp.env.file=$environmentFile" -cp 'target/abastecimiento-1.0-SNAPSHOT.jar;target/dependency/*' com.alexis.App
        if ($LASTEXITCODE -ne 0) { throw "La aplicacion no pudo arrancar con $environmentFilename. Revisa el diagnostico anterior." }
    } finally { $env:JAVA_HOME = $previousJavaHome }
} finally { Pop-Location }
