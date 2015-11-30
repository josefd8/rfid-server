
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;

/**
 * La clase implementa los metodos necesarios para establecer conexiones
 * UDP en modo servidor
 * 
 * @author José Fernández
 * @version 2.0, 16/07/2013
 */

public class udpServer implements Runnable{
	private int puertoEscucha;
	private String IPEscucha;
	DatagramSocket socket;
	byte[] receiveData = new byte[255];
	private List<UDPServerListener> listeners = new ArrayList<UDPServerListener>();
	
	public void setPuertoEscucha(int puertoEscucha) {
		this.puertoEscucha = puertoEscucha;
	}

	public void setIPEscucha(String iPEscucha) {
		IPEscucha = iPEscucha;
	}

	public udpServer() {
		super();
		//Debe registrarse el listener
		//this.addUDPServerListener(new Principal2());
	}

	public void startUDPServer() throws SocketException, UnknownHostException{
			socket = new DatagramSocket(this.puertoEscucha, InetAddress.getByName(this.IPEscucha));
			Thread thread1 = new Thread(this);
			thread1.start();
	}

	public void stopUDPServer(){
		socket.close();
	}

	public void sendData(InetAddress address, int port, byte[] message){
		DatagramPacket p = new DatagramPacket(message, port, address, port);
		try {
			socket.send(p);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		while (true) {
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try {

				socket.receive(receivePacket);

				byte[] c;
				c = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
				String id = receivePacket.getAddress().toString() + ":" + receivePacket.getPort();
				this.onMessageReceived(c, id.replace("/",""));
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}	
	}


	public static interface UDPServerListener extends EventListener{
		void onMessageReceived(byte b[], String id);
	}	

	public void addUDPServerListener(UDPServerListener listener){
		listeners.add(listener);
	}
	public void removeUDPServerListener(UDPServerListener listener){
		listeners.remove(listener);
	}

	protected void onMessageReceived(byte b[], String id){
		for(UDPServerListener listener:listeners)
			listener.onMessageReceived(b, id);
	}


}
