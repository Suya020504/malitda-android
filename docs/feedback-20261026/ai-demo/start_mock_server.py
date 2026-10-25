"""Offline-only R4 protocol demonstrator. No external service or API key is used."""
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
import base64, json, pathlib, re, time

ROOT = pathlib.Path(__file__).resolve().parent
IMAGE = ROOT / 'water.png'
(ROOT/'logs').mkdir(exist_ok=True)
STYLES = ['storybook','photo','simple','watercolor','colored-pencil','clay','paper-cut','pixel','comic','line-art']
PROFILES = {}
TOKEN = 'local-mock-demo-only'

class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        pass

    def record(self, kind, **detail):
        with (ROOT/'logs/mock-requests.jsonl').open('a',encoding='utf-8') as f:
            f.write(json.dumps({'time':time.time(),'kind':kind,'mode':'offline-mock','paid_api_calls':0,**detail},ensure_ascii=False)+'\n')

    def send(self, code, body):
        encoded=json.dumps(body,ensure_ascii=False).encode('utf-8')
        self.send_response(code); self.send_header('Content-Type','application/json'); self.send_header('Content-Length',str(len(encoded))); self.end_headers()
        try: self.wfile.write(encoded)
        except (BrokenPipeError, ConnectionResetError): self.record('client_disconnected')

    def do_GET(self):
        if self.path == '/health': return self.send(200,{'mode':'live','demo_mode':'offline-mock','ok':True})
        if self.path == '/api/config': return self.send(200,{'mode':'live','demo_mode':'offline-mock','ready':True,'session_token':TOKEN,'notice':None})
        if self.path == '/api/styles':
            return self.send(200,{'styles':[{'id':s,'preview_url':'/styles/'+s+'.png'} for s in STYLES]})
        if re.fullmatch(r'/styles/[a-z-]+\.png',self.path):
            body=IMAGE.read_bytes(); self.send_response(200); self.send_header('Content-Type','image/png'); self.send_header('Content-Length',str(len(body))); self.end_headers(); return self.wfile.write(body)
        if re.fullmatch(r'/api/profiles/[^/]+/style',self.path):
            key=self.path.split('/')[3]
            return self.send(200,PROFILES[key]) if key in PROFILES else self.send(404,{'detail':'No style yet'})
        return self.send(404,{'detail':'Unknown route'})

    def do_POST(self):
        if self.headers.get('Origin') != 'http://127.0.0.1:8769' or self.headers.get('X-Demo-Token') != TOKEN:
            return self.send(403,{'detail':'Local protocol token required'})
        length=int(self.headers.get('Content-Length','0'))
        if not 0 < length <= 8192: return self.send(413,{'detail':'Request too large'})
        try: payload=json.loads(self.rfile.read(length))
        except (ValueError,UnicodeDecodeError): return self.send(400,{'detail':'Invalid JSON'})
        if re.fullmatch(r'/api/profiles/[^/]+/style',self.path):
            key=self.path.split('/')[3]; old=PROFILES.get(key,{'revision':0})
            if payload.get('expected_revision') != old['revision']: return self.send(409,{'detail':'Revision mismatch'})
            if payload.get('style_id') not in STYLES: return self.send(422,{'detail':'Unknown style'})
            PROFILES[key]={'style_id':payload['style_id'],'revision':old['revision']+1}
            self.record('style_saved',style_id=payload['style_id'],revision=PROFILES[key]['revision'])
            return self.send(200,PROFILES[key])
        if self.path == '/api/generate':
            key=payload.get('profile_id'); profile=PROFILES.get(key)
            if not payload.get('consent') or not payload.get('meaning') or not profile: return self.send(422,{'detail':'Missing generation requirements'})
            if payload.get('profile_revision') != profile['revision']: return self.send(409,{'detail':'Revision mismatch'})
            self.record('generation_started',consent=payload['consent'],style_id=profile['style_id'],profile_revision=profile['revision'])
            time.sleep(14)
            data='data:image/png;base64,'+base64.b64encode(IMAGE.read_bytes()).decode('ascii')
            self.record('generation_complete',returned_existing_asset=IMAGE.name,external_provider_called=False)
            return self.send(200,{'data_url':data,'mode':'offline-mock'})
        return self.send(404,{'detail':'Unknown route'})

if __name__=='__main__':
    print('Offline-only R4 mock listening at 127.0.0.1:8769; existing water asset; zero paid calls',flush=True)
    ThreadingHTTPServer(('127.0.0.1',8769),Handler).serve_forever()
