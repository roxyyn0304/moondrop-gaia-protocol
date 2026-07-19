# MOONDROP Pudding Protocol

MOONDROP Pudding (布丁) TWS 蓝牙 SPP 控制协议库，基于 btsnoop 抓包逆向 + DEX 反编译分析。

## 设备信息

| 参数 | 值 |
|------|-----|
| 设备 | MOONDROP Pudding (MD-TWS-056) |
| 芯片 | 杰理 (Jieli) VID=0x05D6 |
| 固件 | 3.5.2 |
| 蓝牙 | SPP → 虚拟串口 |
| Vendor ID | 29 (0x001D) |
| APP | MOONDROP Link v2.23.1c |

## 协议格式

```
FF 04 [Len:2 BE] [Seq:1] [Vendor:1] [Feature:1] [Cmd:1] [Payload...]
```

- Len = 1 + 1 + payload.size（feature + cmd + payload）
- 总包长 = 8 + payload.size
- 响应 Feature = 请求 Feature | 0x01 (bit0)

### 响应格式

| 类型 | 格式 | 说明 |
|------|------|------|
| QUERY 响应 | `[当前值]` | 1 字节，直接是当前状态 |
| SET 响应 | `[当前值, ?, ?]` | 3 字节，[0]=当前值，[1][2]=未知 |

## Feature ID

| Feature | 响应 | 用途 | 状态 |
|---------|------|------|------|
| 0x00 | 0x01 | 基础查询（固件版本、序列号、设备ID、配置、设备状态） | ✓ 已确认 |
| 0x0A | 0x0B | EQ 参数读写（频率/gain/Q） | ✓ 实测验证 |
| 0x1A | 0x1B | 设备管理（电量查询） | ✓ 实测验证 |
| 0x1E | 0x1F | Gain 增益控制 | ✓ 实测验证 |
| 0x20 | 0x21 | 编解码器（LDAC/LC3） | ✓ 已确认 |
| 0x28 | 0x29 | 设备信息（连接设备名、设备子类型） | ✓ btsnoop 确认 |
| 0x2C | 0x2D | 电池通知 | ✓ btsnoop 确认 |
| 0x40 | 0x41 | ANC 降噪控制 | ✓ 实测验证 |

## 支持的功能

### 基础查询 (Feature=0x00)

| Cmd | 功能 | 状态 |
|-----|------|------|
| 0x01 | 支持的命令列表 | ✓ |
| 0x05 | 固件版本 | ✓ |
| 0x07 | EQ 状态（兼心跳） | ✓ |
| 0x0C | 配置查询（子命令在 payload 中） | ✓ |
| 0x0D | 设备状态 | ✓ |
| 0x14 | 序列号 | ✓ |
| 0x15 | 设备 ID | ✓ |

### ANC 降噪控制 (Feature=0x40)

| Cmd | 功能 | 状态 |
|-----|------|------|
| 0x03 | ANC 状态查询 | ✓ |
| 0x04 | ANC 模式设置 | ✓ |
| 0x29 | ANC 可用模式查询 | ✓ |
| 0x81 | ANC 状态异步推送 | ✓ |
| 0x84 | ANC 错误通知 | ✓ |

### Gain 增益 (Feature=0x1E)

| Cmd | 功能 | 状态 |
|-----|------|------|
| 0x01 | Gain 查询 | ✓ |
| 0x02 | Gain 设置 | ✓ |

### 编解码器 (Feature=0x20)

| Cmd | 功能 | 状态 |
|-----|------|------|
| 0x01 | LC3 状态 | ✓ |
| 0x05 | LDAC 状态 | ✓ |
| 0x06 | 编解码器设置 | ✓ |

### 设备管理 (Feature=0x1A)

| Cmd | 功能 | 状态 |
|-----|------|------|
| 0x00 | 设备管理查询（电量） | ✓ |
| 0x01 | 设备管理设置 | ✓ |

### 设备信息 (Feature=0x28)

| Cmd | 功能 | 状态 |
|-----|------|------|
| 0x01 | 设备信息 | ✓ |
| 0x03 | 设备子类型 | ✓ |
| 0x05 | 连接设备名 | ✓ |

### EQ 参数 (Feature=0x0A)

| Cmd | 功能 | 状态 |
|-----|------|------|
| 0x03 | EQ 带宽 | ✓ |
| 0x05 | EQ 参数查询 | ✓ |
| 0x06 | EQ 参数设置 | ✓ |

### 电池通知 (Feature=0x2C)

| Cmd | 功能 | 状态 |
|-----|------|------|
| 0x01 | 电池查询 1 | ✓ |
| 0x02 | 电池查询 2 | ✓ |
| 0x03 | 电池异步通知 | ✓ |

## ANC 模式

| 值 | 模式 | 说明 |
|----|------|------|
| 0x00 | 关闭 | 关闭 ANC |
| 0x01 | 自适应 | 自适应降噪 |
| 0x02 | 通透 | 通透模式 |
| 0x03 | 抗风噪 | 抗风噪模式 |
| 0x04 | 降噪 | 降噪组入口，SET 后恢复上次子模式 |

## Gain 级别

| 值 | 级别 |
|----|------|
| 0x00 | 高 |
| 0x01 | 中 |
| 0x02 | 低 |

## 项目结构

```
src/main/kotlin/com/moondrop/protocol/
├── GaiaConstants.kt       # 协议常量（Feature/Cmd ID、ANC 模式）
├── GaiaPacketBuilder.kt   # 命令包构造器
├── ResponseParser.kt      # 响应解析器
├── codec/
│   └── GaiaCodec.kt       # 帧编解码 + 流式解码器
└── model/
    ├── GaiaPacket.kt      # 数据包模型
    ├── Enums.kt           # AncMode / GainLevel 枚举
    └── EqBand.kt          # EQ 频段配置
```

## 快速开始

```kotlin
import com.moondrop.protocol.GaiaPacketBuilder
import com.moondrop.protocol.codec.GaiaCodec
import com.moondrop.protocol.ResponseParser

// ANC 查询
val query = GaiaPacketBuilder.ancQuery()
val bytes = GaiaCodec.encode(query)

// ANC 设置为自适应降噪
val set = GaiaPacketBuilder.ancSet(AncMode.ADAPTIVE)
val bytes2 = GaiaCodec.encode(set)

// 查询 ANC 可用模式
val avail = GaiaPacketBuilder.ancAvailable()
val bytes3 = GaiaCodec.encode(avail)

// Gain 设置为低
val gainSet = GaiaPacketBuilder.gainSet(GainLevel.LOW)
val bytes4 = GaiaCodec.encode(gainSet)

// 固件版本查询
val fwQuery = GaiaPacketBuilder.firmwareVersionQuery()
val bytes5 = GaiaCodec.encode(fwQuery)

// 批量状态查询
val packets = GaiaPacketBuilder.statusQueryPackets()
packets.forEach { val raw = GaiaCodec.encode(it); /* send raw */ }

// 解析响应
val response: GaiaPacket = GaiaCodec.decode(rxBytes) ?: return
val fw = ResponseParser.parseFirmwareVersion(response)
val anc = ResponseParser.parseAncMode(response)
val gain = ResponseParser.parseGainLevel(response)
val ldac = ResponseParser.parseLdacStatus(response)
val lc3 = ResponseParser.parseLc3Status(response)

// 流式解码（处理分包/粘包）
val decoder = GaiaCodec.StreamDecoder()
val packets = decoder.feed(serialData)
```

## Web 控制界面

```bash
# 自动查找 MOONDROP 蓝牙串口
python tools/webtest.py

# 指定串口
python tools/webtest.py --port COM3

# 指定 Web 端口
python tools/webtest.py --web-port 9090

# 同时指定
python tools/webtest.py --port COM3 --web-port 9090
```

功能：
- **自动串口检测** — 通过 VID=0x05D6 自动查找 MOONDROP 设备
- ANC 降噪控制（关闭/通透/降噪/自适应/抗风噪）
- Gain 增益控制（高/中/低）
- 快捷命令（固件版本/序列号/设备ID/EQ/设备状态/LDAC/LC3/连接设备名）
- 自定义 Feature/Cmd 命令发送
- 电量显示（左耳/右耳/充电盒）
- 实时通信日志（TX=琥珀色, RX=绿色, ERR=红色）

## 辅助工具

```bash
# 检查蓝牙设备状态 (PowerShell)
powershell tools/check_bt.ps1

# 重置 RFCOMM 设备 (PowerShell)
powershell tools/fix_rfc.ps1

# 一键启动 Web 控制界面
Testing.bat
```

## 构建与测试

```bash
# Kotlin 库 (Gradle, JVM 17)
./gradlew build          # 编译 + 运行单元测试

# Web 控制界面
python tools/webtest.py  # 自动检测串口，启动 Web UI
```

## 参考资料

- [SpaceTravel-Protocol](https://github.com/pubglite55/SpaceTravel-Protocol) — Space Travel 抓包数据
- [moondrop-spp-controller](https://github.com/ribentianhuang38-boop/moondrop-spp-controller) — Android SPP 控制实现

## License

MIT
