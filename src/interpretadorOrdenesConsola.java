import static java.lang.System.in;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Esta clase mantiene un hilo de procesamiento que toma las instrucciones
 * escritas en la línea de comandos del sistema, y genera acciones 
 * correspondientes.
 * @author José Fernández
 */
public class interpretadorOrdenesConsola implements Runnable{

	private List<OrdenesDeConsola> listeners = new ArrayList<OrdenesDeConsola>();
		
	public interpretadorOrdenesConsola(OrdenesDeConsola escuchante){
		this.addOrdenesDeConsolaListener(escuchante);
		Thread hilo = new Thread(this);
		hilo.setName("interpretadorOrdenesConsola");
		hilo.start();
	}

	@Override
	public void run() {

		while (true) {

			@SuppressWarnings("resource")
			Scanner keyboard = new Scanner(in);
			
			String action;
			try {
				if (keyboard.hasNext()) {
					action = keyboard.next();
					action = action.toLowerCase();
				} else {
					action = "";
				}
			} catch (NoSuchElementException e) {
				action = "";
				e.printStackTrace();
			}

			switch (action) {
			
			case "salir":
				this.nuevaOrden(action);
				break;
				
			case "reiniciar":
				this.nuevaOrden(action);
				break;
				
			case "estado":
				this.nuevaOrden(action);
				break;
				
			case "finalizar":
				this.nuevaOrden(action);
				break;
				
			case "ondebug":
				this.nuevaOrden(action);
				break;
				
			case "offdebug":
				this.nuevaOrden(action);
				break;
				
			case "":
				break;
				
			case "opciones":
				System.out.println(" ondebug:   Muestra las operaciones en curso en la ventana de comandos.");
				System.out.println(" offdebug:  Apaga la salida en pantalla de las operaiones en curso.");
				System.out.println(" finalizar: Fuerza la finalización de todas las lecturas en curso.");
				System.out.println(" estado:    Solicita el estado de todos los readers.");
				System.out.println(" reiniciar: Fuerza el reinicio del parser y la" +
						" finalización de las lecturas pendientes.");
				System.out.println(" salir:     Finaliza el parser.");
				break;

			default:
				System.out.println("Orden no reconocida. Introduzca 'opciones' " +
						"para ver los comandos disponibles");
				break;
			}	
		}

	}
	
	public static interface OrdenesDeConsola extends EventListener{
		void nuevaOrden(String orden);
	}	
	
	
	public void addOrdenesDeConsolaListener(OrdenesDeConsola listener){
		listeners.add(listener);
	}
	public void removeOrdenesDeConsolaListener(OrdenesDeConsola listener){
		listeners.remove(listener);
	}
	
	protected void nuevaOrden(String orden){
		for(OrdenesDeConsola listener:listeners)
			listener.nuevaOrden(orden);
	}

}
