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
    body_len = 1 + 1 + len(payload)  # feature + cmd + payload
    pkt = bytearray(8 + len(payload))
    pkt[0]=0xFF; pkt[1]=0x04; pkt[2]=((body_len>>8)&0xFF); pkt[3]=(body_len&0xFF)
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

def send_cmd(feature, cmd, payload_hex="", timeout=2.0, retries=2):
    payload=bytes.fromhex(payload_hex.replace(" ","")) if payload_hex else b""
    tx=build_tx(feature, cmd, payload)
    last_rx=None
    for attempt in range(retries+1):
        with ser_lock:
            if not ser or not ser.is_open: return {"error":"串口未连接"}
            ser.reset_input_buffer(); ser.write(tx); ser.flush()
            deadline=time.time()+timeout; rx=b""
            while time.time()<deadline:
                if ser.in_waiting>0:
                    rx+=ser.read(ser.in_waiting)
                    # 检查是否收到完整包: 至少 8 字节头 + body_len
                    if len(rx)>=8 and rx[0]==0xFF and rx[1]==0x04:
                        body_len=(rx[2]<<8)|rx[3]
                        total=8+body_len
                        if len(rx)>=total:
                            rx=rx[:total]
                            break
                    time.sleep(0.02)
                else:
                    time.sleep(0.02)
        last_rx=rx
        if rx: break
        if attempt<retries: time.sleep(0.1)
    if not last_rx: return {"tx":decode_packet(tx),"rx_raw":None,"error":"无响应"}
    return {"tx":decode_packet(tx),"rx_raw":last_rx.hex(" ").upper(),"decoded":decode_packet(last_rx)}

PRESETS=[
    {"name":"固件版本","feature":0x05,"cmd":0x00,"payload":""},
    {"name":"序列号","feature":0x14,"cmd":0x01,"payload":""},
    {"name":"设备ID","feature":0x15,"cmd":0x00,"payload":""},
    {"name":"EQ 状态","feature":0x07,"cmd":0x00,"payload":""},
    {"name":"设备状态","feature":0x0D,"cmd":0x07,"payload":""},
]

HTML=r"""<!DOCTYPE html><html lang="zh"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>MOONDROP Pudding Controller</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
:root{
  --bg:#0b0d11;--surface:#13161d;--surface2:#1c2029;--border:#282d38;
  --text:#e2e6ed;--text2:#6b7385;--accent:#3b82f6;--amber:#f59e0b;
  --green:#22c55e;--red:#ef4444;--dim:#3d4455;
}
body{font-family:system-ui,-apple-system,'Segoe UI',sans-serif;background:var(--bg);color:var(--text);min-height:100vh;padding:16px;line-height:1.5}
.app{max-width:900px;margin:0 auto}

/* Header */
.hdr{display:flex;align-items:center;justify-content:space-between;padding:12px 0 16px;border-bottom:1px solid var(--border);margin-bottom:20px}
.hdr-left{display:flex;align-items:center;gap:10px}
.hdr h1{font-size:14px;font-weight:600;letter-spacing:.3px}
.hdr .device{font-size:12px;color:var(--text2);font-family:'SF Mono','Cascadia Code',Consolas,monospace}
.status-badge{display:flex;align-items:center;gap:6px;font-size:11px;padding:4px 10px;border-radius:20px;border:1px solid var(--border);background:var(--surface)}
.status-badge .dot{width:7px;height:7px;border-radius:50%}
.status-badge .dot.ok{background:var(--green);box-shadow:0 0 6px rgba(34,197,94,.4)}
.status-badge .dot.off{background:var(--dim)}

/* Control panels - side by side */
.panels{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:16px}
@media(max-width:640px){.panels{grid-template-columns:1fr}}

.panel{background:var(--surface);border:1px solid var(--border);border-radius:10px;padding:16px;position:relative}
.panel::before{content:'';position:absolute;top:0;left:0;right:0;height:2px;border-radius:10px 10px 0 0}
.panel.anc::before{background:linear-gradient(90deg,var(--accent),transparent)}
.panel.gain::before{background:linear-gradient(90deg,var(--amber),transparent)}

.panel-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:14px}
.panel-title{font-size:11px;text-transform:uppercase;letter-spacing:1px;font-weight:600}
.panel.anc .panel-title{color:var(--accent)}
.panel.gain .panel-title{color:var(--amber)}
.panel-status{font-family:'SF Mono','Cascadia Code',Consolas,monospace;font-size:13px;font-weight:600;padding:2px 8px;border-radius:4px;background:var(--bg)}
.panel.anc .panel-status{color:var(--accent)}
.panel.gain .panel-status{color:var(--amber)}

.mode-btns{display:flex;gap:6px}
.mode-btn{flex:1;padding:10px 8px;border-radius:6px;border:1px solid var(--border);background:var(--bg);color:var(--text2);font-family:inherit;font-size:12px;font-weight:500;cursor:pointer;transition:all .15s;text-align:center}
.mode-btn:hover{border-color:var(--border);color:var(--text);background:var(--surface2)}
.mode-btn.active{border-color:currentColor;background:rgba(255,255,255,.03)}
.panel.anc .mode-btn.active{color:var(--accent);border-color:var(--accent);box-shadow:0 0 12px rgba(59,130,246,.15)}
.panel.gain .mode-btn.active{color:var(--amber);border-color:var(--amber);box-shadow:0 0 12px rgba(245,158,11,.15)}
.mode-btn .label{display:block}
.mode-btn .hex{display:block;font-family:'SF Mono','Cascadia Code',Consolas,monospace;font-size:9px;color:var(--dim);margin-top:2px}
.mode-btn.active .hex{opacity:.7}

/* Tools row */
.tools{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:16px}
@media(max-width:640px){.tools{grid-template-columns:1fr}}

.tool-card{background:var(--surface);border:1px solid var(--border);border-radius:10px;padding:14px}
.tool-card h3{font-size:10px;text-transform:uppercase;letter-spacing:1px;color:var(--text2);margin-bottom:10px;font-weight:500}

.presets{display:flex;flex-wrap:wrap;gap:5px}
.preset{padding:5px 10px;border-radius:4px;border:1px solid var(--border);background:var(--bg);color:var(--text2);font-family:inherit;font-size:11px;cursor:pointer;transition:all .12s}
.preset:hover{border-color:var(--accent);color:var(--text);background:var(--surface2)}
.preset:active{transform:scale(.97)}

.custom{display:flex;gap:5px;flex-wrap:wrap;align-items:end}
.custom label{font-size:9px;color:var(--text2);display:block;margin-bottom:2px;text-transform:uppercase;letter-spacing:.5px}
.custom input{background:var(--bg);color:var(--text);border:1px solid var(--border);border-radius:4px;padding:5px 7px;font:11px 'SF Mono','Cascadia Code',Consolas,monospace;width:52px}
.custom input.wide{width:80px}
.custom .send-btn{padding:5px 12px;border-radius:4px;border:1px solid var(--accent);background:rgba(59,130,246,.1);color:var(--accent);font-family:inherit;font-size:11px;font-weight:500;cursor:pointer;transition:all .12s}
.custom .send-btn:hover{background:rgba(59,130,246,.2)}

/* Log terminal */
.log-section{background:var(--surface);border:1px solid var(--border);border-radius:10px;overflow:hidden}
.log-header{display:flex;align-items:center;justify-content:space-between;padding:10px 14px;border-bottom:1px solid var(--border)}
.log-header h3{font-size:10px;text-transform:uppercase;letter-spacing:1px;color:var(--text2);font-weight:500}
.log-header .count{font-size:10px;color:var(--dim);font-family:'SF Mono','Cascadia Code',Consolas,monospace}
#log{max-height:320px;overflow-y:auto;font-size:11px;font-family:'SF Mono','Cascadia Code',Consolas,monospace;line-height:1.5;padding:8px 0}
.log-entry{padding:4px 14px;border-bottom:1px solid rgba(255,255,255,.03)}
.log-entry:last-child{border-bottom:none}
.log-ts{color:var(--dim);font-size:10px}
.log-tx{color:var(--amber);font-weight:500}
.log-rx{color:var(--green);font-weight:500}
.log-err{color:var(--red)}
.log-info{color:var(--text2)}
.log-hex{color:var(--dim);font-size:10px;word-break:break-all;margin-top:1px}
.log-decode{margin-top:2px;padding:3px 6px;background:var(--bg);border-radius:3px;font-size:10px}
.log-decode .feat{color:var(--accent)}.log-decode .info{color:var(--text2)}.log-decode .ascii{color:var(--green)}.log-decode .err{color:var(--red)}

.empty{color:var(--dim);font-size:11px;padding:20px 0;text-align:center}

/* Scrollbar */
::-webkit-scrollbar{width:4px}
::-webkit-scrollbar-track{background:transparent}
::-webkit-scrollbar-thumb{background:var(--border);border-radius:2px}
::-webkit-scrollbar-thumb:hover{background:var(--dim)}

/* Disabled state */
button:disabled{opacity:.4;cursor:not-allowed}
</style></head><body>
<div class="app">
<div class="hdr">
  <div class="hdr-left">
    <h1>MOONDROP Pudding</h1>
    <span class="device">Pudding MD-TWS-056</span>
  </div>
  <div class="status-badge"><span class="dot" id="st"></span><span id="st-text">连接中...</span></div>
</div>

<div class="panels">
  <div class="panel anc">
    <div class="panel-header">
      <span class="panel-title">降噪控制</span>
      <span class="panel-status" id="anc-status">—</span>
    </div>
    <div class="mode-btns" id="anc-modes">
      <button class="mode-btn" data-val="00"><span class="label">关闭</span><span class="hex">0x00</span></button>
      <button class="mode-btn" data-val="02"><span class="label">通透</span><span class="hex">0x02</span></button>
      <button class="mode-btn" data-val="04"><span class="label">降噪</span><span class="hex">0x04</span></button>
    </div>
  </div>
  <div class="panel gain">
    <div class="panel-header">
      <span class="panel-title">增益控制</span>
      <span class="panel-status" id="gain-status">—</span>
    </div>
    <div class="mode-btns" id="gain-modes">
      <button class="mode-btn" data-val="00"><span class="label">高</span><span class="hex">0x00</span></button>
      <button class="mode-btn" data-val="01"><span class="label">中</span><span class="hex">0x01</span></button>
      <button class="mode-btn" data-val="02"><span class="label">低</span><span class="hex">0x02</span></button>
    </div>
  </div>
</div>

<div class="tools">
  <div class="tool-card">
    <h3>快捷命令</h3>
    <div class="presets" id="presets"></div>
  </div>
  <div class="tool-card">
    <h3>自定义命令</h3>
    <div class="custom">
      <div><label>Feature</label><input id="f" value="40"></div>
      <div><label>Cmd</label><input id="c" value="03"></div>
      <div><label>Payload</label><input id="p" class="wide" placeholder="hex"></div>
      <button class="send-btn" onclick="sendCustom()">发送</button>
    </div>
  </div>
</div>

<div class="log-section">
  <div class="log-header">
    <h3>通信日志</h3>
    <span class="count" id="log-count">0</span>
  </div>
  <div id="log"><div class="empty">等待连接...</div></div>
</div>
</div>

<script>
const log=document.getElementById('log'),st=document.getElementById('st'),stText=document.getElementById('st-text'),logCount=document.getElementById('log-count');
let logN=0;

function addLog(h){
  if(log.children===1&&log.firstElementChild.classList.contains('empty'))log.innerHTML='';
  const d=document.createElement('div');d.className='log-entry';d.innerHTML=h;log.prepend(d);
  while(log.children>50)log.removeChild(log.lastChild);
  logN++;logCount.textContent=logN;
}

function decodeInfo(d){if(!d)return'';let s=`<span class="feat">F:0x${d.feature.toString(16).padStart(2,'0')}</span> <span class="info">(${d.feature_name})</span> `;s+=`C:${d.cmd_hex}`;if(d.ascii)s+=` → "${d.ascii}"`;else if(d.payload.length)s+=` [${d.payload_hex}]`;return s}

function modeName(m){return{0:'关闭',2:'通透',4:'降噪',8:'自适应',16:'抗风噪'}[m]||`未知(0x${m.toString(16)})`}

function updateAncUI(mode){
  document.getElementById('anc-status').textContent=modeName(mode);
  document.querySelectorAll('#anc-modes .mode-btn').forEach(b=>b.classList.toggle('active',parseInt(b.dataset.val,16)===mode));
}

function gainName(m){return{0:'高',1:'中',2:'低'}[m]||`未知(0x${m})`}

function updateGainUI(level){
  document.getElementById('gain-status').textContent=gainName(level);
  document.querySelectorAll('#gain-modes .mode-btn').forEach(b=>b.classList.toggle('active',parseInt(b.dataset.val,16)===level));
}

async function send(f,c,p){
  document.querySelectorAll('.mode-btn,.preset,.send-btn').forEach(b=>b.disabled=true);
  st.className='dot';stText.textContent='通信中...';
  const r=await fetch(`/api/send?feature=${f}&cmd=${c}&payload=${encodeURIComponent(p||'')}`);
  const j=await r.json();
  const t=new Date().toLocaleTimeString('zh',{hour12:false,hour:'2-digit',minute:'2-digit',second:'2-digit'});
  if(j.error&&!j.rx_raw){addLog(`<span class="log-ts">${t}</span> <span class="log-err">✗ ${j.error}</span>`);st.className='dot off';stText.textContent='通信失败'}
  else{
    let h=`<span class="log-ts">${t}</span> <span class="log-tx">TX</span> <span class="log-hex">${j.tx?j.tx.raw:''}</span><div class="log-decode">${decodeInfo(j.tx)}</div>`;
    if(j.decoded){h+=`<span class="log-rx">RX</span> <span class="log-hex">${j.rx_raw}</span><div class="log-decode">${decodeInfo(j.decoded)}</div>`;
      if((j.decoded.feature&0xFE)===0x40&&(j.decoded.cmd===3||j.decoded.cmd===4)){updateAncUI(j.decoded.payload[0])}
      if((j.decoded.feature&0xFE)===0x1E&&(j.decoded.cmd===1||j.decoded.cmd===2)){updateGainUI(j.decoded.payload[0])}
    }else if(j.rx_raw){h+=`<span class="log-rx">RX</span> <span class="log-hex">${j.rx_raw}</span>`}
    else{h+=`<span class="log-info">设备无响应</span>`}
    addLog(h);st.className='dot ok';stText.textContent='已连接';
  }
  await new Promise(r=>setTimeout(r,1200));
  document.querySelectorAll('.mode-btn,.preset,.send-btn').forEach(b=>b.disabled=false);
}

function sendCustom(){const f=parseInt(document.getElementById('f').value,16);const c=parseInt(document.getElementById('c').value,16);send(f,c,document.getElementById('p').value)}

document.getElementById('anc-modes').addEventListener('click',e=>{const b=e.target.closest('.mode-btn');if(b){send(0x40,0x04,b.dataset.val);updateAncUI(parseInt(b.dataset.val,16))}});
document.getElementById('gain-modes').addEventListener('click',e=>{const b=e.target.closest('.mode-btn');if(b){send(0x1E,0x02,b.dataset.val);updateGainUI(parseInt(b.dataset.val,16))}});

fetch('/api/presets').then(r=>r.json()).then(ps=>{const g=document.getElementById('presets');ps.forEach(p=>{const b=document.createElement('button');b.className='preset';b.textContent=p.name;b.title=`F:0x${p.feature.toString(16).padStart(2,'0')} C:0x${p.cmd.toString(16).padStart(2,'0')}`;b.onclick=()=>send(p.feature,p.cmd,p.payload);g.appendChild(b)})});

document.querySelectorAll('.mode-btn,.preset,.send-btn').forEach(b=>b.disabled=true);
send(0x40,0x03);
send(0x1E,0x01);

fetch('/api/status').then(r=>r.json()).then(j=>{st.className=j.connected?'dot ok':'dot off';stText.textContent=j.connected?'已连接':'未连接'});
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
