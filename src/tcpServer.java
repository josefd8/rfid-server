import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Esta clase implementa los métodos necesarios para establecer conexiones
 * TCP en modo servidor. Establece hilos individuales para la escucha de 
 * mensajes de cada cliente.
 * 
 * @author José Fernández
 * @version 2.3, 16/07/2013
 */

public class tcpServer implements Runnable{

	private int puertoEscucha;
	private String IPEscucha;
	private ServerSocket nuevoSocket = null;;
	private operation operation;
	private Socket socket;
	static private int BUFFER_SIZE = 1024;
	private CopyOnWriteArrayList<Socket> clientsTable = new CopyOnWriteArrayList<Socket>();
	private List<TCPServerListener> listeners = new ArrayList<TCPServerListener>();	
	private String identificador;
	
	public enum operation {
		CLIENTS_LISTENER, MESSAGE_LISTENER 
	}

	private operation getOperation() {
		return operation;
	}

	private Socket getSocket() {
		return socket;
	}

	private void setSocket(Socket socket) {
		this.socket = socket;
	}

	private void setOperation(operation operation) {
		this.operation = operation;
	}

	/**
	 * Obtiene la direccón del puerto local actual al que el objeto ServerSocket
	 * esta asociado.
	 * @return Entero que representa el puerto (0-65535).
	 */
	public int getPuertoEscucha() {
		return puertoEscucha;
	}

	/**
	 * Se obtiene la cantidad de clientes conectados.
	 * @return Número entero que representa la cantidad de clientes conectados.
	 */
	public int getClientsTable() {
		return clientsTable.size();
	}

	/**
	 * Puerto local en el que se establecera la escucha de peticiones de clientes.
	 * Si es cero (0), se toma el primer puerto disponible automaticamente.
	 * @param puertoEscucha Entero que representa el puerto al que establecer la conexión.
	 */
	public void setPuertoEscucha(int puertoEscucha) {
		this.puertoEscucha = puertoEscucha;
	}

	/**
	 * Dirección IP local desde la cual se creará la conexión.
	 * @param iPEscucha Cadena de carácteres que representan la dirección IP ("127.0.0.1").
	 * Si el parámetro es vacío (""), se iniciará el servidor en la primera interfaz de red
	 * disponible (127.0.0.1).
	 */
	public void setIPEscucha(String iPEscucha) {
		IPEscucha = iPEscucha;
	}

	/**
	 * Obtiene la dirección IP a la que esta asociada el objeto ServerSocket actual.
	 * @return Cadena de carácteres que representan la dirección IP ("127.0.0.1")
	 */
	public String getIPEscucha() {
		return IPEscucha;
	}

	public String getIdentificador() {
		return identificador;
	}

	public void setIdentificador(String identificador) {
		this.identificador = identificador;
	}

	/**
	 * Crea una nueva instancia del servidor TCP
	 * @param escuchante Objeto que implementa la interfaz TCPServerListener, receptora de
	 * eventos
	 * @param identificador Identificador genérico del servidor, por ejemplo "server1". Cuando
	 * una misma aplicación debe crear mas de 2 Servidores TCP, ayuda a determinar por cual de
	 * todos ellos sucedió cierto evento.
	 */
	public tcpServer(TCPServerListener escuchante, String identificador) {
		this.addTCPServerListener(escuchante);
		this.setIdentificador(identificador);
	}

	/**
	 * Inicia el servidor TCP
	 * @throws UnknownHostException No se pudo encontrar direccion IP para el host.
	 * @throws IOException Ocurrió un error al abrir el socket.
	 * @throws IllegalArgumentException El número de puerto esta fuera del rango válido.
	 */
	@SuppressWarnings("static-access")
	public void startTcpServer() throws UnknownHostException, IOException, IllegalArgumentException{
		this.nuevoSocket = new ServerSocket(this.puertoEscucha,10,InetAddress.getByName(this.IPEscucha));

		//Si el puerto se establecio como 0.
		if (this.puertoEscucha == 0) {
			this.setPuertoEscucha(this.nuevoSocket.getLocalPort());
		}

		//Si la IP se establece a "".
		if (this.IPEscucha.matches("")) {
			InetAddress ia = this.nuevoSocket.getInetAddress();
			this.setIPEscucha(ia.getHostAddress());
		}

		this.setOperation(operation.CLIENTS_LISTENER);
		Thread thread1 = new Thread(this);
		thread1.setName("CLIENTS_LISTENER_tcpServer");
		thread1.start();
	}

	@SuppressWarnings("static-access")
	@Override
	public void run() {

		switch (this.getOperation()) {

		case CLIENTS_LISTENER:

			while (!nuevoSocket.isClosed()) {
				
				Socket newconex = new Socket();
				try {

					try {
						newconex = nuevoSocket.accept();
						this.clientsTable.add(newconex);
					} catch (SocketException e) {
						this.nuevoSocket.close();
						break;
					}

					if (newconex != null) {

						this.setOperation(operation.MESSAGE_LISTENER);
						this.setSocket(newconex);
						Thread thread3 = new Thread(this);
						thread3.setName("MESSAGE_LISTENER_tcpServer");
						thread3.start();
						Thread.sleep(500);
					}

					String id = newconex.getInetAddress() + ":" + newconex.getPort();

					this.onClientConnected(id.replace("/", ""), this.getIdentificador());


					Thread.sleep(500);
				} catch (IOException e) {
					System.out.println("Error in tcpServer - CLIENTS_LISTENER thread: " + e.getLocalizedMessage());
				} catch (InterruptedException e) {
					System.out.println("Error in tcpServer - CLIENTS_LISTENER thread: " + e.getLocalizedMessage());
				}
			}
			break;


		case MESSAGE_LISTENER:

			Socket socketclient = new Socket();
			socketclient = this.getSocket();
			BufferedInputStream incomingpackage = null;
			String id = socketclient.getInetAddress() + ":" + socketclient.getPort();

			Socket remote = socketclient;

			while (!socketclient.isClosed()) {
				try {
					PrintWriter out = new PrintWriter(socketclient.getOutputStream(), true);
					if (out.checkError()) {
						clientsTable.remove(socketclient);
						socketclient.close();
						break;
					} 	
					incomingpackage = new BufferedInputStream(socketclient.getInputStream());
					byte[] b = new byte[BUFFER_SIZE];
					byte[] c;
					int message = incomingpackage.read(b,0,BUFFER_SIZE);

					if (message >= 0) {
						c = Arrays.copyOf(b, message);

						this.onMessageReceived(c, id.replace("/", ""), this.getIdentificador(),remote);
					} else {

						socketclient.close();
						clientsTable.remove(socketclient);
						this.onClientDisconnected(id.replace("/", ""), this.getIdentificador());
						break;
					}

				} catch (IOException e) {
					System.out.println("tcpServer: " + id.replace("/", "") + " " + e.getMessage());
					clientsTable.remove(socketclient);
					break;
				}	
			}

			break;
		}		
	}

	/**
	 * Envía el mensaje al cliente identificado por por una dirección IP y un puerto.
	 * @param ipAdress IP y puerto del cliente remoto (127.0.0.1:8080).
	 * @param message Mensaje a enviar (bytes).
	 * @param remote objeto tipo Socket que representa el socket destino a enviar datos.
	 */
	public void sendData(String ipAdress, String message,Socket remote){


		try {
			DataOutputStream salida = new DataOutputStream(remote.getOutputStream());
			salida.write(message.getBytes());

		} catch (IOException e) {
			System.out.println("sendData: " + e.getMessage());
		} catch (Exception e){
			System.out.println("sendData: " + e.getMessage());
		}
	}


	public void sendData(String ipAdress, byte[] message){

		for (Socket s : clientsTable) {
			String id = s.getInetAddress() + ":" + s.getPort();
			if (id.contains(ipAdress)) {
				try {
					DataOutputStream salida = new DataOutputStream(s.getOutputStream());
					salida.write(message);
					break;
				} catch (IOException e) {
					System.out.println("sendData: " + e.getMessage());
				}catch (Exception e){
					System.out.println("sendData: " + e.getMessage());
				}
			}	
		}
	}

	/**
	 * Envía el mensaje al cliente identificado por por una dirección IP y un puerto.
	 * @param ipAdress IP y puerto del cliente remoto (127.0.0.1:8080).
	 * @param message Mensaje a enviar (String).
	 */
	public void sendData(String ipAdress, String message){

		for (Socket s : clientsTable) {
			String id = s.getInetAddress() + ":" + s.getPort();
			if (id.contains(ipAdress)) {
				try {
					DataOutputStream salida = new DataOutputStream(s.getOutputStream());
					salida.write(message.getBytes());
					break;
				} catch (IOException e) {
					System.out.println("sendData: " + e.getMessage());
				}catch (Exception e){
					System.out.println("sendData: " + e.getMessage());
				}
			}	
		}
	}

	/**
	 * Envía el mismo mensaje a todos los clientes conectados.
	 * @param message Mensaje (String).
	 */
	public void broadcastMessage(String message){

		for (Socket s : clientsTable) {
			try {
				if(!s.isClosed())
				{
					DataOutputStream salida = new DataOutputStream(s.getOutputStream());
					salida.write(message.getBytes());

				}
			} catch (IOException e) {
				System.out.println("broadcastMessage: " + e.getMessage());
			}catch (Exception e){
				System.out.println("broadcastMessage: " + e.getMessage());
			}
		}		
	}

	/**
	 * Envía el mismo mensaje a todos los clientes conectados.
	 * @param message Mensaje (bytes).
	 */
	public void broadcastMessage(byte[] message){

		for (Socket s : clientsTable) {
			try {
				DataOutputStream salida = new DataOutputStream(s.getOutputStream());
				salida.write(message);
			} catch (IOException e) {
				System.out.println("broadcastMessage: " + e.getMessage());
			}catch (Exception e){
				System.out.println("broadcastMessage: " + e.getMessage());
			}
		}
	}

	/**
	 * Desconecta el cliente especificado por una dirección IP y puerto.
	 * @param ipAdress IP y puerto del cliente a desconectar (127.0.0.1:8080).
	 */
	public void disconnectClient(String ipAdress){

		for (Socket s : clientsTable) {
			String id = s.getInetAddress() + ":" + s.getPort();
			if (id.contains(ipAdress)) {
				try {
					s.close();
					clientsTable.remove(s);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}	
		}
	}

	/**
	 * Desconecta todos los clientes asociados a este Socket.
	 */
	public void disconnectAllClients(){
		for (Socket s : clientsTable) {
			try {
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		clientsTable.clear();
	}

	/**
	 * Cierra el Socket TCP.
	 * @throws IOException Ocurrió un error de entrada salida (I/O) al cerrar el Socket
	 */
	public void stopTcpServer() throws IOException{
		if (!(this.nuevoSocket == null)) {
			this.disconnectAllClients();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			this.nuevoSocket.close();
		}		
	}


	/**
	 *Proporciona métodos para el uso de la clase tcpServer.
	 * @author José Fernández
	 */
	public static interface TCPServerListener extends EventListener{
		/**
		 * Este método es ejecutado de forma asíncrona cada vez que se recibe
		 * un nuevo mensaje de algunos de los clientes conectados.
		 * @param b Mensaje enviado por el cliente (bytes)
		 * @param id Cliente que envía el mensaje (ip:puerto)
		 * @param identificador Identificador de la instancia de tcpServer
		 * @param remote Objeto tipo Socket que identifica al cliente remoto
		 */
		void onMessageReceived(byte b[], String id, String identificador,Socket remote);
		/**
		 * Este método se ejecuta de forma asíncrona cada vez que un nuevo
		 * cliente establece una conexión.
		 * @param id Dirección ip:puerto del cliente remoto.
		 * @param identificador Identificador de la instancia de tcpServer
		 */
		void onClientConnected(String id, String identificador);
		/**
		 * Este método se ejecuta de forma asíncrona cada vez que un cliente
		 * cierra su conexión con el servidor TCP.
		 * @param id Dirección ip:puerto del cliente.
		 * @param identificador Identificador de la instancia de tcpServer
		 */
		void onClientDisconnected(String id, String identificador);
	}	

	public void addTCPServerListener(TCPServerListener listener){
		listeners.add(listener);
	}
	public void removeTCPServerListener(TCPServerListener listener){
		listeners.remove(listener);
	}


	protected synchronized void onMessageReceived( byte b[],  String id,  String identificador, Socket remote){
		for (int i=0;i<listeners.size();i++) {
			listeners.get(i).onMessageReceived(b, id, identificador,remote);	    
		}
	}
	protected synchronized void onClientConnected(String id, String identificador){

		for (int i=0;i<listeners.size();i++) {
			listeners.get(i).onClientConnected(id, identificador);    
		}

	}    
	protected synchronized void onClientDisconnected(String id, String identificador){

		for (int i=0;i<listeners.size();i++) {
			listeners.get(i).onClientDisconnected(id, identificador);
		}

	} 
}