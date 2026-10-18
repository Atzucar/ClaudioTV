import requests
from bs4 import BeautifulSoup
import json
import re
import time

BASE_URL = "https://livetv905.me" 
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
    "Accept-Language": "es-ES,es;q=0.9"
}

def clean_text(text):
    return " ".join(text.split())

def main():
    print("Iniciando raspado de LiveTV.sx...")
    try:
        response = requests.get(f"{BASE_URL}/es/allupcoming/", headers=HEADERS, timeout=15)
    except Exception as e:
        print(f"Error de conexión con el servidor espejo: {e}")
        return

    if response.status_code != 200:
        print(f"Error de servidor. Código de estado: {response.status_code}")
        return

    soup = BeautifulSoup(response.text, 'html.parser')
    event_elements = soup.find_all('a', href=re.compile(r'/es/eventinfo/\d+'))
    
    catalog = []
    processed_urls = set()

    for elem in event_elements:
        href = elem['href']
        event_url = f"{BASE_URL}{href}"
        
        if event_url in processed_urls:
            continue
        processed_urls.add(event_url)

        title = clean_text(elem.text)
        if not title or "vs" not in title.lower():
            continue

        print(f"Analizando evento deportivo: {title}")
        
        time_text = "20:45"
        parent_row = elem.find_parent('tr')
        if parent_row:
            time_elem = parent_row.find('span', class_='evtime')
            if time_elem:
                time_text = clean_text(time_elem.text)

        channels = []
        try:
            time.sleep(1)
            event_resp = requests.get(event_url, headers=HEADERS, timeout=10)
            event_soup = BeautifulSoup(event_resp.text, 'html.parser')
            
            links_tables = event_soup.find_all('table', class_='linksnew')
            canal_index = 1
            
            for table in links_tables:
                for row in table.find_all('tr'):
                    a_tag = row.find('a', href=True)
                    if not a_tag:
                        continue
                        
                    link_href = a_tag['href']
                    stream_url = ""
                    
                    if "acestream://" in link_href:
                        stream_url = link_href
                    elif "webplayer" in link_href or "pk=" in link_href:
                        hash_match = re.search(r'[a-fA-F0-9]{40}', link_href)
                        if hash_match:
                            stream_url = f"acestream://{hash_match.group(0)}"
                    
                    if stream_url:
                        quality = "HD" if "hd" in row.text.lower() else "SD"
                        peers_match = re.search(r'(\d+)\s*%', row.text)
                        peers = f"{peers_match.group(1)} pares" if peers_match else "120 pares"
                        
                        channels.append({
                            "name": f"Canal {canal_index}",
                            "url": stream_url,
                            "quality": quality,
                            "peers": peers
                        })
                        canal_index += 1
                        
        except Exception as e:
            print(f"Error leyendo canales de {title}: {e}")
            continue

        if channels:
            catalog.append({
                "id": href.split('/')[-2],
                "title": title,
                "time": time_text,
                "type": "EVENTO",
                "category": "Evento deportivo",
                "channels": channels
            })

        if len(catalog) >= 10:
            break

    with open("catalog.json", "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=4)
    print(f"Catálogo final generado con {len(catalog)} eventos activos.")

if __name__ == "__main__":
    main()
