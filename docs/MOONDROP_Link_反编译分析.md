# MOONDROP Link APP 反编译分析

## APK 信息

| 项目 | 值 |
|------|-----|
| 包名 | `com.moondroplab.moondrop.moondrop_app` |
| 版本 | `2.23.1c-260622ai` |
| 版本号 | 102029 |
| MinSDK | 28 (Android 9) |
| TargetSDK | 37 |
| CompileSDK | 37 |

## 核心发现

### 1. 使用高通官方 GAIA V3 SDK

APP 内置了 `com.qualcomm.qti.gaiaclient` 包，这是高通官方的 GAIA V3 客户端 SDK。

### 2. ANC 控制走 BLE GATT

APP 同时使用了：
- **SPP RFCOMM** — 基础查询（固件版本、配置等）
- **BLE GATT** — ANC 控制、高级功能

关键 BLE 类：
- `BluetoothGatt`
- `BluetoothGattCallback`
- `BluetoothGattCharacteristic`
- `BluetoothGattService`

### 3. ANC 协议: AncV2

ANC 使用 GAIA V3 的 AncV2 Feature，关键方法：

| 方法 | 说明 |
|------|------|
| `handleGetANCMode` | 获取 ANC 模式 |
| `handleSetANCMode` | 设置 ANC 模式 |
| `handleGetANCAction` | 获取 ANC 动作 |
| `handleSetANCAction` | 设置 ANC 动作 |
| `onAncV2Mode` | ANC V2 模式回调 |
| `onAncV2SwitchConf` | ANC V2 切换配置回调 |
| `handleSetAncV2SwitchConf` | 设置 ANC V2 切换配置 |
| `onANCActionReceived` | 接收 ANC 动作通知 |

### 4. 支持的功能

从反编译字符串提取：

| 功能 | 支持 |
|------|------|
| ANC (降噪) | ✓ `supportAnc` |
| 自适应 ANC | ✓ `supportAdaptiveANC` |
| 空间音频 | ✓ `V3SpatialAudioPlugin` |
| 电源控制 | ✓ `V3PowerControlPlugin` |
| 电源超时 | ✓ `V3PowerTimeoutPlugin` |
| 统计 | ✓ `V3StatisticsPlugin` |
| LHDC | ✓ `publishLhdcState` |
| 左右平衡 | ✓ `publishLeftRightBalance` |

### 5. GAIA V3 插件列表

| 插件 | 说明 |
|------|------|
| `V3SpatialAudioPlugin` | 空间音频 |
| `V3PowerControlPlugin` | 电源控制 |
| `V3PowerTimeoutPlugin` | 电源超时 |
| `V3StatisticsPlugin` | 统计信息 |
| `ANCPublisher` | ANC 发布器 |
| `LedPublisher` | LED 控制 |
| `VoicePublisher` | 语音 |
| `VoiceUIPublisher` | 语音 UI |
| `BatteryPublisher` | 电量 |
| `UpgradePublisher` | 固件升级 |

### 6. 关键日志字符串

```
(RESTORE COEF)!!!!.0ANC USER TRIGGER FLOW FINISHED
(UPDATE COEF)!!!!.3ANC USER TRIGGER FLOW STOPPED BY
(state = mTimerSendCancelCmd delay 2000ms
function = getAncSetting-begin
function = getAncSetting-end
supportAdaptiveANC=.., supportAnc=.., supportDev
onANCActionReceived: actionsMap =
```

### 7. 蓝牙权限

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-feature android:name="android.hardware.bluetooth" />
<uses-feature android:name="android.hardware.bluetooth_le" />
```

### 8. 依赖库

- Flutter (应用框架)
- Koin (依赖注入)
- Qualcomm GAIA Client SDK
- Google Play Services
- AndroidX Camera

## 结论

MOONDROP Link 使用高通 GAIA V3 SDK，ANC 控制通过 **BLE GATT** 通道实现，而非 SPP RFCOMM。SPP 仅用于基础查询。

要实现第三方 ANC 控制，需要：
1. 通过 BLE GATT 连接设备
2. 找到 GAIA GATT Service
3. 通过 GATT Characteristic 发送 GAIA V3 AncV2 命令
