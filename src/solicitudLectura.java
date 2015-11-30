import org.llrp.ltk.net.LLRPConnector;
import org.llrp.ltk.net.LLRPEndpoint;
import org.llrp.ltk.types.LLRPMessage;

/**
 * 
 */

/**
 * Esta clase se encarga de procesar las lecturas por solicitud en un nuevo hilo
 * de ejecucion
 * 
 * @author José Fernández
 *
 */
public class solicitudLectura implements Runnable, LLRPEndpoint {

	@SuppressWarnings("unused")
	private LLRPConnector reader;
	
	
	public solicitudLectura() {
		super();
		
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	@Override
	public void errorOccured(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void messageReceived(LLRPMessage arg0) {
		// TODO Auto-generated method stub
	
		System.out.println("mensaje recibido!!!");
		
	}

}
