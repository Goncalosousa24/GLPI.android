import urllib.request
import json

url = "http://localhost:8080/apirest.php/Computer"
headers = {
    "App-Token": "Kv6GgUHREqU0e35dKamiQSh5vjUYenPrqMItEeIh",
    "Session-Token": "9qi3dk783f483o8nihin95te95",
    "Host": "glpi.ad.cm-vilaverde.pt",
    "Content-Type": "application/json"
}
payload = {
    "input": {
        "name": "computadorteste123",
        "comment": "Adicionado via GLPI Mobile App",
        "entities_id": "0",
        "states_id": "2",
        "users_id_tech": "357",
        "users_id": "357"
    }
}

req = urllib.request.Request(url, data=json.dumps(payload).encode('utf-8'), headers=headers, method='POST')
try:
    with urllib.request.urlopen(req) as r:
        print("Status Code:", r.status)
        print("Response:", r.read().decode('utf-8'))
except urllib.error.HTTPError as e:
    print("HTTP Error:", e.code)
    print("Response:", e.read().decode('utf-8'))
except Exception as e:
    print("Error:", e)
