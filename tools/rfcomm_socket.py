"""通过 Winsock RFCOMM socket 直接连接 MOONDROP，绕过虚拟串口。"""
import socket
import struct
import time

# MOONDROP Pudding MAC (从 PnP 获取)
ADDR = "D2:D1:11:3A:B6:72"
SPP_UUID = "00001101-0000-1000-8000-00805f9b34fb"
PORT = 1  # RFCOMM channel

def uuid_to_bytes(uuid_str):
    """将 UUID 字符串转为 16 字节的大端格式。"""
    u = uuid_str.replace("-", "")
    return bytes.fromhex(u)

def build_tx(fid, cid, payload=b"", seq=0):
    body_len = 1 + 1 + len(payload)
    pkt = bytearray(8 + len(payload))
    pkt[0] = 0xFF; pkt[1] = 0x04
    pkt[2] = (body_len >> 8) & 0xFF; pkt[3] = body_len & 0xFF
    pkt[4] = seq; pkt[5] = 0x1D; pkt[6] = fid; pkt[7] = cid
    if payload: pkt[8:] = payload
    return bytes(pkt)

def decode_packet(data):
    if len(data) < 8 or data[0] != 0xFF or data[1] != 0x04:
        return None
    f = data[6]; c = data[7]
    p = list(data[8:]) if len(data) > 8 else []
    ascii_str = ""
    if p:
        chars = [chr(b) if 32 <= b < 127 else None for b in p]
        if sum(1 for ch in chars if ch) >= len(p) * 0.6:
            ascii_str = "".join(ch for ch in chars if ch)
    return {"feature": f, "cmd": c, "payload": p, "ascii": ascii_str, "raw": " ".join(f"{b:02X}" for b in data)}

def try_connect(addr, uuid, channel):
    """尝试通过 RFCOMM socket 连接。"""
    sock = socket.socket(socket.AF_BTH, socket.SOCK_STREAM, socket.BTHPROTO_RFCOMM)
    sock.settimeout(10)
    addr_bytes = bytes.fromhex(addr.replace(":", ""))
    # Windows Bluetooth socketaddr: 2 bytes family + 8 bytes addr + 1 byte port
    sockaddr = struct.pack("H", 32) + bytes(reversed(addr_bytes)) + struct.pack("B", channel)
    try:
        sock.connect(sockaddr)
        return sock
    except Exception as e:
        sock.close()
        return None

# 先扫描看能找到哪些 RFCOMM 通道
print("=== 尝试 RFCOMM 连接 ===")
print(f"目标: {ADDR}")

# 尝试不同通道
for ch in range(1, 6):
    print(f"\n尝试通道 {ch}...")
    sock = try_connect(ADDR, SPP_UUID, ch)
    if sock:
        print(f"  通道 {ch} 连接成功!")
        # 发送固件版本查询
        tx = build_tx(0x05, 0x00)
        print(f"  TX: {' '.join(f'{b:02X}' for b in tx)}")
        sock.send(tx)
        time.sleep(1)
        try:
            rx = sock.recv(1024)
            if rx:
                print(f"  RX: {' '.join(f'{b:02X}' for b in rx)}")
                pkt = decode_packet(rx)
                if pkt:
                    print(f"  解析: feature=0x{pkt['feature']:02X} cmd=0x{pkt['cmd']:02X}")
                    if pkt["ascii"]:
                        print(f"  ASCII: {pkt['ascii']}")
            else:
                print("  无响应")
        except socket.timeout:
            print("  接收超时")
        sock.close()
        break
    else:
        print(f"  通道 {ch} 失败")
