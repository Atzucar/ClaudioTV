import requests
from bs4 import BeautifulSoup
import json
import re
import time

# Usamos el espejo activo principal y estable de LiveTV
BASE_URL = "https://livetv905.me" 
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Accept-Language": "es-ES,es;q=0.9"
}

def clean_text(text):
    return " ".join(text.split()) if text else ""

def main():
    print("Iniciando raspado con mapeo compatible para la App...")
    try:
        response = requests.get(f"{BASE_URL}/es/allupcoming/", headers=HEADERS, timeout=20)
    except Exception as e:
        print(f"Error de red crítico con el servidor espejo: {e}")
        return

    if response.status_code != 200:
        print(f"LiveTV denegó el acceso. Código: {response.status_code}")
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
        if not title or "vs" not in title.lower() and "–" not in title:
            continue

        print(f"-> Procesando partido: {title}")
        
        time_text = "20:45"
        parent_row = elem.find_parent(['tr', 'td', 'div'])
        if parent_row:
            time_elem = parent_row.find(class_=re.compile(r'time|evtime'))
            if time_elem:
                time_text = clean_text(time_elem.text)

        # Usamos la estructura nativa exacta que tu Activity lee
        links_list = []
        try:
            time.sleep(1.5)
            event_resp = requests.get(event_url, headers=HEADERS, timeout=12)
            event_soup = BeautifulSoup(event_resp.text, 'html.parser')
            
            canal_index = 1
            for a_tag in event_soup.find_all('a', href=True):
                link_href = a_tag['href']
                stream_url = ""
                
                if "acestream://" in link_href:
                    stream_url = link_href
                elif "webplayer" in link_href or "pk=" in link_href or "id=" in link_href:
                    hash_match = re.search(r'[a-fA-F0-9]{40}', link_href)
                    if hash_match:
                        stream_url = f"acestream://{hash_match.group(0)}"
                
                if stream_url and stream_url not in [l['url'] for l in links_list]:
                    row_text = a_tag.find_parent().text.lower() if a_tag.find_parent() else ""
                    quality = "HD" if "hd" in row_text or "kbps" in row_text else "SD"
                    
                    # REQUISITO CLAVE: Nombre de campos idénticos a los del Gist (links, name, url, quality, peers)
                    links_list.append({
                        "name": f"Canal {canal_index}",
                        "url": stream_url,
                        "quality": quality,
                        "peers": "120" # Si la app procesa los pares como entero o texto simple
                    })
                    canal_index += 1
                        
        except Exception as e:
            print(f"Aviso: Salto de evento: {e}")
            continue

        if links_list:
            # Estructura del objeto principal coincidente con tu Gist de CATALOG_URL
            catalog.append({
                "id": href.split('/')[-2],
                "title": title,
                "time": time_text,
                "type": "EVENTO",
                "category": "Deportes en Directo",
                "links": links_list # Cambiado de 'channels' a 'links' para tu App
            })

        if len(catalog) >= 15:
            break

    # Escribir el JSON definitivo
    with open("catalog.json", "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=4)
        
    print(f"Proceso finalizado. Guardado con {len(catalog)} partidos.")

if __name__ == "__main__":
    main()
