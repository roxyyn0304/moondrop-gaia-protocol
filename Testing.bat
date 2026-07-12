@echo off
start http://127.0.0.1:8080
python tools\webtest.py COM3 --port 8080
pause
