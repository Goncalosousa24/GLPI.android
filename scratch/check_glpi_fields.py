import requests

url = "https://glpi.ad.cm-vilaverde.pt/apirest.php/listSearchOptions/Ticket"
headers = {
    "App-Token": "Kv6GgUHREqU0e35dKamiQSh5vjUYenPrqMItEeIh",
    "Session-Token": "oefkvv50equgq8b7i1pcou1mbb"
}

try:
    r = requests.get(url, headers=headers, verify=False)
    if r.status_code == 200:
        data = r.json()
        print("--- SEARCH OPTIONS FOR TICKET ---")
        for key, value in data.items():
            name = value.get("name", "")
            if "source" in name.lower() or "fonte" in name.lower():
                print(f"ID: {key} -> Name: {name}")
    else:
        print(f"Error: {r.status_code}")
        print(r.text)
except Exception as e:
    print(f"Exception: {e}")
