# PNG를 ICO로 변환하는 스크립트
param(
    [string]$inputFile = "composeApp\src\jvmMain\resources\logo.png",
    [string]$outputFile = "composeApp\src\jvmMain\resources\logo.ico"
)

Write-Host "PNG를 ICO로 변환 중..." -ForegroundColor Cyan
Write-Host "입력: $inputFile" -ForegroundColor Gray
Write-Host "출력: $outputFile" -ForegroundColor Gray

try {
    # .NET의 System.Drawing을 사용하여 변환
    Add-Type -AssemblyName System.Drawing

    $png = [System.Drawing.Image]::FromFile((Resolve-Path $inputFile))

    # 아이콘 크기들 (Windows 권장)
    $sizes = @(16, 32, 48, 64, 128, 256)

    # ICO 파일 생성을 위한 임시 비트맵들
    $icon = New-Object System.Drawing.Icon $png.GetHicon()

    # 간단한 변환 (단일 크기)
    $bitmap = New-Object System.Drawing.Bitmap $png, 256, 256
    $iconHandle = $bitmap.GetHicon()
    $icon = [System.Drawing.Icon]::FromHandle($iconHandle)

    # ICO 파일로 저장
    $stream = [System.IO.File]::Create((Resolve-Path $outputFile -ErrorAction SilentlyContinue).Path)
    if ($stream -eq $null) {
        $stream = [System.IO.File]::Create((Join-Path (Get-Location) $outputFile))
    }
    $icon.Save($stream)
    $stream.Close()

    Write-Host "✓ 변환 완료!" -ForegroundColor Green
    Write-Host "생성된 파일: $outputFile" -ForegroundColor Green
}
catch {
    Write-Host "❌ 변환 실패: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "대안: 온라인 변환 도구 사용" -ForegroundColor Yellow
    Write-Host "1. https://convertio.co/kr/png-ico/" -ForegroundColor Cyan
    Write-Host "2. logo.png 업로드" -ForegroundColor Cyan
    Write-Host "3. 다운로드한 logo.ico를 composeApp\src\jvmMain\resources\ 에 저장" -ForegroundColor Cyan
}
