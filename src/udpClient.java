
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;

/**
 * Esta clase implementa los metodos para crear una conexion UDP en
 * modo cliente. Todas las  * Exceptiones son "throws" o "arrojadas" 
 * a la clase que la instancia  * donde deben ser controladas de la 
 * forma apropiada
 * 
 * @author José Fernández
 * @version 1.0, 04/07/2013
 */
public class udpClient implements Runnable{

	private int puertoLocal;
	private String ipLocal;
	private DatagramSocket udpSocket;
	private DatagramPacket message;
	byte[] receiveData = new byte[255];
	private List<UDPClientListener> listeners = new ArrayList<UDPClientListener>();

	public udpClient() {
		super();
		//Debe registrarse el listener
		//this.addUDPClientListener(new Principal2());
	}

	public void startRemoteConection() throws UnknownHostException, SocketException {
		//Si no se especifica IP:puerto el socket se enlaza a cualquier puerto
		//disponible en la maquina local
		udpSocket = new DatagramSocket();
		Thread thread1 = new Thread(this);
		thread1.start();
	}
		
	public void startRemoteConection(String ipLocal, int puertoLocal ) throws UnknownHostException, SocketException {
		this.ipLocal = ipLocal;
		this.puertoLocal = puertoLocal;
		udpSocket = new DatagramSocket(this.puertoLocal,InetAddress.getByName(this.ipLocal));
		Thread thread1 = new Thread(this);
		thread1.start();
	}


	public void terminateRemoteConection() {
		udpSocket.close();
	}

	public void sendData(byte[] b, String ipDestino, int puertoDestino) throws IOException, PortUnreachableException{
		message = new DatagramPacket(b, b.length, InetAddress.getByName(ipDestino), puertoDestino);
		udpSocket.send(message);
	}

	@Override
	public void run(){
		// TODO Para leer mensajes enviados por el servidor

		while (!udpSocket.isClosed()) {
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try {

				udpSocket.receive(receivePacket);

				byte[] c;
				c = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
				String id = receivePacket.getAddress().toString() + ":" + receivePacket.getPort();

				this.onMessageReceived(c, id.replace("/", ""));

			} catch (IOException e) {
				e.printStackTrace();
			}	
		}	

	}

	public static interface UDPClientListener extends EventListener{
		void onMessageReceived(byte b[], String id);
	}	

	public void addUDPClientListener(UDPClientListener listener){
		listeners.add(listener);
	}
	public void removeUDPClientListener(UDPClientListener listener){
		listeners.remove(listener);
	}

	protected void onMessageReceived(byte b[], String id){
		for(UDPClientListener listener:listeners)
			listener.onMessageReceived(b, id);
	}	
}
