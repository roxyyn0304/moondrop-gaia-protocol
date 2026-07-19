"""MOONDROP GAIA V3 蓝牙测试 Web UI。"""
import sys, json, time, threading
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
import serial

VENDOR_ID = 0x001D
FEATURE_NAMES = {0x00:"基础查询",0x0A:"EQ",0x1A:"设备管理",0x1E:"增益",0x20:"编解码器",0x28:"设备信息",0x2C:"电池",0x40:"ANC"}
ANC_MODES = {0x00:"关闭",0x01:"自适应",0x02:"通透",0x03:"抗风噪"}

def feature_name(f):
    return FEATURE_NAMES.get(f&0xFE, "") or f"0x{f:02X}"

def parse_battery(payload):
    """解析电量 [ID:1][level:1]... 对, 来自 F=0x1B 响应, 0xFF=不可用"""
    if len(payload) < 2 or len(payload) % 2 != 0:
        return None
    names = {1: "左耳", 2: "右耳", 3: "充电盒"}
    bats = []
    for i in range(0, len(payload), 2):
        bats.append({"id": payload[i], "name": names.get(payload[i], f"设备{payload[i]}"), "level": payload[i+1]})
    return bats

def build_tx(fid, cid, payload=b"", seq=0):
    body_len = len(payload)  # payload size only (matches btsnoop capture)
    pkt = bytearray(8 + len(payload))
    pkt[0]=0xFF; pkt[1]=0x04; pkt[2]=((body_len>>8)&0xFF); pkt[3]=(body_len&0xFF)
    pkt[4]=seq; pkt[5]=VENDOR_ID&0xFF; pkt[6]=fid&0xFF; pkt[7]=cid&0xFF
    pkt[8:]=payload
    return bytes(pkt)

def decode_packet(data):
    if len(data)<8 or data[0]!=0xFF or data[1]!=0x04: return None
    if (data[5]&0xFF)!=VENDOR_ID: return None
    f=data[6]&0xFF; c=data[7]&0xFF; p=list(data[8:]) if len(data)>8 else []
    ascii_str=""
    if p:
        chars=[chr(b) if 32<=b<127 else None for b in p]
        if sum(1 for ch in chars if ch)>=len(p)*0.6: ascii_str="".join(ch for ch in chars if ch)
    result = {"feature":f,"feature_name":feature_name(f),"cmd":c,"cmd_hex":f"0x{c:02X}","payload":p,"payload_hex":" ".join(f"{b:02X}" for b in p),"ascii":ascii_str,"raw":data.hex(" ").upper()}
    if (f & 0xFE) == 0x1A and len(p) >= 2 and len(p) % 2 == 0:
        battery = parse_battery(p)
        if battery: result["battery"] = battery
    return result

ser=None; ser_lock=threading.Lock()

def send_cmd(feature, cmd, payload_hex="", timeout=2.0, retries=2):
    payload=bytes.fromhex(payload_hex.replace(" ","")) if payload_hex else b""
    tx=build_tx(feature, cmd, payload)
    last_rx=None
    for attempt in range(retries+1):
        with ser_lock:
            if not ser or not ser.is_open: return {"tx":decode_packet(tx),"rx_raw":None,"error":"串口未连接"}
            ser.reset_input_buffer(); ser.write(tx); ser.flush()
            deadline=time.time()+timeout; rx=b""
            while time.time()<deadline:
                if ser.in_waiting>0:
                    rx+=ser.read(ser.in_waiting)
                    if len(rx)>=8 and rx[0]==0xFF and rx[1]==0x04:
                        body_len=(rx[2]<<8)|rx[3]
                        total=8+body_len
                        if len(rx)>=total:
                            rx=rx[:total]
                            break
                time.sleep(0.02)
        last_rx=rx
        if rx: break
        if attempt<retries: time.sleep(0.1)
    if not last_rx: return {"tx":decode_packet(tx),"rx_raw":None,"error":"无响应"}
    return {"tx":decode_packet(tx),"rx_raw":last_rx.hex(" ").upper(),"decoded":decode_packet(last_rx)}

PRESETS=[
    {"name":"固件版本","feature":0x00,"cmd":0x05,"payload":""},
    {"name":"序列号","feature":0x00,"cmd":0x14,"payload":""},
    {"name":"设备ID","feature":0x00,"cmd":0x15,"payload":""},
    {"name":"EQ 状态","feature":0x00,"cmd":0x07,"payload":""},
    {"name":"设备状态","feature":0x00,"cmd":0x0D,"payload":"0700000004"},
    {"name":"ANC 可用模式","feature":0x40,"cmd":0x29,"payload":""},
    {"name":"LDAC 状态","feature":0x20,"cmd":0x05,"payload":""},
    {"name":"LC3 状态","feature":0x20,"cmd":0x01,"payload":""},
    {"name":"Gain 查询","feature":0x1E,"cmd":0x01,"payload":""},
    {"name":"连接设备名","feature":0x28,"cmd":0x05,"payload":""},
]

HTML=r"""<!DOCTYPE html><html lang="zh"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>MOONDROP Pudding Controller</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
:root{--bg:#0a0c10;--surface:#12141a;--surface2:#181b24;--border:#1e2230;--text:#e2e8f0;--text2:#6b7280;--dim:#374151;--anc:#2b7be4;--gain:#e68a2e;--bat:#22a06b;--tx:#e68a2e;--rx:#22a06b;--err:#ef4444}
body{font-family:system-ui,-apple-system,'Segoe UI',sans-serif;background:var(--bg);color:var(--text);padding:12px;font-size:13px;line-height:1.5;min-height:100vh}
.app{max-width:960px;margin:0 auto}
.hdr{display:flex;align-items:center;justify-content:space-between;padding:8px 0 12px;border-bottom:1px solid var(--border);margin-bottom:12px}
.hdr h1{font-size:13px;font-weight:600;letter-spacing:.3px}
.hdr .device{font-size:11px;color:var(--text2);font-family:'SF Mono','Cascadia Code',Consolas,monospace}
.st{display:flex;align-items:center;gap:6px;font-size:11px;color:var(--text2);font-family:'SF Mono',Consolas,monospace}
.st-d{width:6px;height:6px;border-radius:50%;background:var(--dim)}
.st-d.ok{background:var(--rx);box-shadow:0 0 6px rgba(34,160,107,.4)}
.st-d.off{background:var(--dim)}
.grid{display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px;margin-bottom:8px}
@media(max-width:700px){.grid{grid-template-columns:1fr}}
.panel{background:var(--surface);border:1px solid var(--border);border-radius:8px;overflow:hidden}
.ph{display:flex;align-items:center;justify-content:space-between;padding:10px 12px 8px;font-size:10px;text-transform:uppercase;letter-spacing:1px;font-weight:600}
.pv{font-family:'SF Mono',Consolas,monospace;font-size:13px;font-weight:600;padding:1px 6px;border-radius:3px;background:var(--bg)}
.pb{padding:0 12px 10px}
.panel.anc{border-top:2px solid var(--anc)}.panel.anc .ph{color:var(--anc)}
.panel.gain{border-top:2px solid var(--gain)}.panel.gain .ph{color:var(--gain)}
.panel.bat{border-top:2px solid var(--bat)}.panel.bat .ph{color:var(--bat)}
.m{display:flex;gap:4px}
.mb{flex:1;padding:7px 4px;border-radius:5px;border:1px solid var(--border);background:var(--bg);color:var(--text2);font-family:inherit;font-size:11px;font-weight:500;cursor:pointer;transition:all .12s;text-align:center;line-height:1.2}
.mb:hover{border-color:var(--dim);color:var(--text);background:var(--surface2)}
.mb.a{border-color:currentColor;background:rgba(255,255,255,.03)}
.panel.anc .mb.a{color:var(--anc);border-color:var(--anc);box-shadow:0 0 8px rgba(43,123,228,.12)}
.panel.gain .mb.a{color:var(--gain);border-color:var(--gain);box-shadow:0 0 8px rgba(230,138,46,.12)}
.mb .s{display:block;font-family:'SF Mono',Consolas,monospace;font-size:8px;color:var(--dim);margin-top:1px}
.ns{margin-top:8px;padding-top:8px;border-top:1px solid var(--border)}
.ns .mb{font-size:10px;padding:6px 4px}
.bi{display:flex;align-items:center;gap:5px;padding:2px 0}
.bl{font-size:10px;color:var(--text2);min-width:2.8em}
.bb{flex:1;height:6px;background:var(--bg);border-radius:3px;overflow:hidden}
.bf{display:block;height:100%;border-radius:3px;background:var(--bat);transition:width .25s}
.bp{font-size:10px;font-family:'SF Mono',Consolas,monospace;color:var(--text2);min-width:2.8em;text-align:right}
.br{color:var(--text2);cursor:pointer;font-size:11px;padding:0 4px}
.br:hover{color:var(--text)}
.tb{display:flex;align-items:center;gap:8px;padding:8px 12px;background:var(--surface);border:1px solid var(--border);border-radius:8px;margin-bottom:8px;flex-wrap:wrap}
.tl{font-size:9px;text-transform:uppercase;letter-spacing:.5px;color:var(--dim);font-weight:600}
.sep{width:1px;height:18px;background:var(--border)}
.pr{padding:3px 8px;border-radius:4px;border:1px solid var(--border);background:var(--bg);color:var(--text2);font-family:inherit;font-size:10px;cursor:pointer}
.pr:hover{border-color:var(--anc);color:var(--text);background:var(--surface2)}
.cf{display:inline-flex;align-items:center;gap:3px;flex-shrink:0}
.cf label{font-size:8px;text-transform:uppercase;letter-spacing:.5px;color:var(--dim)}
.cf input{width:40px;padding:3px 4px;border-radius:3px;border:1px solid var(--border);background:var(--bg);color:var(--text);font:10px 'SF Mono',Consolas,monospace}
.cf input.w{width:72px}
.sb{padding:3px 10px;border-radius:4px;border:1px solid var(--anc);background:rgba(43,123,228,.1);color:var(--anc);font-family:inherit;font-size:10px;font-weight:500;cursor:pointer}
.sb:hover{background:rgba(43,123,228,.2)}
.ls{background:var(--surface);border:1px solid var(--border);border-radius:8px;overflow:hidden}
.lh{display:flex;align-items:center;justify-content:space-between;padding:8px 12px;border-bottom:1px solid var(--border)}
.lh h3{font-size:10px;text-transform:uppercase;letter-spacing:1px;color:var(--dim);font-weight:500}
.lh .c{font-size:10px;color:var(--dim);font-family:'SF Mono',Consolas,monospace}
#lg{max-height:320px;overflow-y:auto;font-size:11px;font-family:'SF Mono','Cascadia Code',Consolas,monospace;line-height:1.5;padding:6px 10px}
.le{margin-bottom:6px;border-radius:5px;background:var(--surface2);border-left:3px solid var(--border);overflow:hidden}
.le:last-child{margin-bottom:0}
.le.tx{border-left-color:var(--tx)}.le.rx{border-left-color:var(--rx)}.le.err{border-left-color:var(--err);background:rgba(239,68,68,.04)}
.lr{display:flex;align-items:baseline;gap:8px;padding:4px 8px}
.ld{font-size:9px;font-weight:700;text-transform:uppercase;letter-spacing:.5px;min-width:18px}
.le.tx .ld{color:var(--tx)}.le.rx .ld{color:var(--rx)}.le.err .ld{color:var(--err)}
.lhx{color:var(--text2);word-break:break-all;font-size:10px}
.lt{color:var(--dim);font-size:8px;margin-left:auto;white-space:nowrap}
.ldc{padding:2px 8px 4px 29px;font-size:10px;color:var(--text2)}
.ldc .f{color:var(--anc);font-weight:600}
.ldc .i{color:var(--dim)}.ldc .t{color:var(--bat)}.ldc .e{color:var(--err)}
.em{color:var(--dim);font-size:11px;padding:20px 0;text-align:center}
button:disabled{opacity:.4;cursor:not-allowed}
::-webkit-scrollbar{width:3px}
::-webkit-scrollbar-track{background:transparent}
::-webkit-scrollbar-thumb{background:var(--border);border-radius:2px}
</style></head><body>
<div class="app">

<div class="hdr">
  <div><h1>MOONDROP Pudding</h1><span class="device">Pudding MD-TWS-056</span></div>
  <div class="st"><span class="st-d" id="std"></span><span id="stt">连接中...</span></div>
</div>

<div class="grid">

<div class="panel anc">
  <div class="ph"><span>降噪控制</span><span class="pv" id="ancv">—</span></div>
  <div class="pb">
    <div class="m" id="anct">
      <button class="mb" data-v="00"><span>关闭</span><span class="s">0x00</span></button>
      <button class="mb" data-v="02"><span>通透</span><span class="s">0x02</span></button>
      <button class="mb" data-v="04" id="nct"><span>降噪</span><span class="s" id="nca">▸</span></button>
    </div>
    <div class="ns" id="ncs" style="display:none">
      <div class="m">
        <button class="mb" data-v="01"><span>自适应</span><span class="s">0x01</span></button>
        <button class="mb" data-v="03"><span>抗风噪</span><span class="s">0x03</span></button>
      </div>
    </div>
  </div>
</div>

<div class="panel gain">
  <div class="ph"><span>增益控制</span><span class="pv" id="gnv">—</span></div>
  <div class="pb">
    <div class="m" id="gnt">
      <button class="mb" data-v="00"><span>高</span><span class="s">0x00</span></button>
      <button class="mb" data-v="01"><span>中</span><span class="s">0x01</span></button>
      <button class="mb" data-v="02"><span>低</span><span class="s">0x02</span></button>
    </div>
  </div>
</div>

<div class="panel bat">
  <div class="ph"><span>电量</span><span class="br" id="batbtn">↻</span></div>
  <div class="pb">
    <div class="bi"><span class="bl">左耳</span><span class="bb"><span class="bf" id="batl" style="width:0%"></span></span><span class="bp" id="batlt">--</span></div>
    <div class="bi"><span class="bl">右耳</span><span class="bb"><span class="bf" id="batr" style="width:0%"></span></span><span class="bp" id="batrt">--</span></div>
    <div class="bi"><span class="bl">充电盒</span><span class="bb"><span class="bf" id="batc" style="width:0%"></span></span><span class="bp" id="batct">--</span></div>
  </div>
</div>

</div>

<div class="tb">
  <span class="tl">快捷</span>
  <div id="prs" style="display:inline-flex;gap:4px;flex-wrap:wrap"></div>
  <span class="sep"></span>
  <div style="display:flex;gap:3px;flex-shrink:0;align-items:center">
    <div class="cf"><label>F</label><input id="fi" value="40"></div>
    <div class="cf"><label>C</label><input id="ci" value="03"></div>
    <div class="cf"><label>Payload</label><input id="pi" class="w" placeholder="hex"></div>
    <button class="sb" onclick="sc()">发送</button>
  </div>
</div>

<div class="ls">
  <div class="lh"><h3>通信日志</h3><span class="c" id="lgc">0</span></div>
  <div id="lg"><div class="em">等待连接...</div></div>
</div>

</div>

<script>
const lg=document.getElementById('lg'),std=document.getElementById('std'),stt=document.getElementById('stt'),lgc=document.getElementById('lgc');let ln=0;
function al(h,c){if(lg.children===1&&lg.firstElementChild.classList.contains('em'))lg.innerHTML='';const d=document.createElement('div');d.className='le'+(c?' '+c:'');d.innerHTML=h;lg.prepend(d);while(lg.children>50)lg.removeChild(lg.lastChild);ln++;lgc.textContent=ln}
function mn(m){return{0:'关闭',1:'自适应',2:'通透',3:'抗风噪',4:'降噪'}[m]||('未知(0x'+m.toString(16)+')')}
function gn(m){return{0:'高',1:'中',2:'低'}[m]||('未知(0x'+m+')')}
function di(d,s){if(!d)return'';let r='<span class="f">F:0x'+d.feature.toString(16).padStart(2,'0')+'</span> <span class="i">('+d.feature_name+')</span> C:'+d.cmd_hex;if(d.ascii)r+=' <span class="t">→ "'+d.ascii+'"</span>';else if(d.payload.length)r+=' <span class="i">['+d.payload_hex+']</span>';if(s){const t=ps(d);if(t)r+='<br><span class="t">→ '+t+'</span>'}return r}
function ps(d){const p=d.payload,f=d.feature&0xFE,c=d.cmd;if(!p||!p.length)return'';if(f===0x40&&(c===3||c===4))return'ANC: '+mn(p[0]);if(f===0x1E&&(c===1||c===2))return'增益: '+gn(p[0]);if(f===0x20&&c===5)return'LDAC: '+(p[0]?'启用':'关闭');if(f===0x20&&c===1)return'LC3: '+(p[0]?'启用':'关闭');if(f===0x1A&&c<=1){const n={1:'左耳',2:'右耳',3:'充电盒'};const b=i=>i===0xFF?'无':i+'%';return p.length>=4?'电量: '+n[p[0]]+' '+b(p[1])+' '+n[p[2]]+' '+b(p[3])+(p[4]?' '+n[p[4]]+' '+b(p[5]):''):''}if(f===0x00&&c===0x0D)return'设备状态';if(f===0x00&&c===0x0C)return'配置查询 #'+p[0];if(f===0x00&&c===0x01)return'支持 '+p.length+' 条命令';if(f===0x00&&c===0x07)return'EQ 状态';if(f===0x28&&c===5){const n=d.ascii||'';return n?'连接手机: '+n:'设备信息'}if(f===0x28&&c===3)return'子类型: '+p[0];if(f===0x40&&c===0x29){const a=['关闭','自适应','通透','抗风噪','降噪'];return'可用: '+a.map((n,i)=>(p[i]?n:'无')).join(' ')}return''}
function lr(d,h,dc,cl){return'<div class="lr"><span class="ld">'+d+'</span><span class="lhx">'+h+'</span><span class="lt">'+new Date().toLocaleTimeString('zh',{hour12:false,hour:'2-digit',minute:'2-digit',second:'2-digit'})+'</span></div>'+(dc?'<div class="ldc">'+dc+'</div>':'')}

function ua(m){const n=m===1||m===3||m===4;document.getElementById('ancv').textContent=mn(m);document.querySelectorAll('#anct .mb').forEach(b=>b.classList.toggle('a',(b.dataset.v==='04'&&n)||parseInt(b.dataset.v,16)===m));const s=document.getElementById('ncs'),a=document.getElementById('nca');if(n){s.style.display='block';a.textContent='▾';document.querySelectorAll('#ncs .mb').forEach(b=>b.classList.toggle('a',parseInt(b.dataset.v,16)===m))}else{s.style.display='none';a.textContent='▸'}}
function ug(l){document.getElementById('gnv').textContent=gn(l);document.querySelectorAll('#gnt .mb').forEach(b=>b.classList.toggle('a',parseInt(b.dataset.v,16)===l))}
function ub(b){const m={1:'batl',2:'batr',3:'batc'};b.forEach(b=>{const i=m[b.id];if(!i)return;if(b.level===255){document.getElementById(i).style.width='0%';document.getElementById(i+'t').textContent='无'}else{const p=Math.min(b.level,100);document.getElementById(i).style.width=p+'%';document.getElementById(i+'t').textContent=p+'%'}})}

async function send(f,c,p){
  document.querySelectorAll('.mb,.pr,.sb').forEach(b=>b.disabled=true);std.className='st-d';stt.textContent='通信中...';
  try{
    const r=await fetch('/api/send?feature='+f+'&cmd='+c+'&payload='+encodeURIComponent(p||''));
    const j=await r.json();
    if(j.error&&!j.rx_raw){al(lr('ERR',j.tx?j.tx.raw:'','<span class="e">'+j.error+'</span>'),'err');std.className='st-d off';stt.textContent='通信失败'}
    else{
      let h=lr('TX',j.tx?j.tx.raw:'',di(j.tx));
      if(j.decoded){h+=lr('RX',j.rx_raw,di(j.decoded,true));if(j.decoded.payload.length>0&&(j.decoded.feature&0xFE)===0x40&&(j.decoded.cmd===3||j.decoded.cmd===4))ua(j.decoded.payload[0]);if(j.decoded.payload.length>0&&(j.decoded.feature&0xFE)===0x1E&&(j.decoded.cmd===1||j.decoded.cmd===2))ug(j.decoded.payload[0]);if(j.decoded.battery)ub(j.decoded.battery)}
      else if(j.rx_raw)h+=lr('RX',j.rx_raw,'');
      else h+='<div class="lr"><span class="ld" style="color:var(--err)">--</span><span style="color:var(--err);padding:4px 8px">设备无响应</span></div>';
      al(h);std.className='st-d ok';stt.textContent='已连接';
    }
  }catch(e){al(lr('ERR','','<span class="e">请求失败: '+e.message+'</span>'),'err');std.className='st-d off';stt.textContent='连接失败'}
  await new Promise(r=>setTimeout(r,2000));document.querySelectorAll('.mb,.pr,.sb').forEach(b=>b.disabled=false);
}
function sc(){try{send(parseInt(document.getElementById('fi').value,16),parseInt(document.getElementById('ci').value,16),document.getElementById('pi').value)}catch(e){alert('参数错误: '+e.message)}}

document.getElementById('anct').addEventListener('click',e=>{const b=e.target.closest('.mb');if(!b)return;const v=parseInt(b.dataset.v,16);if(v===4){const s=document.getElementById('ncs'),a=document.getElementById('nca');const o=s.style.display!=='block';s.style.display=o?'block':'none';a.textContent=o?'▾':'▸';return}send(0x40,0x04,b.dataset.v);ua(v)});
document.getElementById('ncs').addEventListener('click',e=>{const b=e.target.closest('.mb');if(!b)return;send(0x40,0x04,b.dataset.v);ua(parseInt(b.dataset.v,16))});
document.getElementById('gnt').addEventListener('click',e=>{const b=e.target.closest('.mb');if(b){send(0x1E,0x02,b.dataset.v);ug(parseInt(b.dataset.v,16))}});
document.getElementById('batbtn').addEventListener('click',()=>send(0x1A,0x01,'0102'));

fetch('/api/presets').then(r=>r.json()).then(ps=>{const g=document.getElementById('prs');ps.forEach(p=>{const b=document.createElement('button');b.className='pr';b.textContent=p.name;b.title='F:0x'+p.feature.toString(16).padStart(2,'0')+' C:0x'+p.cmd.toString(16).padStart(2,'0');b.onclick=()=>send(p.feature,p.cmd,p.payload);g.appendChild(b)})}).catch(e=>console.error('加载预设失败:',e));

document.querySelectorAll('.mb,.pr,.sb').forEach(b=>b.disabled=true);

(async()=>{stt.textContent='探测设备...';const ck=await fetch('/api/check').then(r=>r.json()).catch(()=>({online:false}));if(ck.online){std.className='st-d ok';stt.textContent='已连接';await send(0x40,0x03);await send(0x1E,0x01);await send(0x1A,0x01,'0102')}else{std.className='st-d off';stt.textContent='设备未响应';al('<span class="lt">'+new Date().toLocaleTimeString('zh',{hour12:false,hour:'2-digit',minute:'2-digit',second:'2-digit'})+'</span> <span class="e">✗ 设备未响应 — 请确认蓝牙已配对且 SPP 已连接，然后刷新页面重试</span>');document.querySelectorAll('.mb,.pr,.sb').forEach(b=>b.disabled=false)}})();
</script></body></html>"""

device_online=False

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        global device_online
        p=urlparse(self.path); q=parse_qs(p.query)
        if p.path in("/","/index.html"):
            self.send_response(200);self.send_header("Content-Type","text/html; charset=utf-8");self.end_headers();self.wfile.write(HTML.encode())
        elif p.path=="/api/status":self._json({"connected":ser is not None and ser.is_open,"online":device_online,"port":ser.port if ser else None})
        elif p.path=="/api/check":
            r=send_cmd(0x00, 0x05, "")
            device_online=r.get("decoded") is not None
            self._json({"online":device_online,"result":r})
        elif p.path=="/api/presets":self._json(PRESETS)
        elif p.path=="/api/send":
            try:
                f=int(q.get("feature",["0"])[0]); c=int(q.get("cmd",["0"])[0])
                self._json(send_cmd(f,c,q.get("payload",[""])[0]))
            except (ValueError,TypeError):self._json({"error":"参数格式错误"})
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
    global ser;port,web_port=None,8080
    args=sys.argv[1:]
    i=0
    while i<len(args):
        if args[i]=="--port" and i+1<len(args):
            if args[i+1].upper().startswith("COM"):port=args[i+1]
            i+=2
        elif args[i]=="--web-port" and i+1<len(args):
            try:web_port=int(args[i+1])
            except:pass
            i+=2
        elif args[i].upper().startswith("COM"):port=args[i];i+=1
        else:i+=1
    if not port:
        port=find_moondrop_port()
    if not port:
        print("  未找到 MOONDROP 蓝牙串口");print("  用法: python webtest.py [--port COMx] [--web-port 8080]");return
    print(f"  串口: {port}")
    try:ser=serial.Serial(port,115200,timeout=0.5,write_timeout=2);print(f"  {port} OK")
    except Exception as e:print(f"  {port} failed: {e}");return
    server=HTTPServer(("127.0.0.1",web_port),Handler)
    print(f"  http://127.0.0.1:{web_port}\n")
    try:server.serve_forever()
    except KeyboardInterrupt:pass
    finally:
        if ser and ser.is_open:ser.close()
        server.server_close()

if __name__=="__main__":main()
