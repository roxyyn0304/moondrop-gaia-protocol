# 尝试重置 RFCOMM 设备
$rfcomm = Get-PnpDevice | Where-Object { $_.InstanceId -match 'MS_RFCOMM' }
if ($rfcomm) {
    Write-Host "找到 RFCOMM: $($rfcomm.Status) - $($rfcomm.FriendlyName)"
    Write-Host "尝试禁用..."
    Disable-PnpDevice -InstanceId $rfcomm.InstanceId -Confirm:$false -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
    Write-Host "尝试启用..."
    Enable-PnpDevice -InstanceId $rfcomm.InstanceId -Confirm:$false -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3
    $rfcomm2 = Get-PnpDevice | Where-Object { $_.InstanceId -match 'MS_RFCOMM' }
    Write-Host "结果: $($rfcomm2.Status)"
} else {
    Write-Host "未找到 RFCOMM 设备"
}
