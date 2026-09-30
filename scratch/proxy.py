import socket
import threading

def handle_client(client_socket, target_host, target_port):
    target_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        target_socket.connect((target_host, target_port))
    except Exception as e:
        print(f"[-] Erro ao ligar ao servidor real {target_host}:{target_port}: {e}")
        client_socket.close()
        return

    def forward(source, destination):
        try:
            while True:
                data = source.recv(4096)
                if not data:
                    break
                destination.sendall(data)
        except Exception:
            pass
        finally:
            source.close()
            destination.close()

    threading.Thread(target=forward, args=(client_socket, target_socket), daemon=True).start()
    threading.Thread(target=forward, args=(target_socket, client_socket), daemon=True).start()

def main():
    local_port = 8080
    target_host = "10.0.2.24"
    target_port = 80

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(('127.0.0.1', local_port))
    server.listen(5)
    print(f"[+] Proxy ativo no Mac: http://localhost:{local_port} -> {target_host}:{target_port}")
    print("[+] No emulador Android, configura o servidor como: http://10.0.2.2:8080/")
    print("[+] Prime Ctrl+C para parar o proxy.")

    try:
        while True:
            client, addr = server.accept()
            handle_client(client, target_host, target_port)
    except KeyboardInterrupt:
        print("\n[-] A parar o proxy.")
    finally:
        server.close()

if __name__ == "__main__":
    main()
