import java.util.Date;

/**
 * Estructura básica de todos los mensajes generados por un Reader.
 * @author José Fernández
 */

public class etiqueta {
	
	/**
	 * Antena por la que fue leído el mensaje.
	 */
	public int idAntena;
	
	/**
	 * Información EPC de la etiqueta.
	 */
	public String epc;
	
	/**
	 * indica la longitud del EPC (96 o 128 bits).
	 */
	public String largoepc;
	
	/**
	 * Ultima vez que la etiqueta fue leída por el reader.
	 */
	public Date ultimaVezFecha;
	
	/**
	 * Si es reporte de etiqueta, keep alive, mensaje de status, etc.
	 */
	public String descripcionMensaje;
	
	/**
	 * valor identificador de descripcionMensaje.
	 * 1 = tag_in, tag_out o tags leídos en operaciones de lectura.
	 * 2 = Respuesta de una solicitud de estado de reader.
	 * 3 = Notificación de fin de solicitud de estado readers.
	 * 4 = Notificación de fin de lectura.
	 * 5 = Notificación de fin de operación de detener lectura.
	 * 6 = Consulta de operaciones pendientes.
	 * 7 = Notificación de reinicio parser.
	 * 8 = Notificación de cambio de estado de reader (reader conectado/desconectado) o 
	 * notificación de cambio en alguna antena
	 */
	public int idMensaje;
	
	/**
	 * Lector (reader) por el que se realizó la lectura.
	 */
	public int idLector;
	
	/**
	 * Si 0 = tag_in, 1 = tag_out, 2 = inventario.
	 */
	public int tipoOperacion;
	
	/**
	 * Número que identifica el inventario (código operación).
	 */
	public int idInventario;
	
	/**
	 * Identificador único de cada mensaje producido por el reader.
	 */
	public String MessageID;
	
	/**
	 * Max RSSI emitido por el reader.
	 */
	public String PeakRSSI;
	
	/**
	 * En solicitudes de estado, retorna el tipo de identificador del reader
	 * (serial, MAC Address).
	 */
	public String TipoIdentificadorReader = "";
	
	/**
	 * Identificador del reader (es asignado por el fabricante).
	 */
	public String IdentificadorReader = "";
	
	/**
	 * Estado de las antenas del reader, donde 0 significa antena desconectada,
	 * 1 antena conectada.
	 */
	public String EstadoAntenas = "";
	
	/**
	 * Versión de firmware del reader.
	 */
	public String FirmwareReader = "";
	
	/**
	 * Valor numérico que especifica el modelo del reader.
	 */
	public String NombreModeloReader = "";
	
	/**
	 * Valor numérico que especifica el estado del reader (0-Desconectado, 1-Conectado, 2-Desconocido)
	 */
	public int estadoReader;
}
