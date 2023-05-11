import paramiko
import os
import json

SSH_PASSWORD = os.environ['SSH_PASSWORD']
SSH_IP = os.environ['SSH_IP']

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

ssh.connect(SSH_IP, 22, 'root', SSH_PASSWORD)

print(f'Connect to {SSH_IP} OK')

# upload Toeic.war
ftp = ssh.open_sftp()

try:
    ftp.mkdir('/root/toeic-data')
except: pass

files = os.listdir()
files = [f for f in files if f.endswith('.zip')]

for file in files:
    ftp.put(file, '/root/toeic-data/' + file)
    print("Ok", file)

ftp.close()

print("OK")

