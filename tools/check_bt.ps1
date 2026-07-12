Get-PnpDevice -Class Bluetooth | Where-Object { $_.FriendlyName -match 'MOONDROP|RFCOMM|Serial|SPP' } | Format-Table Status,FriendlyName,InstanceId -AutoSize
