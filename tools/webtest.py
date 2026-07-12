"""MOONDROP GAIA V3 蓝牙测试 Web UI。"""
import sys, json, time, threading
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
import serial

VENDOR_ID = 0x001D
FEATURE_NAMES = {0x00:"设备管理",0x01:"基础功能",0x03:"序列号",0x05:"固件版本",0x07:"EQ",0x0A:"自定义EQ",0x0C:"配置",0x0D:"设备状态",0x14:"序列号",0x15:"设备ID",0x1E:"增益",0x20:"编解码器",0x40:"ANC"}
ANC_MODES = {0x00:"关闭",0x02:"通透",0x04:"降噪",0x08:"自适应",0x10:"抗风噪"}

def feature_name(f):
    return FEATURE_NAMES.get(f&0xFE, "") or f"0x{f:02X}"

def build_tx(fid, cid, payload=b"", seq=0):
    pkt = bytearray(8 + len(payload))
    pkt[0]=0xFF; pkt[1]=0x04; pkt[2]=((len(payload)>>8)&0xFF); pkt[3]=(len(payload)&0xFF)
    pkt[4]=seq; pkt[5]=VENDOR_ID&0xFF; pkt[6]=fid&0xFF; pkt[7]=cid&0xFF
    if payload: pkt[8:]=payload
    return bytes(pkt)

def decode_packet(data):
    if len(data)<8 or data[0]!=0xFF or data[1]!=0x04: return None
    if (data[5]&0xFF)!=VENDOR_ID: return None
    f=data[6]&0xFF; c=data[7]&0xFF; p=list(data[8:]) if len(data)>8 else []
    ascii_str=""
    if p:
        chars=[chr(b) if 32<=b<127 else None for b in p]
        if sum(1 for ch in chars if ch)>=len(p)*0.6: ascii_str="".join(ch for ch in chars if ch)
    return {"feature":f,"feature_name":feature_name(f),"cmd":c,"cmd_hex":f"0x{c:02X}","payload":p,"payload_hex":" ".join(f"{b:02X}" for b in p),"ascii":ascii_str,"raw":data.hex(" ").upper()}

ser=None; ser_lock=threading.Lock()

def send_cmd(feature, cmd, payload_hex="", timeout=1.0):
    payload=bytes.fromhex(payload_hex.replace(" ","")) if payload_hex else b""
    tx=build_tx(feature, cmd, payload)
    with ser_lock:
        if not ser or not ser.is_open: return {"error":"串口未连接"}
        ser.reset_input_buffer(); ser.write(tx); ser.flush()
        deadline=time.time()+timeout; rx=b""
        while time.time()<deadline:
            if ser.in_waiting>0:
                rx+=ser.read(ser.in_waiting)
                if len(rx)>=8 and rx[0]==0xFF:
                    time.sleep(0.05)
                    if ser.in_waiting: rx+=ser.read(ser.in_waiting)
                    break
            time.sleep(0.05)
    if not rx: return {"tx":decode_packet(tx),"rx_raw":None,"error":"无响应"}
    return {"tx":decode_packet(tx),"rx_raw":rx.hex(" ").upper(),"decoded":decode_packet(rx)}

PRESETS=[
    {"name":"固件版本","feature":0x05,"cmd":0x00,"payload":""},
    {"name":"序列号","feature":0x14,"cmd":0x01,"payload":""},
    {"name":"设备ID","feature":0x15,"cmd":0x00,"payload":""},
    {"name":"ANC 查询","feature":0x40,"cmd":0x03,"payload":""},
    {"name":"ANC 关闭","feature":0x40,"cmd":0x04,"payload":"00"},
    {"name":"ANC 通透","feature":0x40,"cmd":0x04,"payload":"02"},
    {"name":"ANC 降噪","feature":0x40,"cmd":0x04,"payload":"04"},
    {"name":"ANC 自适应","feature":0x40,"cmd":0x04,"payload":"08"},
    {"name":"ANC 抗风噪","feature":0x40,"cmd":0x04,"payload":"10"},
    {"name":"ANC 可用模式","feature":0x40,"cmd":0x29,"payload":""},
    {"name":"Gain 查询","feature":0x1E,"cmd":0x01,"payload":""},
    {"name":"EQ 状态","feature":0x07,"cmd":0x00,"payload":""},
    {"name":"设备状态","feature":0x0D,"cmd":0x07,"payload":""},
]

HTML=r"""<!DOCTYPE html><html lang="zh"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>MOONDROP GAIA V3</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
:root{
  --bg:#08090c;--surface:#0f1218;--surface2:#161b24;--border:#1e2530;
  --text:#c8d1dc;--text2:#6b7688;--accent:#00d4ff;--accent2:#0090c0;
  --green:#3ddc84;--red:#ff5252;--amber:#ffc107;--dim:#3a4250;
}
body{font-family:'SF Mono','Cascadia Code',Consolas,monospace;background:var(--bg);color:var(--text);min-height:100vh;padding:20px}
.app{max-width:860px;margin:0 auto}

/* Header */
.hdr{display:flex;align-items:center;gap:12px;padding:16px 0 20px;border-bottom:1px solid var(--border)}
.hdr h1{font-size:15px;font-weight:600;letter-spacing:.5px;color:var(--accent)}
.hdr .tag{font-size:11px;color:var(--text2);background:var(--surface2);padding:3px 8px;border-radius:4px}
.dot{width:8px;height:8px;border-radius:50%;display:inline-block;margin-right:6px}
.dot.ok{background:var(--green)}.dot.off{background:var(--dim)}

/* Grid */
.grid{display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:16px}
@media(max-width:640px){.grid{grid-template-columns:1fr}}

/* Cards */
.card{background:var(--surface);border:1px solid var(--border);border-radius:10px;padding:16px}
.card h2{font-size:11px;text-transform:uppercase;letter-spacing:1px;color:var(--text2);margin-bottom:12px;font-weight:500}

/* ANC Panel - hero */
.anc{background:linear-gradient(135deg,#0a1628,#0f1a2e);border:1px solid #1a2d44;border-radius:12px;padding:20px;margin-bottom:16px;position:relative;overflow:hidden}
.anc::before{content:'';position:absolute;top:-50%;right:-20%;width:200px;height:200px;background:radial-gradient(circle,rgba(0,212,255,.06),transparent 70%);pointer-events:none}
.anc h2{font-size:11px;text-transform:uppercase;letter-spacing:1px;color:var(--accent);margin-bottom:14px;font-weight:500}
.anc-modes{display:flex;gap:8px;flex-wrap:wrap}
.anc-btn{padding:10px 16px;border-radius:8px;border:1px solid var(--border);background:var(--surface);color:var(--text);font-family:inherit;font-size:13px;cursor:pointer;transition:all .15s;position:relative}
.anc-btn:hover{border-color:var(--accent);background:#0d1a28}
.anc-btn.active{border-color:var(--accent);background:rgba(0,212,255,.1);color:var(--accent);box-shadow:0 0 12px rgba(0,212,255,.15)}
.anc-btn .val{font-size:10px;color:var(--text2);margin-top:2px}
.anc-sub{display:none;margin-top:10px;padding-top:10px;border-top:1px solid var(--border)}
.anc-sub .sub-label{font-size:11px;color:var(--text2);margin-right:8px}
.anc-sub-btn{padding:6px 12px;border-radius:6px;border:1px solid var(--border);background:var(--surface);color:var(--text);font-family:inherit;font-size:12px;cursor:pointer;transition:all .12s;margin-right:6px}
.anc-sub-btn:hover{border-color:var(--accent)}
.anc-sub-btn.active{border-color:var(--accent);color:var(--accent);background:rgba(0,212,255,.1)}
.anc-status{margin-top:12px;font-size:12px;color:var(--text2);display:flex;align-items:center;gap:8px}
.anc-status .current{color:var(--accent);font-weight:600}

/* Preset grid */
.presets{display:grid;grid-template-columns:repeat(auto-fill,minmax(110px,1fr));gap:6px}
.preset{padding:8px 10px;border-radius:6px;border:1px solid var(--border);background:var(--surface);color:var(--text);font-family:inherit;font-size:11px;cursor:pointer;text-align:left;transition:all .12s;line-height:1.4}
.preset:hover{border-color:var(--accent);background:#0d1a28}
.preset:active{transform:scale(.97)}
.preset .feat{color:var(--accent);font-size:10px}

/* Custom command */
.custom{display:flex;gap:6px;flex-wrap:wrap;align-items:end}
.custom label{font-size:10px;color:var(--text2);display:block;margin-bottom:3px}
.custom input{background:var(--bg);color:var(--text);border:1px solid var(--border);border-radius:4px;padding:6px 8px;font:12px monospace;width:60px}
.custom input.wide{width:120px}
.custom button{padding:6px 14px;border-radius:6px;border:1px solid var(--accent);background:rgba(0,212,255,.1);color:var(--accent);font-family:inherit;font-size:12px;cursor:pointer}
.custom button:hover{background:rgba(0,212,255,.2)}

/* Log */
#log{max-height:440px;overflow-y:auto;font-size:11px;line-height:1.6}
.log-entry{padding:8px 0;border-bottom:1px solid var(--border)}
.log-entry:last-child{border-bottom:none}
.log-ts{color:var(--dim);font-size:10px}
.log-tx{color:var(--amber);font-weight:500}
.log-rx{color:var(--green);font-weight:500}
.log-err{color:var(--red)}
.log-info{color:var(--text2)}
.log-hex{color:var(--dim);font-size:10px;word-break:break-all;margin-top:2px}
.log-decode{margin-top:3px;padding:4px 8px;background:var(--bg);border-radius:4px;font-size:11px}
.log-decode .feat{color:var(--accent)}.log-decode .ascii{color:var(--green)}.log-decode .err{color:var(--red)}

.empty{color:var(--dim);font-size:12px;padding:16px 0;text-align:center}
</style></head><body>
<div class="app">
<div class="hdr">
  <h1><span class="dot" id="st"></span>MOONDROP GAIA V3</h1>
  <span class="tag">Pudding MD-TWS-056</span>
  <span class="tag">Feature 0x40 ANC</span>
</div>

<div class="anc">
  <h2>降噪控制</h2>
  <div class="anc-modes" id="anc-modes">
    <button class="anc-btn" data-val="00"><div>关闭</div><div class="val">0x00</div></button>
    <button class="anc-btn" data-val="02"><div>通透</div><div class="val">0x02</div></button>
    <button class="anc-btn" data-val="04"><div>降噪</div><div class="val">0x04</div></button>
  </div>
  <div class="anc-sub" id="anc-sub" style="display:none">
    <span class="sub-label">子模式:</span>
    <button class="anc-sub-btn" data-val="08">自适应</button>
    <button class="anc-sub-btn" data-val="10">抗风噪</button>
  </div>
  <div class="anc-status">状态: <span class="current" id="anc-status">—</span></div>
</div>

<div class="grid">
<div class="card">
  <h2>快捷命令</h2>
  <div class="presets" id="presets"></div>
</div>
<div class="card">
  <h2>自定义命令</h2>
  <div class="custom">
    <div><label>Feature</label><input id="f" value="40"></div>
    <div><label>Cmd</label><input id="c" value="03"></div>
    <div><label>载荷</label><input id="p" class="wide" placeholder="hex"></div>
    <button onclick="sendCustom()">发送</button>
  </div>
</div>
</div>

<div class="card">
  <h2>通信日志</h2>
  <div id="log"><div class="empty">Ready</div></div>
</div>
</div>

<script>
const log=document.getElementById('log'),st=document.getElementById('st');
let ancCurrent='—';

function addLog(h){if(log.children===1&&log.firstElementChild.classList.contains('empty'))log.innerHTML='';const d=document.createElement('div');d.className='log-entry';d.innerHTML=h;log.prepend(d);while(log.children>50)log.removeChild(log.lastChild)}

function decodeInfo(d){if(!d)return'';let s=`<span class="feat">F:0x${d.feature.toString(16).padStart(2,'0')}</span> <span class="info">(${d.feature_name})</span> `;s+=`C:${d.cmd_hex}`;if(d.ascii)s+=` → "${d.ascii}"`;else if(d.payload.length)s+=` [${d.payload_hex}]`;return s}

function modeName(m){return{0:'关闭',2:'通透',4:'降噪',8:'自适应(降噪)',16:'抗风噪(降噪)'}[m]||'未知'}

function updateAncUI(mode){
  document.getElementById('anc-status').textContent=modeName(mode);
  document.querySelectorAll('.anc-btn').forEach(b=>b.classList.toggle('active',parseInt(b.dataset.val,16)===mode));
  const isNc=(mode===4||mode===8||mode===16);
  document.getElementById('anc-sub').style.display=isNc?'block':'none';
  document.querySelectorAll('.anc-sub-btn').forEach(b=>b.classList.toggle('active',parseInt(b.dataset.val,16)===mode));
}

async function send(f,c,p){
  document.querySelectorAll('.anc-btn,.anc-sub-btn,.preset,.custom button').forEach(b=>b.disabled=true);
  st.className='dot';
  const r=await fetch(`/api/send?feature=${f}&cmd=${c}&payload=${encodeURIComponent(p||'')}`);
  const j=await r.json();
  const t=new Date().toLocaleTimeString('zh',{hour12:false,hour:'2-digit',minute:'2-digit',second:'2-digit'});
  if(j.error&&!j.rx_raw){addLog(`<span class="log-ts">${t}</span> <span class="log-tx">发送失败</span><div class="log-decode"><span class="err">${j.error}</span></div>`);st.className='dot off'}
  else{
    let h=`<span class="log-ts">${t}</span><br><span class="log-tx">→ 发送</span> <span class="log-hex">${j.tx?j.tx.raw:''}</span><div class="log-decode">${decodeInfo(j.tx)}</div>`;
    if(j.decoded){h+=`<span class="log-rx">← 接收</span> <span class="log-hex">${j.rx_raw}</span><div class="log-decode">${decodeInfo(j.decoded)}</div>`;
      if((j.decoded.feature&0xFE)===0x40&&j.decoded.cmd===3){updateAncUI(j.decoded.payload[0])}
    }else if(j.rx_raw){h+=`<span class="log-rx">← 接收</span> <span class="log-hex">${j.rx_raw}</span>`}
    else{h+=`<span class="log-info">设备无响应</span>`}
    addLog(h);st.className='dot ok';
  }
  await new Promise(r=>setTimeout(r,1500));
  document.querySelectorAll('.anc-btn,.anc-sub-btn,.preset,.custom button').forEach(b=>b.disabled=false);
}

function sendCustom(){const f=parseInt(document.getElementById('f').value,16);const c=parseInt(document.getElementById('c').value,16);send(f,c,document.getElementById('p').value)}

// ANC buttons
document.getElementById('anc-modes').addEventListener('click',e=>{const b=e.target.closest('.anc-btn');if(b){send(0x40,0x04,b.dataset.val);updateAncUI(parseInt(b.dataset.val,16))}});
document.getElementById('anc-sub').addEventListener('click',e=>{const b=e.target.closest('.anc-sub-btn');if(b){send(0x40,0x04,b.dataset.val);updateAncUI(parseInt(b.dataset.val,16))}});

// Presets
fetch('/api/presets').then(r=>r.json()).then(ps=>{const g=document.getElementById('presets');ps.forEach(p=>{const b=document.createElement('button');b.className='preset';b.innerHTML=`<div>${p.name}</div><div class="feat">F:0x${p.feature.toString(16).padStart(2,'0')}</div>`;b.onclick=()=>send(p.feature,p.cmd,p.payload);g.appendChild(b)})});

// Initial ANC query
send(0x40,0x03);

fetch('/api/status').then(r=>r.json()).then(j=>{st.className=j.connected?'dot ok':'dot off'});
</script></body></html>"""

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        p=urlparse(self.path); q=parse_qs(p.query)
        if p.path in("/","/index.html"):
            self.send_response(200);self.send_header("Content-Type","text/html; charset=utf-8");self.end_headers();self.wfile.write(HTML.encode())
        elif p.path=="/api/status":self._json({"connected":ser is not None and ser.is_open,"port":ser.port if ser else None})
        elif p.path=="/api/presets":self._json(PRESETS)
        elif p.path=="/api/send":self._json(send_cmd(int(q.get("feature",["0"])[0]),int(q.get("cmd",["0"])[0]),q.get("payload",[""])[0]))
        else:self.send_error(404)
    def _json(self,d):
        body=json.dumps(d,ensure_ascii=False).encode()
        self.send_response(200);self.send_header("Content-Type","application/json");self.end_headers();self.wfile.write(body)
    def log_message(self,*a):pass

def find_moondrop_port():
    """自动查找 MOONDROP 蓝牙串口 (VID=0x05D6)"""
    import serial.tools.list_ports
    for p in serial.tools.list_ports.comports():
        if "05D6" in p.hwid.upper():
            return p.device
    # 没找到则用第一个蓝牙串口
    for p in serial.tools.list_ports.comports():
        if "BTHENUM" in p.hwid and "1101" in p.hwid:
            return p.device
    return None

def main():
    global ser;port,web_port="COM3",8080
    for i,a in enumerate(sys.argv[1:]):
        if a=="--port"and i<len(sys.argv)-2:web_port=int(sys.argv[i+2])
        elif a.startswith("COM"):port=a
    # 自动检测串口
    if port=="COM3":
        found=find_moondrop_port()
        if found:port=found
    print(f"  串口: {port}")
    try:ser=serial.Serial(port,115200,timeout=0.5,write_timeout=2);print(f"  {port} OK")
    except Exception as e:print(f"  {port} failed: {e}")
    server=HTTPServer(("127.0.0.1",web_port),Handler)
    print(f"\n  http://127.0.0.1:{web_port}\n")
    try:server.serve_forever()
    except KeyboardInterrupt:pass
    finally:
        if ser and ser.is_open:ser.close()
        server.server_close()

if __name__=="__main__":main()
