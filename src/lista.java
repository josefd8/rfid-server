import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Timer;

/**
 * Esta clase reune funciones para el manejo de listas de etiquetas (tags)
 * producidos por un reader.
 * @author José Fernández
 */

public class lista implements ActionListener, clienteReader.TagListener {

	/**
	 * Antena o grupo de antenas que se tomarán en cuenta en la lectura, representado
	 * por números separados por comas (1,2,3,...n).
	 */
	private String antenas;	

	/**
	 * Lista donde cada elemento representa una antena. Su contenido se obtiene de la
	 * información proporcionada en antenas.
	 * @see #antenas
	 */
	private HashSet<String> listaAntenas = new HashSet<String>();

	/**
	 * Para las listas 0, representa el tiempo que debe pasar desde la ultima vez
	 * que una etiqueta fue leída por un reader para esta considerarse fuera de vista.
	 */
	private int tiempoTags;

	/**
	 * Entero que representa el ID de esta lectura (código de operación asociado).
	 */
	private int idSolicitud;

	/**
	 * Duración de la lectura (en segundos). Si se establece como cero (0) se considera
	 * lectura "infinita" (la lectura no será realmente infinita, tendra una duración máxima
	 * establecida en MAX_TIEMPO_VIDA).
	 * @see #MAX_TIEMPO_VIDA
	 */
	private int duracionSolicitud;

	/**
	 * En listas de lectura con tiempo infinito, establece un tiempo máximo en que la
	 * solicitud permanecerá activa.
	 */
	private int MAX_TIEMPO_VIDA = 3600;

	/**
	 * Indica el tipo de lista. Si False es lista 0, si True es por solicitud.
	 */
	private boolean tipo;

	/**
	 * Mapa que contiene los tags leidos
	 */
	private Map<String, etiqueta> etiquetas = new ConcurrentHashMap<String, etiqueta>();

	/**
	 * Indica si la operación de lectura actual se encuentra en tiempo de prórroga.
	 * Cuando una operación de lectura llega a su fin, se establece un tiempo prudencial
	 * de 30 segundos (prórroga), durante el cual se espera la llegada de nuevos tags.
	 * Si durante ese tiempo no se reciben nuevos tags, se cierra la operación a los 30
	 * segundos, si llegan mas tags, se procesan y se cuentan 30 segundos adicionales.
	 */
	private boolean prorroga = false;

	/**
	 * Duración de la prórroga
	 */
	private int tiempoProrroga;

	private int contador = 0;
	protected clienteReader padre;
	Timer reloj;

	public lista(String antenas, boolean tipo, int tiempoTags,
			int idSolicitud, int duracionSolicitud, clienteReader padre, int tiempoProrroga) {
		super();
		this.antenas = antenas;
		this.tipo = tipo;
		this.tiempoTags = tiempoTags;
		this.idSolicitud = idSolicitud;
		this.duracionSolicitud = duracionSolicitud;
		this.padre = padre;
		this.tiempoProrroga = tiempoProrroga;
	}


	public void iniciar(){

		if (this.tipo) {
			
			String[] a = antenas.split("\\,");

			for (String an : a) {
				this.listaAntenas.add(an);
			}
		}	

		reloj = new Timer(1000, this);
		reloj.start();	
	}


	private void actualizar_lista(etiqueta nuevaEtiqueta){
		//Si la etiqueta ya se encuentra en la lista, actualiza la fecha de
		//última vez vista, si no, se agrega a la lista.

		String llave = nuevaEtiqueta.epc;

		if (etiquetas.containsKey(llave)) {
			etiquetas.get(llave).ultimaVezFecha = nuevaEtiqueta.ultimaVezFecha;
		} else {
			
			//Solo se tomaran en cuenta las etiquetas que entren por determinada
			//antenna. Si no se especifica (igual a 0), se toman todas.
			
			if (filtroAntena(nuevaEtiqueta.idAntena)) {
				etiquetas.put(llave, nuevaEtiqueta);

				if (this.tipo) {
					nuevaEtiqueta.tipoOperacion = 2;
					nuevaEtiqueta.idInventario = this.idSolicitud;
					padre.notificacionNuevaEtiquetaLectura(nuevaEtiqueta);

					//Si nos encontramos en prórroga cuando se lee una nueva etiqueta
					//que no se conocía, se aumenta la prorroga.
					
					if (this.prorroga) {						
						this.duracionSolicitud = this.contador + this.tiempoProrroga;
					}

				} else {
					nuevaEtiqueta.tipoOperacion = 0;
					padre.notificacionNuevaEtiquetaLectura(nuevaEtiqueta);
				}
			}
		}	
	}

	/**
	 * Verifica la antena por la que se recibió la etiqueta.
	 * @param idAntena Entero que representa la antena.
	 * @return False si no corresponde con la antena solicitada.
	 */
	private boolean filtroAntena(int idAntena){

		if (this.antenas.matches("0")) {
			return true;
		} else {
			if (this.listaAntenas.contains(String.valueOf(idAntena))) {
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Calcula si la diferencia entre dos fechas es superior o inferior a 
	 * tiempoTags
	 * @param fecha objeto tipo Date sobre el que se calculará la diferencia con la fecha actual
	 * @return Verdadero si la diferencia es superior a tiempoTags.
	 * @see #tiempoTags
	 */
	private boolean analizarFecha(Date fecha){

		Date fecha_actual = new Date();
		long diferencia;

		diferencia = fecha_actual.getTime() - fecha.getTime();

		if (diferencia > this.tiempoTags) {
			return true;
		} else{
			return false;
		}
	}

	@Override
	/**
	 * Cada segundo se recorre la lista de etiquetas y comprueba el campo ultimaVezFecha.
	 * Si ha pasado un tiempo mayor a tiempoTag desde la última vez que se vio la
	 * etiqueta, se elimina de la lista y se genera un evento de tag_out.
	 */
	public void actionPerformed(ActionEvent e) {

		if (this.tipo) {

			if (!prorroga) {
				if ((this.contador == this.duracionSolicitud) || (this.contador == this.MAX_TIEMPO_VIDA)){

					this.prorroga = true;
					this.duracionSolicitud = this.contador + tiempoProrroga;

				} else {

					this.contador = this.contador + 1;

				}
			}

			if (this.prorroga) {

				if ((this.contador == this.duracionSolicitud) || (this.contador == this.MAX_TIEMPO_VIDA)){
					reloj.stop();
					padre.notificacionFinLectura(this.idSolicitud, this);

				} else {
					this.contador = this.contador + 1;
				}	
			}	
		}

		Iterator<Map.Entry<String, etiqueta>> entradas = etiquetas.entrySet().iterator();

		while (entradas.hasNext()) {
			Map.Entry<String, etiqueta> entradatemp = entradas.next();
			Date fechatag = entradatemp.getValue().ultimaVezFecha;
			if (fechatag != null) {
				if ((analizarFecha(fechatag)) && (!this.tipo)) {
					etiqueta etiquetaout = entradatemp.getValue();
					etiquetaout.ultimaVezFecha = new Date();	
					etiquetaout.tipoOperacion = 1;
					padre.notificacionNuevaEtiquetaLectura(etiquetaout);
					entradas.remove();
				}
			}
		}	
		
	}

	@Override
	/**
	 * Este método se ejecuta de forma asíncrona cuando el reader que mantiene
	 * este objeto lista recibe una nueva etiqueta de cualquiera de sus antenas.
	 * @param tag Objeto tipo etiqueta
	 */
	public void nuevoTagRecibido(etiqueta tag) {
		
			try {
				if (((!(tag.epc.matches(""))) || (!(tag.epc == null)))){
					actualizar_lista(tag);
				}
			} catch (Exception e) {
			}

	}

	@Override
	/**
	 * Este método se ejecuta de forma asíncrona cuando el reader solicita la
	 * finalización de una operación de lectura que se encuentra en curso
	 * @param idSolicitud Entero que representa el ID de la solicitud.
	 */
	public void finalizarLectura(int idSolicitud) {
		if (tipo) {
			if ((this.idSolicitud == idSolicitud) || (idSolicitud == 0)) {
				this.contador = this.duracionSolicitud;
				this.prorroga = true;
			}
		}

	}

}
