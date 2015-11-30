
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;

/**
 * Esta clase permite establecer conexiones TCP en modo cliente
 * con un servidor remoto para el envío de datos.
 * @author José Fernández
 * @version 1.0, 04/07/2013
 */
public class tcpClient implements Runnable{

	private int puertoDestino;
	private String ipDestino;
	private Socket socket;
	private DataOutputStream message;
	static private int BUFFER_SIZE = 255;
	private List<TCPClientListener> listeners = new ArrayList<TCPClientListener>();

	/**
	 * Crea un nuevo objeto tcpClient.
	 * @param puertoDestino Puerto remoto al que se establecerá la conexión.
	 * @param ipDestino Socket remoto al que se establecerá la conexión.
	 * @param escuchante Objeto que implementa los métodos de la interfaz TCPClientListener.
	 */
	public tcpClient(int puertoDestino, String ipDestino, TCPClientListener escuchante) {
		super();
		this.puertoDestino = puertoDestino;
		this.ipDestino = ipDestino;
		this.addTCPClientListener(escuchante);
	}

	/**
	 * Inicia la conexión con el Socket remoto.
	 * @throws UnknownHostException Si la dirección IP del host remoto no puede ser encontrada.
	 * @throws IOException Si un error de entrada/salida (I/O) ocurrió al crear el Socket.
	 * @see #terminateRemoteConection()
	 */
	public void startRemoteConection() throws UnknownHostException, IOException{
		socket = new Socket(ipDestino, puertoDestino);
		Thread thread1 = new Thread(this);
		thread1.start();
	}

	/**
	 * Finaliza la conexion con el Socket remoto
	 * @throws IOException Si un error de entrada/salida (I/O) ocurrió al cerrar el Socket.
	 * @see #startRemoteConection()
	 */
	public void terminateRemoteConection() throws IOException{
		socket.close();	
	}

	/**
	 * Envia un mensaje al Socket remoto.
	 * @param b Mensaje a enviar (bytes).
	 * @throws IOException Si un error de entrada/salida (I/O) ocurrió al momento de enviar los datos.
	 */
	public void sendData(byte[] b) throws IOException{
		message = new DataOutputStream(socket.getOutputStream());
		message.write(b);
	}

	/**
	 * Envia un mensaje al Socket remoto.
	 * @param b Mensaje a enviar (String).
	 * @throws IOException Si un error de entrada/salida (I/O) ocurrió al momento de enviar los datos.
	 */
	public void sendData(String b) throws IOException{
		message = new DataOutputStream(socket.getOutputStream());
		message.write(b.getBytes());
	}

	@Override
	public void run() {
		// Leer mensajes desde el servidor
		BufferedInputStream incomingpackage = null;
		String id = socket.getInetAddress() + ":" + socket.getPort();


		while (!socket.isClosed()) {
			try {
				incomingpackage = new BufferedInputStream(socket.getInputStream());

				byte[] b = new byte[BUFFER_SIZE];
				byte[] c;
				int message = incomingpackage.read(b,0,BUFFER_SIZE);
				c = Arrays.copyOf(b, message);

				this.onMessageReceived(c, id);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}

	public static interface TCPClientListener extends EventListener{
		/**
		 * Ejecuta este método de forma asíncrona cada vez que un Socket remoto
		 * envía un mensaje al cliente.
		 * @param b Mensaje (bytes).
		 * @param id Dirección del Socket remoto que originó el mensaje.
		 */
		void onMessageReceived(byte b[], String id);
	}	

	public void addTCPClientListener(TCPClientListener listener){
		listeners.add(listener);
	}
	public void removeTCPClientListener(TCPClientListener listener){
		listeners.remove(listener);
	}

	protected void onMessageReceived(byte b[], String id){
		for(TCPClientListener listener:listeners)
			listener.onMessageReceived(b, id);
	}

}
