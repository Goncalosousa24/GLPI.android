import requests

url = "http://localhost:8080/apirest.php/search/Ticket"
headers = {
    "App-Token": "Kv6GgUHREqU0e35dKamiQSh5vjUYenPrqMItEeIh",
    "Session-Token": "oefkvv50equgq8b7i1pcou1mbb",
    "Host": "glpi.ad.cm-vilaverde.pt"
}

# Let's try fields that we know are links. And also we can just check if any ticket has ID 2556 to see its links
r = requests.get("http://localhost:8080/apirest.php/Ticket/2556/Item_Ticket", headers=headers)
print("Item_Ticket for 2556:", r.json())
