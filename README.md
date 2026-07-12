# MOONDROP Pudding SPP 协议库

MOONDROP Pudding (布丁) TWS 蓝牙 SPP 控制协议，基于 btsnoop 抓包逆向。

## 设备信息

| 参数 | 值 |
|------|-----|
| 设备 | MOONDROP Pudding (MD-TWS-056) |
| 芯片 | 杰理 (Jieli) VID=0x05D6 |
| 固件 | 3.5.2 |
| 蓝牙 | SPP → 虚拟串口 |
| Vendor ID | 29 (0x001D) |

## 协议格式

```
FF 04 [Len:2 BE] [Seq:1] [Vendor:1] [Feature:1] [Cmd:1] [Payload...]
```

- Len = 1 + 1 + payload.size（feature + cmd + payload）
- 总包长 = 8 + payload.size
- 响应 Feature = 请求 Feature | 0x01 (bit0)

### 协议结构（btsnoop 抓包确认）

大部分查询使用 **Feature=0x00**，Cmd 为功能号。ANC/Gain/Codec 使用独立 Feature ID。

| Feature | 用途 |
|---------|------|
| 0x00 | 基础查询（固件版本、序列号、设备ID、EQ、配置、设备状态） |
| 0x1E | Gain 增益控制 |
| 0x20 | 编解码器（LDAC/LC3） |
| 0x40 | ANC 降噪控制 |

## 支持的功能 (btsnoop 抓包验证)

| Feature | Cmd | 功能 | 状态 |
|---------|-----|------|------|
| 0x00 | 0x01 | 支持的命令 | ✓ |
| 0x00 | 0x05 | 固件版本 | ✓ |
| 0x00 | 0x07 | EQ 查询 | ✓ |
| 0x00 | 0x0C | 配置查询 | ✓ |
| 0x00 | 0x0D | 设备状态 | ✓ |
| 0x00 | 0x14 | 序列号 | ✓ |
| 0x00 | 0x15 | 设备ID | ✓ |
| 0x1E | 0x01 | Gain 查询 | ✓ |
| 0x1E | 0x02 | Gain 设置 | ✓ |
| 0x40 | 0x03 | ANC 查询 | ✓ |
| 0x40 | 0x04 | ANC 设置 | ✓ |
| 0x40 | 0x29 | ANC 可用模式 | ✓ |
| 0x20 | 0x05 | LDAC 状态 | ✓ |

## ANC 模式

| 值 | 模式 | 状态 |
|----|------|------|
| 0x00 | 关闭 | ✓ 可用 |
| 0x02 | 通透 | ✓ 可用 |
| 0x04 | 降噪 | ✓ 可用 |
| 0x08 | 自适应 | ✗ 暂不可用 |
| 0x10 | 抗风噪 | ✗ 暂不可用 |

## Gain 级别

| 值 | 级别 |
|----|------|
| 0x00 | 高 |
| 0x01 | 中 |
| 0x02 | 低 |

## 快速开始

```kotlin
import com.moondrop.protocol.GaiaPacketBuilder
import com.moondrop.protocol.codec.GaiaCodec

// ANC 查询
val query = GaiaPacketBuilder.ancQuery()
val bytes = GaiaCodec.encode(query)

// ANC 设置为降噪
val set = GaiaPacketBuilder.ancSet(AncMode.NOISE_CANCEL)
val bytes2 = GaiaCodec.encode(set)

// Gain 设置为低
val gainSet = GaiaPacketBuilder.gainSet(GainLevel.LOW)
val bytes3 = GaiaCodec.encode(gainSet)

// 固件版本查询
val fwQuery = GaiaPacketBuilder.firmwareVersionQuery()
val bytes4 = GaiaCodec.encode(fwQuery)
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
- ANC 降噪控制（关闭/通透/降噪）
- Gain 增益控制（高/中/低）
- 快捷命令（固件版本/序列号/设备ID/EQ/设备状态）
- 自定义命令发送
- 实时通信日志（TX/RX 卡片式显示）

## 参考资料

- [SpaceTravel-Protocol](https://github.com/pubglite55/SpaceTravel-Protocol) — Space Travel 抓包数据
- [moondrop-spp-controller](https://github.com/ribentianhuang38-boop/moondrop-spp-controller) — Android SPP 控制实现

## License

MIT
