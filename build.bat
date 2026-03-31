@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

:: 切换到脚本所在目录（仓库根）
pushd "%~dp0" > nul 2>&1

:: ---------------------------------------------------------------------------
:: 分支过滤（.NET 正则，见 https://learn.microsoft.com/dotnet/standard/base-types/regular-expression-language-quick-reference）
:: - WHITELIST_REGEX 非空：仅构建本地短分支名匹配该正则的分支（黑名单忽略）
:: - WHITELIST_REGEX 为空：构建所有远程分支，但排除匹配 BLACKLIST_REGEX 的
:: - 多项排除/包含可用正则 alternation，例如 ^(main|maintenance/) 或 ^maintenance/
:: ---------------------------------------------------------------------------
set "BLACKLIST_REGEX=^(dev|maintenance)/"
set "WHITELIST_REGEX="

set JDK_PROPERTIES_FILE=jdks.properties
set GRADLE_USER_HOME=F:\Data\Gradle

set "BRANCH_LIST_FILE=%TEMP%\nf_build_branches_%RANDOM%.txt"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "try { ^
    $bl = $env:BLACKLIST_REGEX; ^
    $wl = $env:WHITELIST_REGEX; ^
    $out = $env:BRANCH_LIST_FILE; ^
    if ($bl) { [void][regex]::new($bl) }; ^
    if ($wl) { [void][regex]::new($wl) }; ^
    $names = @(git branch -r 2>$null | Where-Object { $PSItem -notmatch '->' } | ForEach-Object { ($PSItem.Trim() -replace '^origin/', '') } | Sort-Object -Unique); ^
    $names | Where-Object { ^
      if ($wl) { $PSItem -match $wl } ^
      elseif ($bl) { $PSItem -notmatch $bl } ^
      else { $true } ^
    } | Set-Content -LiteralPath $out -Encoding utf8 ^
  } catch { ^
    Write-Host ('Regex or git error: ' + $_.Exception.Message); ^
    exit 1 ^
  }"
if errorlevel 1 (
  echo Error: Failed to resolve branch list. Check BLACKLIST_REGEX / WHITELIST_REGEX.
  popd
  exit /b 1
)

for /f "usebackq delims=" %%b in ("%BRANCH_LIST_FILE%") do (
  call :build_branch %%b
)

del /q "%BRANCH_LIST_FILE%" > nul 2>&1

git checkout main > nul 2>&1

popd > nul 2>&1
goto :eof

:: 构建指定分支
:build_branch
set "branch_name=%1"
echo ===================================================================
echo Building branch: !branch_name!


:: 切换到目标分支
git checkout !branch_name! > nul 2>&1
if errorlevel 1 (
    echo Error: Failed to switch to branch !branch_name!
    exit /b 1
)

:: 拉取最新的远程代码
git pull origin !branch_name! > nul 2>&1
if errorlevel 1 (
    echo Error: Failed to pull remote code for branch !branch_name!
    exit /b 1
)

:: 读取 build.gradle 中的 Java 版本
set "java_version="
for /f "tokens=3 delims== " %%v in ('findstr /r /c:"def javaVer *=" build.gradle') do (
    set "java_version=%%v"
)

:: 清理版本号
set "java_version=!java_version:.=.!"
set "java_version=!java_version:"=!"
set "java_version=!java_version: =!"

echo Detected Java version: !java_version!

:: 从 jdks.properties 获取jdk路径
set "jdk_path="
for /f "tokens=1,2 delims==" %%j in (%JDK_PROPERTIES_FILE%) do (
    if /i "%%j"=="jdk!java_version!" set "jdk_path=%%k"
)

if not defined jdk_path (
    echo Error: Could not find JDK path for Java version !java_version!
    goto :eof
)

:: 设置 JAVA_HOME 并更新 PATH
set "JAVA_HOME=!jdk_path!"
set "PATH=!JAVA_HOME!\bin;!PATH!"

:: 执行构建
call gradlew.bat clean build

if errorlevel 1 (
    echo Error: Build failed for branch !branch_name!
    exit /b 1
)

echo Build completed for branch !branch_name!


:: 清理状态
goto :eof
