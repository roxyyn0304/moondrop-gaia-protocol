# MOONDROP Pudding SPP 协议库

MOONDROP Pudding (布丁) TWS 蓝牙 SPP 控制协议，基于实测逆向。

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

## 响应格式

| 类型 | 格式 | 说明 |
|------|------|------|
| QUERY 响应 | `[当前值]` | 1 字节，直接是当前状态 |
| SET 响应 | `[当前值, ?, ?]` | 3 字节，[0]=当前值，[1][2]=未知 |

## 支持的功能

| Feature | Cmd | 功能 | 状态 |
|---------|-----|------|------|
| 0x05 | 0x00 | 固件版本 | ✓ |
| 0x14 | 0x01 | 序列号 | ✓ |
| 0x15 | 0x00 | 设备ID | ✓ |
| 0x07 | 0x00 | EQ 状态 | ✓ |
| 0x0C | 各cmd | 配置查询 | ✓ |
| 0x0D | 0x07 | 设备状态 | ✓ |
| 0x1E | 0x01 | Gain 查询 | ✓ |
| 0x1E | 0x02 | Gain 设置 | ✓ |
| 0x40 | 0x03 | ANC 查询 | ✓ |
| 0x40 | 0x04 | ANC 设置 | ✓ |
| 0x40 | 0x29 | ANC 可用模式 | ✓ |

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
```

## Web 控制界面

```bash
python tools/webtest.py
# 或指定串口
python tools/webtest.py COM4
```

功能：
- ANC 降噪控制（关闭/通透/降噪）
- Gain 增益控制（高/中/低）
- 快捷命令（固件版本/序列号/设备ID/EQ/设备状态）
- 自定义命令发送
- 实时通信日志

## 参考资料

- [SpaceTravel-Protocol](https://github.com/pubglite55/SpaceTravel-Protocol) — Space Travel 抓包数据
- [moondrop-spp-controller](https://github.com/ribentianhuang38-boop/moondrop-spp-controller) — Android SPP 控制实现

## License

MIT
