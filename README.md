# MOONDROP Pudding SPP 协议库

MOONDROP Pudding (布丁) TWS 蓝牙 SPP 控制协议，基于实测逆向。

## 设备信息

| 参数 | 值 |
|------|-----|
| 设备 | MOONDROP Pudding (MD-TWS-056) |
| 芯片 | 杰理 (Jieli) VID=0x05D6 |
| 固件 | 3.5.2 |
| 蓝牙 | SPP → 虚拟串口 (COM3) |
| Vendor ID | 29 (0x001D) |

## 协议格式

```
FF 04 [Len:2 BE] [00] [Vendor:1] [Feature:1] [Cmd:1] [Payload...]
```

- Len = payload.size
- 总包长 = 8 + Len
- 响应 Feature = 请求 Feature | 0x01 (bit0)

## 支持的功能

| Feature | Cmd | 功能 | 状态 |
|---------|-----|------|------|
| 0x05 | 0x00 | 固件版本 | ✓ |
| 0x14 | 0x01 | 序列号 | ✓ |
| 0x15 | 0x00 | 设备ID | ✓ |
| 0x07 | 0x00 | EQ 状态 | ✓ |
| 0x0C | 各cmd | 配置查询 | ✓ |
| 0x0D | 0x07 | 设备状态 | ✓ |
| 0x1E | 0x01/02 | Gain 查询/设置 | ✓ |
| **0x40** | **0x03/04/29** | **ANC 降噪** | **✓** |

## ANC 模式

| 值 | 模式 |
|----|------|
| 0x00 | 关闭 |
| 0x02 | 通透 |
| 0x04 | 降噪 |
| 0x08 | 自适应 (降噪子模式) |
| 0x10 | 抗风噪 (降噪子模式) |

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
```

## 参考资料

- [SpaceTravel-Protocol](https://github.com/pubglite55/SpaceTravel-Protocol) — Space Travel 抓包数据
- [moondrop-spp-controller](https://github.com/ribentianhuang38-boop/moondrop-spp-controller) — Android SPP 控制实现

## License

MIT
