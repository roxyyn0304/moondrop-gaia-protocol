# MOONDROP GAIA V3 Protocol Library

水月雨 MOONDROP 耳机蓝牙协议库，基于 Qualcomm GAIA V3 协议实现。

## 协议概述

MOONDROP 全系 TWS 耳机使用统一的 **GAIA V3 (Generic Audio Interface Architecture)** 协议进行蓝牙通信。

| 参数 | 值 |
|------|-----|
| 协议 | GAIA V3 (Qualcomm) |
| 传输 | RFCOMM Channel 16 |
| MTU | 990 bytes |
| UUID | `00001101-0000-1000-8000-00805F9B34FB` (SPP) |
| Vendor ID | 29 (0x001D) |

## 支持的功能

| Feature | ID | 功能 |
|---------|-----|------|
| 设备管理 | 0x00 | 电池查询、配置查询 |
| 基础功能 | 0x01 | 固件版本、状态查询 |
| ANC V2 | 0x03 | 降噪模式控制 |
| EQ/音乐 | 0x07 | 均衡器预设 |
| 编解码器 | 0x0A | LDAC/LHDC/LC3 控制 |
| Gain | 0x1E | 增益控制 |

## 快速开始

### 依赖

```kotlin
dependencies {
    implementation("com.moondrop.protocol:moondrop-gaia-protocol:0.1.0")
}
```

### 基本用法

```kotlin
import com.moondrop.protocol.GaiaPacketBuilder
import com.moondrop.protocol.codec.GaiaCodec
import com.moondrop.protocol.model.AncMode

// 构造 ANC 设置包
val packet = GaiaPacketBuilder.ancSet(AncMode.ANC)
val bytes = GaiaCodec.encode(packet)

// 发送到耳机...
// outputStream.write(bytes)

// 解析耳机响应
val response = GaiaCodec.decode(receivedBytes)
if (response != null) {
    val ancMode = ResponseParser.parseAncMode(response)
    println("Current ANC mode: ${ancMode.label}")
}
```

## 包格式

### TX (手机 → 耳机)

```
FF 04 [lenHi] [lenLo] 00 [seq] [vendorLo] [vendorHi] [featureId] [cmdId] [payload...]
```

### RX (耳机 → 手机)

```
[00] [vendorLo] [vendorHi] [featureId] [cmdId] [payload...]
```

## 项目结构

```
src/main/kotlin/com/moondrop/protocol/
├── GaiaConstants.kt          # 协议常量 (Feature ID, Command ID)
├── GaiaPacketBuilder.kt      # 命令包构造器
├── ResponseParser.kt         # 响应解析器
├── codec/
│   └── GaiaCodec.kt          # 帧编解码器
└── model/
    ├── Enums.kt              # 枚举定义 (AncMode, GainLevel)
    └── GaiaPacket.kt         # 数据包模型
```

## 参考资料

- [SpaceTravel-Protocol](https://github.com/pubglite55/SpaceTravel-Protocol) — Space Travel 抓包数据
- [moondrop-spp-controller](https://github.com/ribentianhuang38-boop/moondrop-spp-controller) — Android SPP 控制实现
- [MOONDROP-Pods-Manager](https://github.com/Zhaoyi-ya/OppoPodsManager) — Windows 桌面管理工具 (OPPO 协议参考)
- [OppoPods](https://github.com/1812z/OppoPods) — Android Xposed 模块 (OPPO 协议参考)

## License

MIT
