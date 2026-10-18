import requests
from bs4 import BeautifulSoup
import json
import re
import time

# Usamos una lista de espejos alternativos por si el principal se cae
DOMAINS = ["https://livetv905.me", "https://livetv.sx", "https://livetv744.me"]

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
    "Accept-Language": "es-ES,es;q=0.9,en;q=0.8",
    "Cache-Control": "max-age=0",
    "Connection": "keep-alive"
}

def clean_text(text):
    return " ".join(text.split()) if text else ""

def main():
    print("Iniciando raspado con simulación de navegación real...")
    
    response = None
    active_base = ""
    
    # Probar los espejos disponibles hasta que uno responda con éxito
    for domain in DOMAINS:
        try:
            print(f"Probando conexión con: {domain}")
            res = requests.get(f"{domain}/es/allupcoming/", headers=HEADERS, timeout=15)
            if res.status_code == 200 and "eventinfo" in res.text:
                response = res
                active_base = domain
                print(f"¡Conexión establecida con éxito en {domain}!")
                break
        except Exception as e:
            print(f"Espejo no disponible: {e}")
            continue

    if not response:
        print("Error crítico: Todos los servidores espejo de LiveTV están caídos o bloqueados.")
        # Generar un catálogo limpio de prueba para que la app no se rompa en el FireStick
        backup = [{"id":"0","title":"Comprobando servidor... Inténtalo de nuevo","time":"00:00","type":"EVENTO","category":"Sistema","links":[]}]
        with open("catalog.json", "w", encoding="utf-8") as f:
            json.dump(backup, f, ensure_ascii=False, indent=4)
        return

    soup = BeautifulSoup(response.text, 'html.parser')
    event_elements = soup.find_all('a', href=re.compile(r'/es/eventinfo/\d+'))
    
    catalog = []
    processed_urls = set()

    for elem in event_elements:
        href = elem['href']
        event_url = f"{active_base}{href}"
        
        if event_url in processed_urls:
            continue
        processed_urls.add(event_url)

        title = clean_text(elem.text)
        if not title or "vs" not in title.lower() and "–" not in title:
            continue

        print(f"-> Cargando partido: {title}")
        
        time_text = "20:45"
        parent_row = elem.find_parent(['tr', 'td', 'div'])
        if parent_row:
            time_elem = parent_row.find(class_=re.compile(r'time|evtime'))
            if time_elem:
                time_text = clean_text(time_elem.text)

        links_list = []
        try:
            time.sleep(2.0) # Delay para evitar que Cloudflare sospeche del bot
            event_resp = requests.get(event_url, headers=HEADERS, timeout=10)
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
                    
                    links_list.append({
                        "name": f"Canal {canal_index}",
                        "url": stream_url,
                        "quality": quality,
                        "peers": "120"
                    })
                    canal_index += 1
                        
        except Exception as e:
            print(f"Salto de sub-enlace: {e}")
            continue

        if links_list:
            catalog.append({
                "id": href.split('/')[-2],
                "title": title,
                "time": time_text,
                "type": "EVENTO",
                "category": "Deportes en Directo",
                "links": links_list
            })

        if len(catalog) >= 12:
            break

    with open("catalog.json", "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=4)
        
    print(f"Catálogo finalizado. Sincronizados {len(catalog)} partidos activos.")

if __name__ == "__main__":
    main()
