import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
//import org.junit.internal.matchers.SubstringMatcher;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.Timer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Soluciones Integrales GIS
 * Proyecto: RFID
 * @author José Fernández
 */
@SuppressWarnings("unused")
public class RFIDMain implements tcpServer.TCPServerListener, interpretadorOrdenesConsola.OrdenesDeConsola{

	private static log_file log = new log_file();
	static tcpServer servidor;
	static tcpServer GrabadorEtiquetas;
	static PostgreSQL conex;
	private static List<RequestListener> solicitudes = new ArrayList<RequestListener>();

	/**
	 * Lista de los readers y su estado (ejemplo 51|ON).
	 */
	private static Map<Integer, String> readers = new HashMap<Integer, String>();

	/**
	 * Se almacenan los códigos de operación que pertenecen a operaciones de lectura.
	 */
	private static HashSet<Integer> inicioLecturaPendientes = new HashSet<Integer>();

	/**
	 * Se mantiene una lista de código de operación/readers involucrados en la operación.
	 */
	@SuppressWarnings("rawtypes")
	private static Map<Integer,HashSet> operacionesPendientes = new HashMap<Integer,HashSet>();

	/**
	 * Para las solicitudes de fin de lectura (donde se indica que operaciones a detener bajo un
	 * código de operación) se mantiene una lista de código de operación/códigos_a_detener.
	 */
	@SuppressWarnings("rawtypes")
	private static Map<Integer,HashSet> finLecturaPendientes = new HashMap<Integer,HashSet>();

	/**
	 * Contiene todos los objetos tipo clienteReader creados durante el inicio del parser.
	 * Durante la solicitud de un reinicio de parser, se recorre esta lista y se llama
	 * al metodo stop() de cada cliente reader.
	 * @param clienteReader objetos tipo clienteReader
	 */
	private static HashSet<clienteReader> objetosClienteReader = new HashSet<clienteReader>();


	/**
	 * Contiene todos los objetos tipo Grabador de etiqueta creados durante la conexion de un grabador de etiqueta.
	 * Cuando los objetos grabadores de etiqueta se desconectan, se recorre esta lista y se llama
	 * al metodo stop() de cada cliente reader.
	 * @param clienteReader objetos tipo clienteReader
	 */
	private static HashSet<clienteReader> objetosGrabadorEtiqueta = new HashSet<clienteReader>();
	/**
	 * Especifica si un código de operación requiere, además de la respuesta a BD, una respuesta
	 * al Webservice por parte del Socket TCP
	 */
	private static Map<Integer, String> codigoRequiereRespuesta = new HashMap<Integer, String>();

	/**
	 * Controla si las operaciones que realiza el parser deben reflejarse en la consola
	 * de comandos
	 */
	private static boolean debug = true;

	/**
	 * Toma las instrucciones generadas por el usuario en la consola y ejecuta las
	 * acciones correspondientes
	 */
	private static interpretadorOrdenesConsola io = new interpretadorOrdenesConsola(new RFIDMain());

	/**
	 * Mantiene una lista de las etiquetas actualmente a la vista del lector. Para su uso con
	 * la aplicación de grabador de etiquetas.
	 */
	private static Map<String, etiqueta> etiquetasEnVista = new ConcurrentHashMap<String, etiqueta>();

	/**
	 * Almacena en un objeto tipo config_BD la configuración de la BD (dirección, nombre, usuario y contraseña)
	 */
	private static config_BD ConfiguracionBaseDeDatos = new config_BD();


	public static void main(String[] args){

		//Se utiliza la libreria Commons CLI para interpretar parametros pasados
		//mediante linea de comandos.
		CommandLineParser parser  = null;  
		CommandLine       cmdLine = null;   

		Options options = new Options();  
		options.addOption("ondebug", false, "Muestra la salida debug por pantalla");  
		options.addOption("h","help", false, "Imprime el mensaje de ayuda");

		parser  = new BasicParser();  
		try {
			cmdLine = parser.parse(options, args);	  

			if (cmdLine.hasOption("h")){  
				new HelpFormatter().printHelp(RFIDMain.class.getCanonicalName(), options );
				System.exit(0);
			}

			if (cmdLine.hasOption("ondebug")){  
				debug = true;
			}

			//Se inicia el Parser
			inicio();

		} catch (ParseException e) {
			grabarErrorLog(e.getMessage(), true);
			System.exit(0);
		}  

	}

	private static void inicio(){

		readers.clear();
		codigoRequiereRespuesta.clear();

		//Se obtiene la configuración de la BD
		ConfiguracionBaseDeDatos = ObtenerConfigBD();
		if (ConfiguracionBaseDeDatos == null) {
			System.exit(0);
		}

		//Se crea el nuevo objeto tipo PostgreSQL para el manejo de consultas
		conex = new PostgreSQL(ConfiguracionBaseDeDatos.getDb_server(),ConfiguracionBaseDeDatos.getDb_name(),ConfiguracionBaseDeDatos.getDb_user(),ConfiguracionBaseDeDatos.getDb_pass());

		//Inicia Servidor TCP para aceptar solicitudes del webservice
		String ipPuertoServidor = obtenerConfigDbParser("OTA");
		iniciarTcpServer(ipPuertoServidor);	
		
		//Inicia Servidor TCP para aceptar solicitudes del webservice
		String ipPuertoGrabador = obtenerConfigDbParser("");
		iniciarTcpGrabador(ipPuertoGrabador);	

		//Se obtienen los readers de BD
		List<config_reader> lista = new ArrayList<config_reader>();
		lista.clear();
		lista = obtenerConfigDbReaders(false);

		grabarGeneralLog("Se han encontrado " + lista.size() + " Readers en BD:", debug);
		for (config_reader un_config_db : lista) {
			grabarGeneralLog(un_config_db.getDescripcion() + " en " + un_config_db.getIp() + ", tipo " + un_config_db.getTipo() + ", ID: " + un_config_db.getIdEquipoRFID(),debug);	
		}

		//Se inicia un hilo por cada reader encontrado en BD. Solo se inician los
		//readers genericos, es decir, los que NO corresponden a grabadores de etiquetas
		if (!(lista.isEmpty())) {
			for (config_reader un_config_reader : lista) {
				clienteReader nuevoLector = new clienteReader(
						un_config_reader.getTiempoTag(),
						un_config_reader.getIp(),
						un_config_reader.getIdEquipoRFID(),
						un_config_reader.getDescripcion(),
						un_config_reader.getTiempoProrroga(),
						un_config_reader.getTipo(),
						un_config_reader.getTiempoSinActividad());
				addTagListener(nuevoLector);
				objetosClienteReader.add(nuevoLector);
				readers.put(un_config_reader.getIdEquipoRFID(), "OFF");
				Thread hilo1 = new Thread(nuevoLector);
				hilo1.setName("Start_reader_" + un_config_reader.getIdEquipoRFID() + "_Main" );
				hilo1.start();

				if (lista == null) {
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
					}
					solicitarReinicioParser(0);
				}
			}
		} else {
			grabarErrorLog("No se encontraron readers válidos en BD", debug);
		}

	}

	/**
	 * Inicia el servidor TCP en la IP y puerto especificada en BD. Si no se encuentra
	 * información en base de datos, se creará un servidor TCP en la interfaz local
	 * 127.0.0.1, y en el primer puerto disponible.
	 * @param ipPuerto Cadena que representa la dirección IP y puerto donde establecer
	 * el servidor TCP (ejemplo: 192.168.1.1:8080).
	 */
	private static void iniciarTcpServer(String ipPuerto){

		String[] params = ipPuerto.split("\\:");

		//Iniciamos el servidor TCP para recibir solicitudes del webservice
		try {
			servidor = new tcpServer(new RFIDMain(),"servidor");

			if ((params[0].matches("")) || (params[1].matches(""))) {
				grabarErrorLog("Error al iniciar el servidor TCP de WebService: No se encontró IP:PUERTO válido en BD", debug);
				servidor.setIPEscucha("");
				servidor.setPuertoEscucha(0);
				servidor.startTcpServer();
				grabarGeneralLog("Estableciendo servidor TCP en " + servidor.getIPEscucha() + ":" + servidor.getPuertoEscucha(),debug);
				return;
			} else {
				servidor.setIPEscucha(params[0]);
				servidor.setPuertoEscucha(Integer.parseInt(params[1]));
				servidor.startTcpServer();
			}

		} catch (UnknownHostException e) {
			grabarErrorLog("Error al iniciar el servidor TCP de WebService: No pudo determinarse la dirección " + ipPuerto + " en la maquina local", debug);
		} catch (IllegalArgumentException e) {
			grabarErrorLog("Error al iniciar el servidor TCP de WebService: El puerto de conexión se encuentra fuera del rango válido", debug);
		} catch (IOException e) {
			grabarErrorLog("Error al iniciar el servidor TCP de WebService: Error de I/O. (" + e.getMessage() + ")", debug);
		} 
	}
	
	/**
	 * Inicia el servidor TCP en la IP y puerto especificada en BD. Si no se encuentra
	 * información en base de datos, se creará un servidor TCP en la interfaz local
	 * 127.0.0.1, y en el primer puerto disponible.
	 * @param ipPuerto Cadena que representa la dirección IP y puerto donde establecer
	 * el servidor TCP (ejemplo: 192.168.1.1:8080).
	 */
	private static void iniciarTcpGrabador(String ipPuerto){

		String[] params = ipPuerto.split("\\:");

		//Iniciamos el servidor TCP para recibir solicitudes del webservice
		try {
			GrabadorEtiquetas = new tcpServer(new RFIDMain(),"GrabadorEtiquetas");

			if ((params[0].matches("")) || (params[1].matches(""))) {
				grabarErrorLog("Error al iniciar el servidor TCP de Grabador de etiquetas: No se encontró IP:PUERTO válido en BD", debug);
				GrabadorEtiquetas.setIPEscucha("");
				GrabadorEtiquetas.setPuertoEscucha(0);
				GrabadorEtiquetas.startTcpServer();
				grabarGeneralLog("Estableciendo servidor Grabador de etiquetas en " + servidor.getIPEscucha() + ":" + servidor.getPuertoEscucha(),debug);
				return;
			} else {
				GrabadorEtiquetas.setIPEscucha(params[0]);
				GrabadorEtiquetas.setPuertoEscucha(Integer.parseInt(params[1]));
				GrabadorEtiquetas.startTcpServer();
			}

		} catch (UnknownHostException e) {
			grabarErrorLog("Error al iniciar el servidor TCP de Grabador de etiquetas: No pudo determinarse la dirección " + ipPuerto + " en la maquina local", debug);
		} catch (IllegalArgumentException e) {
			grabarErrorLog("Error al iniciar el servidor TCP de Grabador de etiquetas: El puerto de conexión se encuentra fuera del rango válido", debug);
		} catch (IOException e) {
			grabarErrorLog("Error al iniciar el servidor TCP de Grabador de etiquetas: Error de I/O. (" + e.getMessage() + ")", debug);
		} 
	}

	/**
	 * Obtiene la configuración de la BD
	 * @return Un objeto config_BD con la configuración de la base de datos
	 */
	private static config_BD ObtenerConfigBD(){

		config_BD config = new config_BD();

		try
		{
			String archivo;
			archivo="config/config.xml";

			File folder = new File("config");

			if (!folder.exists()) {
				folder.mkdir();
				grabarErrorLog("No se encontró el directorio /config. Se creará automáticamente", debug);
			}

			folder = new File(archivo);
			if (!folder.exists()) {
				String contenido;
				contenido="<config>\n <db_server></db_server>\n <db_name></db_name>\n <db_user></db_user>\n <db_pass></db_pass>\n</config>";
				File new_archivo=new File(archivo);
				FileWriter escribir=new FileWriter(new_archivo,true);
				escribir.write(contenido);
				escribir.close();
				grabarErrorLog("No se encontró el archivo de configuración config.xml. Se creará automáticamente", debug);
				return null;
			}

			SAXBuilder builder = new SAXBuilder();
			File xmlFile = new File(archivo);
			Document document = (Document) builder.build( xmlFile );
			Element rootNode = document.getRootElement();

			try {
				config.setDb_name(obtenerNodo(rootNode,"db_name"));
				config.setDb_pass(obtenerNodo(rootNode,"db_pass"));
				config.setDb_server(obtenerNodo(rootNode,"db_server"));
				config.setDb_user(obtenerNodo(rootNode,"db_user"));
				return config;

			} catch (NoElementFoundException e) {
				grabarErrorLog(e.getMessage(), debug);
			} 

			return null;

		}catch ( IOException io ) {
			grabarErrorLog("Se ha encontrado un error al abrir el archivo config.xml: " + io.getMessage(), true);
		}catch ( JDOMException jdomex ) {
			grabarErrorLog("El formato del archivo config.xml no es correcto: " + jdomex.getMessage(), true);
		}

		return null;
	}

	private static String obtenerNodo(Element rootNode, String parametro) throws NoElementFoundException, NullPointerException{

		String valor = "";	

		valor = rootNode.getChild(parametro).getValue();

		try {
			if (valor.matches("")) {
				throw new NoElementFoundException("Parámetro " + parametro + " vacío en el archivo config.xml");
			} else {
				return valor;
			}
		} catch (NullPointerException e) {
			throw new NoElementFoundException("Parámetro " + parametro + " no encontrado en el archivo config.xml");
		}	
	}

	/**
	 * Obtiene la lista de los readers cargados en BD con su configuracion
	 * @return lista de objetos config_db
	 */
	private static List<config_reader> obtenerConfigDbReaders(boolean grabador){
		List<config_reader> lista = new ArrayList<config_reader>();

		try {
			ResultSet resultado = conex.consulta("SELECT * FROM equipos_rfid");

			while(resultado.next()) {
				config_reader r = new config_reader();
				r.setIdEquipoRFID(resultado.getInt("id_equ_rfid"));
				r.setTipo(resultado.getInt("tip_equ_rfid"));
				r.setDescripcion(resultado.getString("des_equ_rfid"));

				String ip = resultado.getString("ip_equ_rfid");
				if (ip.contains(":")) {
					ip = ip.substring(0, ip.lastIndexOf(":"));
					r.setIp(ip);
				} else {
					r.setIp(ip);
				}

				r.setTiempoTag(resultado.getInt("tie_tag"));
				r.setTiempoProrroga(resultado.getInt("tie_pro"));
				r.setEsGrabadorEtiquetas(resultado.getBoolean("es_gra_et"));
				r.setTiempoSinActividad(resultado.getInt("tie_sin_act"));
				if (r.isEsGrabadorEtiquetas() == grabador) {
					lista.add(r);
				}	
			}

			resultado.close();
			return lista;

		} catch(Exception e) {
			grabarErrorLog("Se encontró un error al obtener la configuración de los readers en BD: " + e.getMessage(), true);
			lista.clear();
			return lista;
		}
	}

	/**
	 * Obtiene configuración adicional para el inicio del parser
	 * @return lista de objetos config_db
	 */
	private static String obtenerConfigDbParser(String tipo){

		String parametro;

		
		if (tipo.matches("OTA")) {
			parametro = "RFID_OTA";
		} else {
			parametro = "RFID_GE";
		}
		

		String ipPuerto = "";

		ResultSet resultado = conex.consulta("SELECT * FROM config  WHERE parametro = '" + parametro + "'");

		try {
			while(resultado.next()) {
				ipPuerto = resultado.getString("valor");
			}

			resultado.close();
		} catch(Exception e) {
			grabarErrorLog("Se encontró un error al obtener la configuración del parser en BD: " + e.getMessage(), true);
			ipPuerto = "";
			return ipPuerto;
		}

		return ipPuerto;			
	}

	/**
	 * Obtiene el comando OTA recibido y lo procesa con el uso de la clase
	 * procesarSolicitud
	 * @param comando comando OTA
	 */
	private static void procesarMensajeOTA(String comando, String idCliente,Socket remote){

		procesarSolicitud x = new procesarSolicitud();
		x.procesar(comando);

		if (x.getError().matches("")) {
		
			if (x.getComando().matches("HANDHELD")) {
				servidor.sendData(idCliente, "OK \r\n", remote);
				EnviarMensajeHandheld(x);
				return;
			} 
				
			forzarActualizacionEstadoReaders();
			String error = revisarEstadoReaders(x);

			if (error.matches("")) {
				//Si el comando requiere una respuesta hacia el webService (por el Socket TCP)
				//no se envia 'OK', sino que se envía la respuesta
				if (x.getRequiereRespuesta().matches("FALSE")) {
					grabarOTA(comando + " ;OK",false);
					servidor.sendData(idCliente, "OK \r\n",remote);
				}		
				generarEventosEnComandoOTA(x);
			} else {
				servidor.sendData(idCliente, error,remote);
				grabarOTA(comando + ", " + error,false);
			}	
			
		}else{
			grabarOTA(comando + ", " + x.getError(), true);
			servidor.broadcastMessage(x.getError() + "\r\n");
		}
	}
	
	/**
	 * Fuerza la consulta del estado de los readers para que estos actualicen su estado justo
	 * antes de procesar un comando nuevo.
	 */
	private static void forzarActualizacionEstadoReaders(){
		for (clienteReader cr : objetosClienteReader) {
			etiqueta tag = new etiqueta();
			tag = cr.consultarEstadoReader();
			tag = null;
		}
	}

	/**
	 * Cuando el XML proveniente del webservice proviene de una lectura de tag de un equipo
	 * handheld, se envia el tag a todas las lstas creadas para que sea tomado en cuenta por
	 * las que lo tienen in cluidas en su filtro.
	 */
	private static void EnviarMensajeHandheld(procesarSolicitud x){
		
		switch (x.getTipoHandheld()) {
		
		//Nuevo tag leido desde handheld
		case 0:
			etiqueta tag = new etiqueta();
			tag.idLector = x.getIdHandheld();
			tag.idAntena = x.getAntenaHandheld();
			tag.epc = x.getEpcHandheld();
			tag.idMensaje = 1;
			
			try {
				SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				tag.ultimaVezFecha = dt.parse(x.getHoraHandheld());
			} catch (java.text.ParseException e) {
				grabarErrorLog("Error al convertir la hora de la etiqueta leida por Handheld: " + e.getMessage(), debug);
			}
				
			nuevoMensajeHandheld(x.getTipoHandheld(), x.getIdHandheld(), tag);	
			break;
	
		default:
			nuevoMensajeHandheld(x.getTipoHandheld(), x.getIdHandheld(), null);
			break;
		}
		
	}
	
	/**
	 * Dada una nueva solicitud, esta función verifica que los readers involucrados en la
	 * misma existan y esten en linea y que los códigos de operación a detener
	 * se encuentren actualmente activos. Verifica además que el codigo de operacion de la
	 * solicitud actual no este ya actualmente en uso.
	 * @param x objeto tipo procesar solicitud
	 * @return verdadera si todos los readers son validos y estan en linea, false
	 * lo contrario
	 */
	@SuppressWarnings("rawtypes")
	private static String revisarEstadoReaders(procesarSolicitud x){

		HashSet<Integer> readersNoConectados = new HashSet<Integer>();
		HashSet<Integer> codigosOperacion = new HashSet<Integer>();
		readersNoConectados.clear();
		codigosOperacion.clear();
		String error = "";

		try {

			//Primero verificamos que no existan operaciones actuales con el mismo
			//codigo de operación	
			if (operacionesPendientes.containsKey(x.getIdOperacion())) {
				error = "Ya existe una operación en proceso con el mismo código: " + x.getIdOperacion();
				grabarErrorLog(error, true);
				return error;
			}

			if (x.getComando().matches("ESTADO")) {
				if (!x.getIdsEstado().matches("")) {

					for (String id : x.getIdsEstado().split(",")) {
						if (readers.containsKey(Integer.parseInt(id))) {
							if (readers.get(Integer.parseInt(id)).matches("OFF")) {
								readersNoConectados.add(Integer.parseInt(id));
							}
						} else{
							readersNoConectados.add(Integer.parseInt(id));
						}
					}		
				} else {
					Iterator it = readers.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry e = (Map.Entry)it.next();
						if (e.getValue().toString().matches("OFF")) {							
							readersNoConectados.add((Integer) e.getKey());
						}
					}	
				}
			}

			if (x.getComando().matches("LECTURA")) {		
				if (x.getTipoLectura().matches("ON")) {		
					for (String filtro : x.getFiltros()) {

						String[] s = filtro.split("\\|");
						String[] r = s[0].split(",");

						for (String reader : r) {
							if (readers.containsKey(Integer.parseInt(reader))) {
								if (readers.get(Integer.parseInt(reader)).matches("OFF")) {
									readersNoConectados.add(Integer.parseInt(reader));
								}
							}else {
								readersNoConectados.add(Integer.parseInt(reader));
							}
						}		
					}

				} else {
					String[] codigos = x.getFiltroLecturaOFF().split(",");
					for (String cod : codigos) {
						if (!(operacionesPendientes.containsKey(Integer.parseInt(cod)))) {
							codigosOperacion.add(Integer.parseInt(cod));
						}
					}
				}
			}

			String problema = "";

			if ((readersNoConectados.isEmpty()) && (codigosOperacion.isEmpty())) {
				return "";
			}

			if (!(readersNoConectados.isEmpty())) {

				for (Integer i : readersNoConectados) {
					problema = problema + " " + i.toString();
				}
				error = "Los readers:" + problema + " no se encuentran conectados para la solicitud " + x.getIdOperacion();
				grabarErrorLog(error, true);
				return error;
			}

			if (!(codigosOperacion.isEmpty())) {

				for (Integer i : codigosOperacion) {
					problema = problema + " " + i.toString();
				}
				error = "No existen operaciones pendientes con id:" + problema;
				grabarErrorLog(error, true);
				return error;
			}

			return "";

		} catch (NumberFormatException e) {
			error = "Ocurrió un problema durante la decodificación del mensaje:  " + e.getMessage();
			grabarErrorLog(error, true);
			return error;
		}

	}

	/**
	 * Toma el comando OTA procesado y genera los eventos correspondientes segun su
	 * tipo
	 * @param x es un objeto tipo procesarSolicitud
	 */
	@SuppressWarnings("rawtypes")
	private static void generarEventosEnComandoOTA(procesarSolicitud x){

		//Cada nuevo comando ota (que contiene un solo codigo de operacion)
		//involucra a 1 o mas readers. Se mantiene la lista operacionesPendientes
		//que indica que readers estan asociados a un codigo de operacion en concreto
		//(cod_ope|{r1,r2,r3...rn}). De esta forma, se puede determinar que readers
		//deben culminar cierto proceso para darle un fin a esa operación.
		//Se guarda además una lista que indica que códigos de operación requieren respuesta
		//al Webservice.

		codigoRequiereRespuesta.put(x.getIdOperacion(), x.getRequiereRespuesta());
		HashSet<Integer> idsinvolucrados = new HashSet<Integer>();
		idsinvolucrados.clear();

		//Nueva solicitud de estado de readers
		if (x.getComando().matches("ESTADO")) {
			if (!x.getIdsEstado().matches("")) {
				operacionesPendientes.put(x.getIdOperacion(), idsinvolucrados);
				for (String id : x.getIdsEstado().split(",")) {
					idsinvolucrados.add(Integer.parseInt(id));
					solicitarEstadoReader(id, x.getIdOperacion());
				}
				operacionesPendientes.put(x.getIdOperacion(), idsinvolucrados);

			} else {
				//Si la lista de readers viene vacia, debe hacerse una consulta sobre todos
				//Se recorre el mapa de readers
				Iterator it = readers.entrySet().iterator();

				while (it.hasNext()) {
					Map.Entry e = (Map.Entry)it.next();
					idsinvolucrados.add((Integer) e.getKey());
				}
				operacionesPendientes.put(x.getIdOperacion(), idsinvolucrados);
				solicitarEstadoReader("", x.getIdOperacion());
			}
		}

		//Nueva solicitud de lectura en readers
		if (x.getComando().matches("LECTURA")) {

			if (x.getTipoLectura().matches("ON")) {
				solicitarInicioLecturaReader(x);

				//Si es una solicitud de lectura, se almacena ademas
				//en el archivo OTA_pendientes.xml
				//insertarOTApendiente(x.getNodo());

			} else {
				solicitarFinLecturaReader(x);
			}
		}

		//Nueva solicitud para consultar operaciones pendientes
		if (x.getComando().matches("CONSULTA")) {
			if (x.getTipoConsulta().matches("SIMPLE")) {
				solicitudConsultaOperacionesPendientes(x.getCodigosOpConsulta(), x.getIdOperacion());	
			} else {
				solicitudConsultaOperacionesPendientes("0", x.getIdOperacion());
			}
		}

		//Nueva solicitud de configuracion (reset de parser)
		if (x.getComando().matches("CONFIGURACION")) {
			if (x.getTipoComando().matches("COMANDO")) {
				if (x.getAccionComando().matches("RESET")) {
					solicitarReinicioParser(x.getIdOperacion());
				}		
			}
		}	
	}

	/**
	 * En el archivo OTA_pendientes.xml se guardan todos los comandos OTA
	 * de solicitudes de lectura que se hayan recibido. Se actualiza el
	 * tiempo de la lectura
	 */
	private static void actualizarOTA_pendientes(){
		String archivo ="config/OTA_pendientes.xml";
		File folder = new File("OTA_pendientes");	
		folder = new File(archivo);

		//Si el archivo no existe, se crea
		if (!folder.exists()) {
			String contenido;
			contenido="<solicitudes_pendientes></solicitudes_pendientes>";

			File new_archivo = new File(archivo);
			FileWriter escribir;
			try {
				escribir = new FileWriter(new_archivo,true);
				escribir.write(contenido);
				escribir.close();
			} catch (IOException e) {
				grabarErrorLog("Error al crear el archivo OTA_pendientes.xml, " + e.getMessage(), false);
			}

		} else{
			//Si existe, se itera entra las solicitudes de lectura y se actualiza
			//el tiempo de lectura

			org.w3c.dom.Document doc = null;  
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();  
			DocumentBuilder db;
			try {
				db = dbf.newDocumentBuilder();
				doc = db.parse("config/OTA_pendientes.xml");

				NodeList solicitudes = doc.getFirstChild().getChildNodes();

				for(int i=0; i<solicitudes.getLength(); i++){
					Node solicitud = solicitudes.item(i);
					NodeList info = solicitud.getChildNodes();

					for(int j=0; j<info.getLength(); j++){
						Node parametros = info.item(j);

						if (parametros.getNodeName().matches("parametros")) {
							NodeList parametro = parametros.getChildNodes();

							for(int k=0; k<parametro.getLength(); k++){

								Node nombreparametro = parametro.item(k);

								if (nombreparametro.getNodeName().matches("tiempo")) {

									int tiempo = Integer.parseInt(nombreparametro.getTextContent());

									if (tiempo <= 0)  {
										tiempo = 30;
									} else {
										tiempo = tiempo - 5;
									}
									nombreparametro.setTextContent(String.valueOf(tiempo));
								}	
							}
						}
					}	
				}

				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult("config/OTA_pendientes.xml");
				transformer.transform(source, result);

			} catch (ParserConfigurationException e) {
				grabarErrorLog("actualizarOTA_pendientes, " + e.getMessage() , false);	
			} catch (SAXException e) {
				grabarErrorLog("actualizarOTA_pendientes, " + e.getMessage() , false);
			} catch (IOException e) {
				grabarErrorLog("actualizarOTA_pendientes, " + e.getMessage() , false);
			} catch (TransformerConfigurationException e) {
				grabarErrorLog("actualizarOTA_pendientes, " + e.getMessage() , false);
			} catch (TransformerException e) {
				grabarErrorLog("actualizarOTA_pendientes, " + e.getMessage() , false);
			}   

		}
	}

	/**
	 * Toma los comandos pendientes del archivo OTA_pendientes.xml y los reinserta
	 * para que sean enviados a todos los readers
	 */
	private static void reenviarOTApendiente(){
		org.w3c.dom.Document doc = null;  
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			doc = db.parse("config/OTA_pendientes.xml"); 

			Node rootNode = doc.getFirstChild();
			NodeList solicitudes = rootNode.getChildNodes();

			for (int i = 0; i < solicitudes.getLength(); i++) {
				String xmlaString = nodoAstring(solicitudes.item(i));
				//procesarMensajeOTA(xmlaString);
			}

		} catch (ParserConfigurationException e) {
			grabarErrorLog("reenviarOTApendiente, " + e.getMessage(), false);
		} catch (SAXException e) {
			grabarErrorLog("reenviarOTApendiente, " + e.getMessage(), false);
		} catch (IOException e) {
			grabarErrorLog("reenviarOTApendiente, " + e.getMessage(), false);
		}   
	}

	/**
	 * Añade la nueva solicitud de lectura al archivo OTA_pendientes.xml
	 * @param nodo Elemento tipo Node a insertar
	 */
	private static void insertarOTApendiente(org.w3c.dom.Node nodo){

		org.w3c.dom.Document doc = null;  
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();  

		try {  

			DocumentBuilder db = dbf.newDocumentBuilder();   
			doc = db.parse("config/OTA_pendientes.xml");  

			//Se obtiene el cod_ope del nodo a insertar
			NodeList temp = nodo.getChildNodes();
			String cod_ope = temp.item(1).getTextContent();

			//Si un comando con el mismo codigo de operacion ya se encuentra en
			//la lista, no se agrega
			boolean yaEnLista = false;
			Node rootNode = doc.getFirstChild();
			NodeList solicitudes = rootNode.getChildNodes();


			for (int i = 0; i < solicitudes.getLength(); i++) {
				NodeList n = solicitudes.item(i).getChildNodes();

				if (n.item(1).getTextContent().matches(cod_ope)){
					yaEnLista = true;
				}	
			}

			if (!yaEnLista) {
				org.w3c.dom.Node imported = doc.importNode(nodo, true);
				doc.getFirstChild().appendChild(imported);

				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);

				StreamResult result = new StreamResult("config/OTA_pendientes.xml");
				transformer.transform(source, result);
			}

		} catch (Exception e) {  
			grabarErrorLog("insertarOTApendiente, " + e.getMessage(), false);
		}  
	}

	/**
	 * Remueve la solicitud de lectura identificada con un código de operación
	 * de la lista OTA_pendientes.xml
	 * @param codigoOperacion Entero que representa el código de operación de lectura a eliminar
	 */
	private static void removerOTApendiente(int codigoOperacion){

		org.w3c.dom.Document doc = null;  
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();  

		try {  

			DocumentBuilder db = dbf.newDocumentBuilder();   
			doc = db.parse("config/OTA_pendientes.xml");  

			Node rootNode = doc.getFirstChild();
			NodeList solicitudes = rootNode.getChildNodes();

			for (int i = 0; i < solicitudes.getLength(); i++) {
				NodeList detallesSolicitud = solicitudes.item(i).getChildNodes();

				if (detallesSolicitud.item(1).getTextContent().matches(String.valueOf(codigoOperacion))) {
					rootNode.removeChild(solicitudes.item(i));
				}
			}

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);

			StreamResult result = new StreamResult("config/OTA_pendientes.xml");
			transformer.transform(source, result);

		} catch (Exception e) {  
			grabarErrorLog("removerOTApendiente, " + e.getMessage(), false);
		}

	}

	/**
	 * Tranforma un objeto Node a una String
	 * @param node objeto tipo Node a transformar
	 * @return contenido del nodo en String
	 */
	private static String nodoAstring(Node node) {
		StringWriter sw = new StringWriter();
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException te) {
			grabarErrorLog("nodoAstring: " + te.getMessage(), false);
			return "";
		}
		return sw.toString();
	}

	/**
	 * Inserta en el archivo lecturas.xml una nueva etiqueta segun su código de operación
	 * 
	 * @param tag etiqueta a almacenar
	 * @param codigoOperacion código de la operación de la lectura que originó la etiqueta
	 */
	private static void insertarTagArchivoLecturas(etiqueta tag, int codigoOperacion){

		String archivo ="config/lecturas.xml";
		File folder = new File("lecturas");	
		folder = new File(archivo);

		//Si el archivo no existe, se crea
		if (!folder.exists()) {
			String contenido;
			contenido="<lecturas></lecturas>";

			File new_archivo = new File(archivo);
			FileWriter escribir;
			try {
				escribir = new FileWriter(new_archivo,true);
				escribir.write(contenido);
				escribir.close();
			} catch (IOException e) {
				grabarErrorLog("Error al crear el archivo lecturas.xml, " + e.getMessage(), false);
			}

		} else {
			org.w3c.dom.Document doc = null;  
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();  
			DocumentBuilder db;

			try {

				db = dbf.newDocumentBuilder();
				doc = db.parse("config/lecturas.xml");
				org.w3c.dom.Element rootElement = doc.getDocumentElement();

				//Creo la nueva entrada en el XML con el id de operacion
				org.w3c.dom.Element nuevoCodigo = doc.createElement("cod_op");
				org.w3c.dom.Element nuevoTag = doc.createElement("nuevoTag");

				nuevoCodigo.setAttribute("valor", String.valueOf(codigoOperacion));

				//Aï¿½ado todos los mienbros de etiqueta en el XML con su valor

				//epc
				org.w3c.dom.Element epc = doc.createElement("epc");
				epc.setTextContent(tag.epc);

				//antena
				org.w3c.dom.Element idAntena = doc.createElement("idAntena");
				idAntena.setTextContent(String.valueOf(tag.idAntena));

				//ultima fecha visto
				org.w3c.dom.Element ultimaVezFecha = doc.createElement("ultimaVezFecha");
				SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				ultimaVezFecha.setTextContent(String.valueOf(dt.format(tag.ultimaVezFecha)));

				//Id reader
				org.w3c.dom.Element idLector = doc.createElement("idLector");
				idLector.setTextContent(String.valueOf(tag.idLector));

				nuevoTag.appendChild(epc);
				nuevoTag.appendChild(idAntena);
				nuevoTag.appendChild(ultimaVezFecha);
				nuevoTag.appendChild(idLector);

				//Recorro la lista para ver si ya esta
				Node root = doc.getFirstChild();
				NodeList codigosOperacion = root.getChildNodes();

				boolean seencuentra = false;
				for (int i = 0; i < codigosOperacion.getLength(); i++) {

					org.w3c.dom.Element f = (org.w3c.dom.Element)codigosOperacion.item(i);
					String valor = f.getAttribute("valor");

					if (valor.matches(String.valueOf(codigoOperacion))) {
						f.appendChild(nuevoTag);
						seencuentra = true;
					}

				}

				if (seencuentra == false) {
					nuevoCodigo.appendChild(nuevoTag);
					rootElement.appendChild(nuevoCodigo);
				}

				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult("config/lecturas.xml");
				transformer.transform(source, result);

			} catch (ParserConfigurationException e) {
				grabarErrorLog("respuestaSolicitudLectura, " + e.getMessage() , false);
			} catch (SAXException e) {
				grabarErrorLog("respuestaSolicitudLectura, " + e.getMessage() , false);
			} catch (IOException e) {
				grabarErrorLog("respuestaSolicitudLectura, " + e.getMessage() , false);
			} catch (TransformerConfigurationException e) {
				grabarErrorLog("respuestaSolicitudLectura, " + e.getMessage() , false);
			} catch (TransformerException e) {
				grabarErrorLog("respuestaSolicitudLectura, " + e.getMessage() , false);
			}
		}

	}

	/**
	 * Elimina del archivo lecturas.xml, todos los tags bajo un mismo código de operación
	 * @param codigoOperacion Entero que representa el código de la operación
	 */
	private static void removerTagsArchivoLecturas(int codigoOperacion){

		org.w3c.dom.Document doc = null;  
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();  
		DocumentBuilder db;

		try {
			db = dbf.newDocumentBuilder();
			doc = db.parse("config/lecturas.xml");

			Node rootNode = doc.getFirstChild();
			NodeList operaciones = rootNode.getChildNodes();

			for (int i = 0; i < operaciones.getLength(); i++) {
				org.w3c.dom.Element f = (org.w3c.dom.Element)operaciones.item(i);

				String cod = f.getAttribute("valor").toString();
				if (cod.matches(String.valueOf(codigoOperacion))) {
					rootNode.removeChild(operaciones.item(i));
				}
			}

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult("config/lecturas.xml");
			transformer.transform(source, result);


		} catch (ParserConfigurationException e) {
			grabarErrorLog("removerTagsArchivoLecturas, " + e.getMessage() , false);
		} catch (SAXException e) {
			grabarErrorLog("removerTagsArchivoLecturas, " + e.getMessage() , false);	
		} catch (IOException e) {
			grabarErrorLog("removerTagsArchivoLecturas, " + e.getMessage() , false);
		} catch (TransformerConfigurationException e) {
			grabarErrorLog("removerTagsArchivoLecturas, " + e.getMessage() , false);
		} catch (TransformerException e) {
			grabarErrorLog("removerTagsArchivoLecturas, " + e.getMessage() , false);
		}
	}

	/**
	 * Escribe un nuevo mensaje en el archivo log_errores.txt.
	 * @param descripcion Mensaje a escribir.
	 * @param desplegarMensaje True si el mensaje debe desplegarse en pantalla, si False
	 * el mensaje no se mostrará en pantalla.
	 */
	private static void grabarErrorLog(String descripcion, boolean desplegarMensaje){
		if (desplegarMensaje) {
			System.out.println(descripcion);
		}
		comprobarMedidaParaCompresion("log/log_errores.txt");
		log.escribir(descripcion, "log/log_errores.txt");
	}

	/**
	 * Escribe un nuevo mensaje en el archivo log_general.txt.
	 * @param descripcion Mensaje a escribir.
	 * @param desplegarMensaje True si el mensaje debe desplegarse en pantalla, si False
	 * el mensaje no se mostrará en pantalla.
	 */
	private static void grabarGeneralLog(String descripcion, boolean desplegarMensaje){
		if (desplegarMensaje) {
			System.out.println(descripcion);
		}
		comprobarMedidaParaCompresion("log/log_general.txt");
		log.escribir(descripcion, "log/log_general.txt");
	}

	/**
	 * Escribe un nuevo mensaje en el archivo log_SQL.txt.
	 * @param descripcion Mensaje a escribir.
	 * @param desplegarMensaje True si el mensaje debe desplegarse en pantalla, si False
	 * el mensaje no se mostrará en pantalla.
	 */
	private static void grabarSQL(String descripcion, boolean desplegarMensaje){
		if (desplegarMensaje) {
			System.out.println(descripcion);
		}
		comprobarMedidaParaCompresion("log/log_SQL.txt");
		log.escribir(descripcion, "log/log_SQL.txt");
	}

	/**
	 * Escribe un nuevo mensaje en el archivo log_OTA.txt.
	 * @param descripcion Mensaje a escribir.
	 * @param desplegarMensaje True si el mensaje debe desplegarse en pantalla, si False
	 * el mensaje no se mostrará en pantalla.
	 */
	private static void grabarOTA(String descripcion, boolean desplegarMensaje){
		if (desplegarMensaje) {
			System.out.println(descripcion);
		}
		comprobarMedidaParaCompresion("log/log_OTA.txt");
		log.escribir(descripcion, "log/log_OTA.txt");
	}

	private static void comprobarMedidaParaCompresion(String ruta){	

		File file =new File(ruta);
		double bytes = file.length();
		double megabytes = bytes/1048576;
		String nombre = file.getName();
		nombre = nombre.substring(0,nombre.indexOf("."));

		if (megabytes > 100) {
			SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
			zip_files comprimirArchivos = new zip_files(log + "/Historico", ruta, nombre + " " + dt.format(new Date()));
			comprimirArchivos.comprimir();
			file.delete();
		}
	}

	/**
	 * El procedimiento almacenaTagDB toma los objetos tipo etiqueta (que contienen información
	 * del EPC leído, reader, antena, tipo de operación) y construye el query correspondiente
	 * a insertar en la base de datos.
	 * @param tag Objeto tipo etiqueta producido en operaciones de tag in/out, inventarios, readers
	 * conectado/desconectado, etc
	 */
	private static void almacenaTagDB(etiqueta tag){

		SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		String query = "";

		//Operaciones de tag_in, tag_out o tags leídos en operaciones de lectura
		if (tag.idMensaje == 1) {
			
			//Operación de tag in o tag out
			if (!(tag.tipoOperacion == 2)) {
				query = "SELECT f_almacena_lectura(0, " + tag.tipoOperacion + "" +
						", '" + dt.format(tag.ultimaVezFecha) + "', " + tag.idAntena + 
						", " + tag.idLector + ", '" + tag.epc + "')";

				for (clienteReader s : objetosGrabadorEtiqueta) {
					if (s.idReader == tag.idLector) {
						enviarAGrabadorEtiquetas(tag);
						query = "";
						break;
					}
				}
			}

			//Operación de inventario
			if (tag.tipoOperacion == 2) {

				query = "SELECT f_almacena_lectura(" + tag.idInventario + "," + "2" + "" +
						", '" + dt.format(tag.ultimaVezFecha) + "', " + tag.idAntena + 
						", " + tag.idLector + ", '" + tag.epc + "')";		
			}
		}	

		//Respuesta de una solicitud de estado de reader
		if (tag.idMensaje == 2) {

			String mensaje = tag.descripcionMensaje + " " + tag.idLector + ", " + tag.TipoIdentificadorReader + ": " + tag.IdentificadorReader + ", " + 
					"Antenas: " + tag.EstadoAntenas + ", Firmware: " + tag.FirmwareReader + ", Modelo: " + tag.NombreModeloReader + ";";

			comprobarEnvioRespuestaWebService(mensaje,tag.idInventario, false);
			GrabadorEtiquetas.broadcastMessage(mensaje);

		}

		//Notificación de fin de solicitud de estado readers
		if (tag.idMensaje == 3) {

			comprobarEnvioRespuestaWebService(tag.descripcionMensaje,tag.idInventario, true);
			GrabadorEtiquetas.broadcastMessage(tag.descripcionMensaje);

			/**
			 * TODO Por definir función de BD para fin de operación de lectura
			 */		
		}

		//Notificación de fin de lectura
		if (tag.idMensaje == 4) {

			comprobarEnvioRespuestaWebService(tag.descripcionMensaje,tag.idInventario, true);
			query = "SELECT f_finaliza_inventario(" + tag.idInventario + ")";

			/**
			 * FIXME se envian los tags del archivo de lecturas (?)
			 */	
		}

		//Notificación de fin de operación de detener lectura
		if (tag.idMensaje == 5) {

			comprobarEnvioRespuestaWebService(tag.descripcionMensaje,tag.idInventario,true);

			/**
			 * TODO por definir funcion de BD a llamar
			 */		
		}

		//Notificación de fin de consulta de operaciones pendientes.
		if (tag.idMensaje == 6) {

			comprobarEnvioRespuestaWebService(tag.descripcionMensaje,tag.idInventario, true);

			/**
			 * TODO por definir funcion de BD a llamar
			 */
		}

		//Reinicio Parser
		if (tag.idMensaje == 7) {

			comprobarEnvioRespuestaWebService(tag.descripcionMensaje, tag.idInventario, true);

			/**
			 * TODO por definir funcion de BD a llamar
			 */
		}

		//Actualización estado de reader o cambio de antenas
		if (tag.idMensaje == 8) {

			tag.EstadoAntenas = tag.EstadoAntenas.replace("", ",");
			tag.EstadoAntenas = tag.EstadoAntenas.replace("-","2");
			tag.EstadoAntenas = tag.EstadoAntenas.replaceFirst(",", "");
			tag.EstadoAntenas = tag.EstadoAntenas.substring(0,tag.EstadoAntenas.length()-1);
			query = "SELECT f_actualiza_estado_reader(" + tag.idLector + "," + tag.estadoReader + ",ARRAY[" + tag.EstadoAntenas + "]);";
		}

		if (!query.matches("")) {
			try {
				conex.insertarEnCola(query);
			} catch (NullPointerException e) {
				grabarSQL(query + ", " + e.getMessage(),debug);
			} catch (InterruptedException e) {
				grabarSQL(query + ", " + e.getMessage(),debug);
			}
		}
	}


	public static void ResultadoInsercionBD(String query, String resultado){
		grabarSQL(query + ", " + resultado,debug);
	}

	private static void enviarAGrabadorEtiquetas(etiqueta tag){

		String m;
		if (tag.tipoOperacion == 0) {
			m = ">" + tag.epc + ";" + tag.idLector + ";" + tag.idAntena + ";" + "IN" + "<";

			etiquetasEnVista.put(tag.epc, tag);	

		} else {
			m = ">" + tag.epc + ";" + tag.idLector + ";" + tag.idAntena + ";" + "OUT" + "<";

			if (etiquetasEnVista.containsKey(tag.epc)) {
				etiquetasEnVista.remove(tag.epc);
			}

		}

		GrabadorEtiquetas.broadcastMessage(m);

	}

	/**
	 * Comprueba si la respuesta requiere el envío de un mensaje al webservice.
	 * @param mensaje Mensaje a enviar.
	 * @param cod_ope Entero que representa el código de operación.
	 * @param esFin Si la operación asociada finalizó por completo.
	 */
	private static void comprobarEnvioRespuestaWebService(String mensaje, int cod_ope, boolean esFin){
		if (codigoRequiereRespuesta.containsKey(cod_ope)) {
			if (codigoRequiereRespuesta.get(cod_ope).matches("TRUE")) {

				if (esFin) {
					servidor.broadcastMessage(mensaje + " Fin operación: " + cod_ope + "\r\n");	
					codigoRequiereRespuesta.remove(cod_ope);				
				} else {
					servidor.broadcastMessage(mensaje);
				}		
			}	
		}

		//controla si los mensajes se muestran en pantalla
		if (debug) {
			System.out.println(mensaje);	
		}

	}

	@Override
	/**
	 * Este método se ejecuta de forma asíncrona cada vez que se recibe una nueva
	 * solicitud de operación desde el WebService o consulta del grabador de etiquetas.
	 */
	public void onMessageReceived(byte[] b, String id, String identificador,Socket remote) {

		if (identificador.matches("servidor")) {
			if (debug) {
				System.out.println("Nueva Solicitud desde " + id + " " + new String(b) + "\r\n");	
			}

			procesarMensajeOTA(new String(b),id,remote);
		}

		if (identificador.matches("GrabadorEtiquetas")) {

			if (!(objetosGrabadorEtiqueta.isEmpty())) {

				String ids = "";
				for (clienteReader s : objetosGrabadorEtiqueta) {
					ids = ids + s.idReader + ",";	
				}

				try {
					ids = ids.substring(0, ids.lastIndexOf(","));

					String comando = new String(b);
					if (comando.matches("ESTADO")) {
						procesarSolicitud x = new procesarSolicitud();
						x.setComando("ESTADO");
						x.setError("");
						x.setRequiereRespuesta("false");
						x.setIdsEstado(ids);
						x.setIdOperacion(0);
						generarEventosEnComandoOTA(x);

					}
				} catch (Exception e) {
					grabarErrorLog("Se encontró un error al procesar la solicitud de estado del grabador de etiquetas: " + e.getMessage(), false);
				}

			} else {
				GrabadorEtiquetas.sendData(id,"ERROR:No se encontraron readers grabadores de etiqueta.",remote);
			}

		}

	}

	@Override
	public void onClientConnected(String id, String identificador) {
		
		if (identificador.matches("GrabadorEtiquetas")) {

			if (debug) {
				System.out.println("Nuevo grabador de etiquetas conectado: " + id);
			}

			//Solamente los carga cuando se conecta 1 grabador de etiquetas.
			if (GrabadorEtiquetas.getClientsTable() == 1) {
				List<config_reader> lista = new ArrayList<config_reader>();
				lista.clear();
				lista = obtenerConfigDbReaders(true);

				if (!(lista.isEmpty())) {
					for (config_reader un_config_db : lista) {
						clienteReader nuevoLector = new clienteReader(
								un_config_db.getTiempoTag(),
								un_config_db.getIp(),
								un_config_db.getIdEquipoRFID(),
								un_config_db.getDescripcion(),
								un_config_db.getTiempoProrroga(),
								un_config_db.getTipo(),
								un_config_db.getTiempoSinActividad());
						addTagListener(nuevoLector);
						objetosGrabadorEtiqueta.add(nuevoLector);
						Thread hilo1 = new Thread(nuevoLector);
						hilo1.setName("Start_tagRecorder_" + un_config_db.getIdEquipoRFID() + "_Main");
						hilo1.start();			
					}	
				}
			}

			Iterator<Map.Entry<String, etiqueta>> entradas = etiquetasEnVista.entrySet().iterator();

			while (entradas.hasNext()) {
				Map.Entry<String, etiqueta> entradatemp = entradas.next();			
				etiqueta tag = entradatemp.getValue();
				String m = ">" + tag.epc + ";" + tag.idLector + ";" + tag.idAntena + ";" + "IN" + "<";
				GrabadorEtiquetas.sendData(id, m);
			}

		}
	}

	@Override
	public void onClientDisconnected(String id, String identificador) {

		if (identificador.matches("GrabadorEtiquetas")) {

			if (debug) {
				System.out.println("Grabador de etiquetas desconectado: " + id);
			}

			//Solamente los carga cuando se conecta 1 grabador de etiquetas.
			if (GrabadorEtiquetas.getClientsTable() == 0) {
				for (clienteReader cr : objetosGrabadorEtiqueta) {
					cr.stop();
					removeTagListener(cr);
					cr.reloj.stop();
					cr = null;

					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						grabarErrorLog("onClientDisconnected: " + e.getMessage(),false);
					}		
				}
				objetosGrabadorEtiqueta.clear();
				etiquetasEnVista.clear();

			}
		}


	}

	@Override
	public void nuevaOrden(String orden) {

		procesarSolicitud x = new procesarSolicitud();

		switch (orden) {

		case "reiniciar":
			solicitarReinicioParser(0);
			break;

		case "estado":
			if (debug) {
				x.setComando("ESTADO");
				x.setError("");
				x.setRequiereRespuesta("true");
				x.setIdsEstado("");
				x.setIdOperacion(0);
				generarEventosEnComandoOTA(x);
			} else {
				System.out.println("Debe activar debug (ondebug) para la salida en pantalla.");
			}
			break;

		case "finalizar":
			x.setComando("LECTURA");
			x.setError("");
			x.setRequiereRespuesta("true");
			x.setTipoLectura("OFF");
			x.setFiltroLecturaOFF("");
			x.setIdOperacion(0);
			generarEventosEnComandoOTA(x);
			break;

		case "ondebug":
			System.out.println("Debug activado.");
			debug = true;
			break;

		case "offdebug":    
			System.out.println("Debug desactivado.");
			debug = false;
			break;	

		case "salir":
			System.out.println("El parser se cerrará.");
			System.exit(0);
			break;
		}
	}

	/**
	 * Genera una nueva consulta de estado a un reader con un codigo de operación
	 * @param idReader Entero que representa el ID del reader
	 * @param idOperacion Entero identificador de la operacion
	 */
	private static void solicitarEstadoReader(String idReader, int idOperacion){
		nuevaConsultaEstado(idReader, idOperacion);
	}

	/**
	 * Genera una nueva solicitud de lectura a un reader con los detalles contenidos
	 * en x
	 * @param x objeto tipo procesarSolicitud
	 */
	private static void solicitarInicioLecturaReader(procesarSolicitud x){

		HashSet<Integer> idsinvolucrados = new HashSet<Integer>();
		idsinvolucrados.clear();

		try {
			for (String arreglo : x.getFiltros()) {	
				String[] temp = arreglo.split("\\|");
				String[] readers = temp[0].split(",");
				String antenas = temp[1];		
				for (String a : readers) {
					nuevoInicioLectura(a, antenas, x.getTiempoLectura(), x.getIdOperacion());		
					//Se crea una lista con los readers involucrados en esta operacion
					idsinvolucrados.add(Integer.parseInt(a));	
				}
			}

			//Se agrega la operacion con los readers que involucra en la lista de
			//operaciones pendientes
			operacionesPendientes.put(x.getIdOperacion(), idsinvolucrados);

			//Se guarda la operacion en la lista de operaciones pendientes (guarda)solo
			//las operaciones que son de lectura)
			inicioLecturaPendientes.add(x.getIdOperacion());
		} catch (NumberFormatException e) {
			grabarErrorLog("Error solicitudLectura: " + e.getMessage(), true);
		}
	}

	/**
	 * Genera un evento para detener las lecturas en un reader. La informacion del
	 * reader y el codigo de operacion se encuentran contenidas en un objeto tipo
	 * procesarSolicitud
	 * @param x objeto tipo procesarSolicitud
	 */
	private static void solicitarFinLecturaReader(procesarSolicitud x){

		HashSet<Integer> codigosAdetener = new HashSet<Integer>();
		codigosAdetener.clear();

		if (x.getFiltroLecturaOFF().matches("")) {
			nuevoDetenerLectura(0, x.getIdOperacion());
			finLecturaPendientes.put(x.getIdOperacion(), inicioLecturaPendientes);

		} else {
			String[] cadena = x.getFiltroLecturaOFF().split(",");

			for (String o : cadena) {
				int p = Integer.parseInt(o);
				codigosAdetener.add(p);
			}
			finLecturaPendientes.put(x.getIdOperacion(), codigosAdetener);

			for (String o : cadena) {
				int p = Integer.parseInt(o);
				nuevoDetenerLectura(p, x.getIdOperacion());
			}

		}
	}

	/**
	 * Consulta si los códigos de operación se encuentran actualmente activos o no.
	 * @param codigoAConsultar códigos de operación a consultar como numeros separados por
	 * coma ("60,61,62..."). Si es cero "0", se consultan todos los codigos.
	 * @param codigoOperacion código de la operación asociado a esta consulta
	 */
	private static void solicitudConsultaOperacionesPendientes(String codigoAConsultar, int codigoOperacion){

		String resultado = "";

		if (!codigoAConsultar.matches("0")) {
			//Se consultan los códigos presentes en codigoAConsultar, que es una cadena
			//de códigos separados por coma (60,61,62,63)
			int cantidad = 0;
			String[] codigos = codigoAConsultar.split(",");
			for (String s : codigos) {
				if (operacionesPendientes.containsKey(Integer.valueOf(s))) {
					resultado = resultado + ",1";
				} else {
					resultado =  resultado + ",0";		
				}
				cantidad = cantidad + 1;
			}

			//Genero mensaje estilo {60,61,62} = {0,1,1} (1 = operación activa, 0 = operación inactiva)
			if (cantidad>0) {
				resultado = resultado.substring(1);
				resultado = "{" + codigoAConsultar + "} = {" + resultado + "}";
			} else {
				resultado = "{} = {}";
			}



			/**
			 * FIXME Por determinarse la forma en que se enviaran a BD
			 */


		} else {
			//Se consultan todos los codigos de operacion
			String operaciones = "";
			int cantidad = 0;
			@SuppressWarnings("rawtypes")
			Iterator<Map.Entry<Integer, HashSet>> codigos = operacionesPendientes.entrySet().iterator();
			while (codigos.hasNext()) {
				@SuppressWarnings("rawtypes")
				Map.Entry<Integer, HashSet> codigotemp = codigos.next();	
				operaciones = operaciones  + "," + codigotemp.getKey().toString();
				resultado = resultado + ",1";
				cantidad = cantidad + 1;
			}

			if (cantidad>0) {
				resultado = "{" + operaciones.substring(1) + "} = {" + resultado.substring(1) + "}";
			} else {
				resultado = "{} = {}";
			}


			/**
			 * FIXME Por determinar la forma en que se enviaran a DB
			 */

		}

		etiqueta tag = new etiqueta();
		tag.idInventario = codigoOperacion;
		tag.idMensaje = 6;
		tag.descripcionMensaje = resultado;
		almacenaTagDB(tag);

	}

	/**
	 * Finaliza todas las operaciones pendientes en el parser, iguala todos los
	 * obtetos objetosClienteReader a null y ejecuta de nuevo la secuencia de inicio
	 */
	private static void solicitarReinicioParser(int codigoOperacion){

		try {

			etiqueta tag = new etiqueta();
			tag.idMensaje = 7;
			tag.idInventario = codigoOperacion;
			tag.descripcionMensaje = "El parser se reiniciará.";
			almacenaTagDB(tag);

			//Cerramos el Socket TCP para escucha de solicitudes
			try {
				servidor.stopTcpServer();
			} catch (Exception e) {
				grabarErrorLog("Se encontró un error al cerrar el Socket TCP para escucha de solicitudes: " + e.getMessage(),false);
			}

			//Cerramos el Socket TCP para el grabador de etiquetas
			try {
				GrabadorEtiquetas.stopTcpServer();
			} catch (Exception e1) {
				grabarErrorLog("Se encontró un error al cerrar el Socket TCP para el grabador de etiquetas: " + e1.getMessage(),true);
			}

			for (clienteReader cr : objetosClienteReader) {
				cr.stop();
				removeTagListener(cr);
				cr.reloj.stop();
				cr = null;

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					grabarErrorLog("solicitarReinicioParser: " + e.getMessage(),false);
				}		
			}

			for (clienteReader cr : objetosGrabadorEtiqueta) {
				cr.stop();
				removeTagListener(cr);
				cr.reloj.stop();
				cr = null;

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					grabarErrorLog("solicitarReinicioParser: " + e.getMessage(),false);
				}		
			}

			readers.clear();
			inicioLecturaPendientes.clear();
			operacionesPendientes.clear();
			finLecturaPendientes.clear();
			objetosClienteReader.clear();
			etiquetasEnVista.clear();
			objetosGrabadorEtiqueta.clear();
			solicitudes.clear();

			inicio();

		} catch (Exception e) {
			grabarErrorLog("solicitarReinicioParser: " + e.getMessage(),false);
		}

	}

	/**
	 * Método llamado por cada reader una vez que termina de procesar una solicitud
	 * de estado
	 * @param codigoOperacion Entero que representa el código de la operación a la que se responde
	 * @param idReader Entero que representa el ID del reader que genera el mensaje de fin de operación
	 * @param tag objeto tipo etiqueta con información sobre el estado del reader.
	 */
	public synchronized static void respuestaSolicitudEstadoReader(int codigoOperacion, int idReader, etiqueta tag){

		//responde idReader
		almacenaTagDB(tag);

		if (operacionesPendientes.containsKey(codigoOperacion)) {
			operacionesPendientes.get(codigoOperacion).remove(idReader);

			if (operacionesPendientes.get(codigoOperacion).isEmpty()) {
				operacionesPendientes.remove(codigoOperacion);

				//System.out.println("ha terminado la operacion de consulta de readers " + codigoOperacion);
				etiqueta tagTemp = new etiqueta();
				tagTemp.idMensaje = 3;
				tagTemp.descripcionMensaje = "Finalización de consulta de estado de readers.";
				tagTemp.idInventario = codigoOperacion;
				almacenaTagDB(tagTemp);

			}
		}
	}

	/**
	 * Este método se ejecuta de forma asíncrona cuando se recibe un nuevo
	 * tag. Si proviene de una operación de lectura, los tags son almacenados 
	 * en un XML según su código de operación para su posterior envió.
	 * @param tag etiqueta proveniente de una operación de lectura
	 */
	public synchronized static void notificacionNuevaEtiqueta(etiqueta tag){
		
		//Si se trata de una operacion de lectura, se almacena en XML
		if (tag.tipoOperacion == 2) {
			insertarTagArchivoLecturas(tag, tag.idInventario);
		}		
		almacenaTagDB(tag);	
	}

	/**
	 * Cuando una lectura (lista) finaliza, se invoca este método indicando
	 * el fin de la lectura, identificada por un código de operación, sobre
	 * un reader
	 * @param codigoOperacion Código de operación asociado a la lectura
	 * @param idReader ID del reader que emite la notificación.
	 */
	public synchronized static void notificacionFinLecturaReader(int codigoOperacion, int idReader){

		//Se revisa la lista de operaciones pendientes y se remueve el reader
		// de la lista de readers involucrados en ese codigo de operacion. Si todos
		//los readers responden (la lista queda vacía), se genera el fin de esa operacion

		if (operacionesPendientes.containsKey(codigoOperacion)) {

			if (operacionesPendientes.get(codigoOperacion).contains(idReader)) {
				operacionesPendientes.get(codigoOperacion).remove(idReader);
			}

			if (operacionesPendientes.get(codigoOperacion).isEmpty()) {

				//System.out.println("finaliza por completo la operacion de lectura " + codigoOperacion);
				operacionesPendientes.remove(codigoOperacion);
				inicioLecturaPendientes.remove(codigoOperacion);

				etiqueta tag = new etiqueta();
				tag.idMensaje = 4;
				tag.descripcionMensaje = "Notificación de fin de lectura";
				tag.idInventario = codigoOperacion;
				almacenaTagDB(tag);

				//Se remueven las lecturas del archivo de lecturas.xml para esa operacion
				removerTagsArchivoLecturas(codigoOperacion);

				//Se elimina la operacion de OTA_pendientes
				removerOTApendiente(codigoOperacion);
			}	

			//Revisamos la lista de fin de lecturas pendientes	
			Set<Integer> b = finLecturaPendientes.keySet();
			for (Integer i : b) {
				if (finLecturaPendientes.get(i).contains(codigoOperacion)) {

					finLecturaPendientes.get(i).remove(codigoOperacion);

					if (finLecturaPendientes.get(i).isEmpty()) {
						finLecturaPendientes.remove(i);
						//System.out.println("finaliza por completo la operacion de fin de lectura " + i);

						etiqueta tag = new etiqueta();
						tag.idMensaje = 5;
						tag.descripcionMensaje = "Notificación de fin de de operación de fin de lectura";
						tag.idInventario = i;
						almacenaTagDB(tag);

					}	
				}	
			}
		}
	}

	/**
	 * Este metodo es llamado de forma asíncrona cada vez que se establece una conexión
	 * exitosa con un reader.
	 * @param idReader Retorna el id del reader que envia el mensaje de conexión exitosa
	 * @param estadoAntenas Estado de las antenas donde "0" es desconectada y "1" conectada.
	 */
	public synchronized static void notificacionCambioEstadoAntenas(int idReader, String estadoAntenas){
		grabarGeneralLog("Cambio de estado de antenas de reader " + idReader + " a " + estadoAntenas, debug);

		etiqueta tag = new etiqueta();
		tag.idMensaje = 8;
		tag.descripcionMensaje = "Cambio de estado en antenas";
		tag.idLector = idReader;
		tag.EstadoAntenas = estadoAntenas;
		tag.estadoReader = 1;
		almacenaTagDB(tag);

	}

	/**
	 * Este metodo es llamado de forma asíncrona cada vez que se establece una conexión
	 * exitosa con un reader.
	 * @param idReader Retorna el id del reader que envia el mensaje de conexión exitosa
	 * @param estadoAntenas Estado de las antenas donde "0" es desconectada y "1" conectada.
	 */
	public static void notificacionReaderConectado(int idReader, String estadoAntenas){
		grabarGeneralLog("Reader " + idReader + " conectado.", debug);

		etiqueta tag = new etiqueta();
		tag.idMensaje = 8;
		tag.descripcionMensaje = "Cambio de estado de reader";
		tag.idLector = idReader;
		tag.EstadoAntenas = estadoAntenas;
		tag.estadoReader = 1;
		almacenaTagDB(tag);

		//Seteamos el estado de ese reader a ON
		if (readers.containsKey(idReader)) {
			readers.put(idReader, "ON");
		}
	}

	/**
	 * Este método es llamado de forma asíncrona cada vez que se desconecta un reader
	 * @param idReader Retorna el id del reader que envía el mensaje de desconexión
	 * @param estadoAntenas Estado de las antenas donde "0" es desconectada y "1" conectada.
	 */
	public static void notificacionReaderDesconectado(int idReader, String estadoAntenas){
		grabarGeneralLog("Reader " + idReader + " desconectado.", debug);

		etiqueta tag = new etiqueta();
		tag.idMensaje = 8;
		tag.descripcionMensaje = "Cambio de estado de reader";
		tag.idLector = idReader;
		tag.EstadoAntenas = estadoAntenas;
		tag.estadoReader = 0;
		almacenaTagDB(tag);	

		//Seteamos el estado de ese reader a OFF
		if (readers.containsKey(idReader)) {
			readers.put(idReader, "OFF");
		}
	}

	public static interface RequestListener extends EventListener{
	/**
		 * Este método se llama de forma asíncrona cada vez que se solicita una consulta
		 * de estado del reader.
		 * @param id ID del reader a consultar.
		 * @param codigoOperacion código de la operación.
		 */
		void nuevaConsultaEstado(String id, int codigoOperacion);
		
		/**
		 * Inicia una nueva lectura en un reader
		 * @param id ID del reader donde se hará la lectura
		 * @param antena Antena o grupo de antenas que se tomaran en cuenta para la lectura.
		 * @param tiempo tiempo que durara la lectura en segundos
		 * @param codigoOperacion código de la operación asociada a la lectura
		 */
		void inicioLectura(String id, String antena, int tiempo, int codigoOperacion);
		
		/**
		 * Detiene una lectura actual.
		 * @param codigoOperacionAdetener código de operación de la lectura que se va a detener.
		 * @param codigoOperacion código de operación asociado al evento de detener lectura.
		 */
		void detenerLectura(int codigoOperacionAdetener, int codigoOperacion);
		
		void nuevoMensajeHandheld(int tipo, int id, etiqueta tag);
	}

	public static void addTagListener(RequestListener listener){
		solicitudes.add(listener);
	}
	public static void removeTagListener(RequestListener listener){
		solicitudes.remove(listener);
	}

	protected static void nuevaConsultaEstado(final String id, final int codigoOperacion){
		Thread t = new Thread(new Runnable() {
			public void run()
			{
				for(RequestListener listener:solicitudes)
					listener.nuevaConsultaEstado(id, codigoOperacion);
			}
		});
		t.setName("nuevaConsultaEstado_Main");
		t.start();
	}

	protected static void nuevoInicioLectura(final String id, final String antena, final int tiempo, final int codigoOperacion){

		Thread t = new Thread(new Runnable() {
			public void run()
			{

				for(RequestListener listener:solicitudes)
					listener.inicioLectura(id, antena, tiempo, codigoOperacion);
			}
		});
		t.setName("nuevoInicioLectura_Main");
		t.start();
	}

	protected static void nuevoDetenerLectura(final int codigoOperacionAdetener, final int codigoOperacion){	

		Thread t = new Thread(new Runnable() {
			public void run()
			{
				for(RequestListener listener:solicitudes)
					listener.detenerLectura(codigoOperacionAdetener, codigoOperacion);
			}
		});
		t.setName("nuevoDetenerLectura_Main");
		t.start();	
	}
	
	protected static void nuevoMensajeHandheld(final int tipo, final int id, final etiqueta tag){	

		Thread t = new Thread(new Runnable() {
			public void run()
			{
				for(RequestListener listener:solicitudes)
					listener.nuevoMensajeHandheld(tipo, id, tag);
			}
		});
		t.setName("nuevoDetenerLectura_Main");
		t.start();	
	}
}
