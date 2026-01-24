import socket
import json
import time

HOST = '127.0.0.1'
PORT = 5000

def send(sock, t, payload):
    msg = {'type': t, 'payload': payload}
    s = json.dumps(msg) + '\n'
    sock.sendall(s.encode('utf-8'))

def recv_line(sock):
    data = b''
    while True:
        ch = sock.recv(1)
        if not ch:
            return None
        if ch == b'\n':
            break
        data += ch
    return data.decode('utf-8')

with socket.create_connection((HOST, PORT), timeout=5) as s:
    print('Connected')
    send(s, 'login', {'id':'1001','password':'parola1'})
    print('Sent login')
    # read responses until login_response
    for _ in range(10):
        line = recv_line(s)
        if not line:
            break
        print('RECV:', line)
        jo = json.loads(line)
        if jo.get('type') == 'login_response':
            print('Login response received')
            break
    time.sleep(0.5)
    send(s, 'start_work', {})
    print('Sent start_work')
    line = recv_line(s)
    print('RECV:', line)
    time.sleep(0.5)
    send(s, 'get_work_status', {})
    line = recv_line(s)
    print('RECV:', line)
    time.sleep(0.5)
    send(s, 'end_work', {})
    line = recv_line(s)
    print('RECV:', line)
    send(s, 'get_work_status', {})
    line = recv_line(s)
    print('RECV:', line)

print('Done')
