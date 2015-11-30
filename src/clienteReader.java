import java.awt.event.ActionEvent; 
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;

import javax.swing.Timer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.llrp.ltk.exceptions.InvalidLLRPMessageException;
import org.llrp.ltk.generated.enumerations.*;
import org.llrp.ltk.generated.messages.*;
import org.llrp.ltk.generated.parameters.*;
import org.llrp.ltk.net.*;
import org.llrp.ltk.types.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Esta clase implementa funcionalidades para la conexión, desconexión, envío y recepción
 * de mensajes a lectores de tarjetas RFID (readers).
 * @author José Fernández
 */
public class clienteReader implements LLRPEndpoint, Runnable, RFIDMain.RequestListener, ActionListener
{
	
	/** The reader. */
	private LLRPConnection reader;

	/** Tiempo de espera máximo para recibir respuesta del reader luego del  envío de un 
	 * mensaje. */
	private static final int TIMEOUT_MS = 10000;

	/** Identificador genérico del Reader Operation Spec (ROSpec). */
	private static final int ROSPEC_ID = 123;

	/**
	 * Tiempo para las operaciones de tag_in y tag_out. Si un reader deja de enviar
	 * mensajes de determinada etiqueta por un tiempo mayor a tiempo_tag, este tag se
	 * considera como fuera de vista.
	 */
	private int tiempo_tag = 5000;

	/**
	 * IP remoto del reader.
	 */
	private String hostname;

	/**
	 * Cadena que representa la descripción del reader.
	 */
	private String descripcion;

	/**
	 * Entero que representa el reader.
	 */
	public int idReader;

	/**
	 * Lista de los códigos de operación que el reader ejecuta
	 * actualmente. El código de cada operación debe ser añadido a la lista,
	 * y removido cuando la lista termina de procesar la solicitud.
	 */
	private HashSet<Integer> operaciones = new HashSet<Integer>();

	/**
	 * Controla la cantidad de tiempo entre consultas de configuración.
	 */
	Timer reloj = new Timer(1000, this);

	/**
	 * Ruta donde se almacenarán los logs de este reader.
	 */
	private String rutaLogs;

	/**
	 * Establece tamaño el máximo (en megabytes), que deberá tener un archivo log
	 * antes de ser comprimido en la carpeta histórico.
	 */
	private int maxLogSize = 100;

	/**
	 * Establece el tiempo de prórroga para las operaciones de lectura en readers.
	 */
	private int tiempoProrroga = 30;

	/** Identifica si se trata de un reader fijo (1), o de un handheld (2). */
	private int tipoReader;

	/** Tiempo en milisegundossin detectar KeepAlive desde un reader para considerarlo como desconectado. */
	private long tiempoSinActividad = 15000;

	/**
	 * Indica cual es el estado del reader. True = reader conectado, False: reader desconectado.
	 */
	private boolean estadoReader = false;

	/**
	 * Guarda la fecha de la ultima vez que se recibio un keepalive desde el reader. Esta variable
	 * ayudará a determinar cuando un reader se a desconectado.
	 */
	private Date fechaUltimoKeepAlive = new Date();

	/** Muestra el estado de las antenas. */
	private String estadoAntenas = "----";

	/** The log. */
	private log_file log = new log_file();
	
	/** The decoder. */
	private decoMensajeRecibido decoder = new decoMensajeRecibido();
	
	/** The listeners. */
	private List<TagListener> listeners = new CopyOnWriteArrayList<TagListener>();
	
	/** The etiquetas. */
	private List<etiqueta> etiquetas = new ArrayList<etiqueta>();

	/**
	 * Crea un nuevo objeto para la conexión, control y manejo de un reader
	 * mantiene la lista 0 y las listas por solicitud.
	 *
	 * @param tiempo_tag Tiempo entre etiquetas (valido para lista 0).
	 * @param hostname Socket remoto del reader (i.e "127.0.0.1").
	 * @param idReader Identificador número del reader.
	 * @param descripcion descripción del reader.
	 * @param tiempoProrroga the tiempo prorroga
	 * @param tipoReader the tipo reader
	 * @param tiempoSinActividad the tiempo sin actividad
	 */
	public clienteReader(int tiempo_tag, String hostname, int idReader, String descripcion, int tiempoProrroga, int tipoReader, int tiempoSinActividad) {
		super();

		if (tiempo_tag <= 0) {
			this.tiempo_tag = 5000;
		} else {
			this.tiempo_tag = tiempo_tag;
		}

		if (tiempoProrroga < 0) {
			this.tiempoProrroga = 0;
		} else {
			this.tiempoProrroga = tiempoProrroga;
		}

		if (tiempoSinActividad <= 0) {
			this.tiempoSinActividad = 15000;
		} else {
			this.tiempoSinActividad = tiempoSinActividad;
		}

		this.hostname = hostname;
		this.idReader = idReader;
		this.descripcion = descripcion;
		this.tipoReader = tipoReader;
		RFIDMain.notificacionReaderDesconectado(this.idReader,this.estadoAntenas);
		reloj.start();

	}

	/**
	 * Comienza.
	 */
	public void comienza()
	{
		//Si se trata de un reader tipo Impinj, deben iniciarse el conjunto de clases
		//y procesos destinados a crear la conexión con estos equipos. Si se trata de un
		//reader tipo handheld, no es necesario crear una conexión, solo el mantenimiento
		//de listas

		if (this.tipoReader == 1) {
			comienzaReaderImpinj();
		}else {
			comienzaHandheld();
		}
		

	}

	/**
	 * Comienza handheld.
	 */
	private void comienzaHandheld(){

		this.rutaLogs = this.descripcion + " - " + this.idReader;
		if (this.rutaLogs.matches("")) {
			this.rutaLogs = "Sin descripcion";
		}

		for (TagListener l : listeners) {
			this.removeTagListener(l);
			l = null;
		}

		listeners.clear();

		//Se inicia el reader como conectado
		estadoReader = false;

	}

	/**
	 * Comienza reader impinj.
	 */
	private void comienzaReaderImpinj(){
		try {

			this.rutaLogs = this.descripcion + " - " + this.idReader;
			if (this.rutaLogs.matches("")) {
				this.rutaLogs = "Sin descripcion";
			}

			connect(this.hostname);
			deleteROSpecs();
			addROSpec();
			enableROSpec();
			startROSpec();
			setReaderConfig();
			
			//Se actualiza el estado de las antenas
			//y el estado del reader en el padre
			etiqueta tag = consultarEstadoReader();
			this.estadoAntenas = tag.EstadoAntenas;
			fechaUltimoKeepAlive = new Date();	
			this.grabarGeneralLog("Reader " + this.descripcion + " " + "id: " + this.idReader + " conectado", false);

			//Borramos la lista de los listeners
			for (TagListener l : listeners) {
				this.removeTagListener(l);
				l = null;
			}
			listeners.clear();

			//Se crea la lista 0
			lista nuevaLista = new lista("0", false, this.tiempo_tag, 0, 0, this, this.tiempoProrroga);
			this.addTagListener(nuevaLista);
			nuevaLista.iniciar();

		} catch (LLRPConnectionAttemptFailedException e) {
			if (!(this.estadoReader)) {
				grabarErrorLog("No se pudo realizar la conexión con el reader " + this.idReader + " en " + this.hostname + ", " + e.getMessage(), false);
				this.estadoReader = false;
				this.stop();
				try {
					Thread.sleep(10000);
					this.comienza();
				} catch (InterruptedException e1) {
					grabarErrorLog("Error en el tiempo de espera para reconexión: " + e.getMessage(),false);
				}
			}
		} 
	}

	/**
	 * Almacenar tag.
	 *
	 * @param message the message
	 */
	private void almacenarTag(LLRPMessage message){

		try {
			String tag = message.toXMLString();
			this.grabarTag(tag, false);
		} catch (InvalidLLRPMessageException e) {
			this.grabarErrorLog("Se encontró un error al grabar el tag: " + e.getMessage(), false);
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		comienza();
	}

	/**
	 * Este método es ejecutado de forma asíncrona cada vez que un nuevo tag
	 * es leído por el reader.
	 *
	 * @param message the message
	 */
	public void messageReceived(LLRPMessage message){
		
		etiquetas.clear();
		etiquetas = decoder.decodificar (message);

		for (etiqueta l : etiquetas) {

			//Nuevo tag por etiqueta leida
			if (l.idMensaje == 1) {
				this.nuevoTagRecibido(l);
				almacenarTag(message);
			} 

			//Mensaje de keepalive desde el reader
			if (l.idMensaje == 2) {
				this.fechaUltimoKeepAlive = new Date();
			}

			//conexion de antena
			if (l.idMensaje == 3) {
				StringBuilder ant = new StringBuilder(this.estadoAntenas);
				ant.setCharAt(l.idAntena -1, '1');
				this.estadoAntenas = ant.toString();
				almacenarTag(message);
				RFIDMain.notificacionCambioEstadoAntenas(this.idReader, this.estadoAntenas);
			}

			//desconexion de antena
			if (l.idMensaje == 4) {
				StringBuilder ant = new StringBuilder(this.estadoAntenas);
				ant.setCharAt(l.idAntena-1, '0');
				this.estadoAntenas = ant.toString();
				almacenarTag(message);
				RFIDMain.notificacionCambioEstadoAntenas(this.idReader, this.estadoAntenas);

			}
		}
	}

	/**
	 * Este método es llamado de forma asíncrona cuando se produce un error.
	 *
	 * @param s the s
	 */
	public void errorOccured(String s)
	{
		this.grabarErrorLog("Se ha producido un error (ltkjava): " + s, false);
	}

	@Override
	/**
	 * Este método se llama de forma asíncrona cada vez que se solicita una consulta
	 * de estado del reader.
	 */
	public void nuevaConsultaEstado(String id, int codigoOperacion) {

		if (this.tipoReader==1) {
			//Consultamos si la solicitud va dirigida a este reader
			if ((id.matches(String.valueOf(this.idReader))) || (id.matches(""))) {

				//Consultamos si ya se esta ejecutando esa operacion en este reader
				//si no esta, se ejecuta
				if (!this.operaciones.contains(codigoOperacion)) {

					this.operaciones.add(codigoOperacion);
					this.grabarComando(
							"Nueva consulta de estado, código de operación: "
									+ codigoOperacion, false);

					etiqueta tag = new etiqueta();
					tag = consultarEstadoReader();
					tag.descripcionMensaje = "Respuesta de estado reader";
					tag.idLector = this.idReader;
					tag.idMensaje = 2;
					tag.idInventario = codigoOperacion;
					RFIDMain.respuestaSolicitudEstadoReader(codigoOperacion,
							this.idReader, tag);
					this.notificacionFinConsultaEstado(codigoOperacion);
				}
			}
		}
	}

	@Override
	/**
	 * Inicia una nueva lectura en un reader
	 * @param id ID del reader donde se hará la lectura
	 * @param antena Antena o grupo de antenas que se tomaran en cuenta para la lectura.
	 * @param tiempo tiempo que durara la lectura en segundos
	 * @param codigoOperacion código de la operación asociada a la lectura
	 */
	public void inicioLectura(String id, String antena, int tiempo, int codigoOperacion) {
		//Consultamos si la solicitud va dirigida a este reader

		if (id.matches(String.valueOf(this.idReader))) {

			//Consultamos si ya se esta ejecutando esa operación en este reader
			//si no está, se ejecuta.			
			lista nuevaLista = new lista(antena, true, this.tiempo_tag, codigoOperacion, tiempo, this, this.tiempoProrroga);
			this.addTagListener(nuevaLista);
			nuevaLista.iniciar();	
			this.operaciones.add(codigoOperacion);
			this.grabarComando("Nueva solicitud lectura, código de operación: " + codigoOperacion, false);
		}
	}

	@Override
	/**
	 * Detiene una lectura actual.
	 * @param codigoOperacionAdetener código de operación de la lectura que se va a detener.
	 * @param codigoOperacion código de operación asociado al evento de detener lectura.
	 */
	public void detenerLectura(int codigoOperacionAdetener, int codigoOperacion) {

		if (this.operaciones.contains(codigoOperacionAdetener) || (codigoOperacionAdetener == 0)){
			this.finalizarLectura(codigoOperacionAdetener);
			this.operaciones.remove(codigoOperacion);
		}		
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {

		if (estadoReader == true) {
			Date fecha_actual = new Date();
			long diferencia;
			diferencia = fecha_actual.getTime()	- this.fechaUltimoKeepAlive.getTime();

			if (diferencia > this.tiempoSinActividad) {

				if (this.tipoReader == 1) {
					//Desde hace 6 segundos no se recibe keepalive, el reader se considera apagado
					this.stop();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					Thread hilo = new Thread(this);
					hilo.setName("Reconnect_Reader_" + this.idReader + "_clienteReader");
					hilo.start();
				} else {
					RFIDMain.notificacionReaderDesconectado(this.idReader, "1");
					this.estadoReader = false;
				}

			}
		}	
	}

	@Override
	public void nuevoMensajeHandheld(int tipo, int id, etiqueta tag) {

		if (id == this.idReader) {
			switch (tipo) {
			case 0:
				this.grabarTag("Lector: " + tag.idLector + ", Antena: " + tag.idAntena + ", EPC: " + tag.epc + ", Hora: " + tag.ultimaVezFecha,false);
				this.nuevoTagRecibido(tag);
				break;

			case 1:
				estadoReader = true;
				this.fechaUltimoKeepAlive = new Date();
				this.grabarGeneralLog("Reader " + this.descripcion + " " + "id: " + this.idReader + " conectado", false);
				RFIDMain.notificacionReaderConectado(id, "1");
				break;

			case 2:
				estadoReader = false;
				this.grabarGeneralLog("Reader " + this.descripcion + " " + "id: " + this.idReader + " desconectado", false);
				RFIDMain.notificacionReaderDesconectado(id, "2");
				break;

			case 3:
				if (this.estadoReader) {
					this.fechaUltimoKeepAlive = new Date();
				} else {
					estadoReader = true;
					this.fechaUltimoKeepAlive = new Date();
					RFIDMain.notificacionReaderConectado(id, "1");
				}
				break;

			default:
				break;
			}
		}

	}

	/**
	 * Este método se ejecuta de forma asíncrona cada vez que una de las listas
	 * asociadas a un reader (tanto lista 0 como cualquier lista por solicitud)
	 * genera un nueva etiqueta procedente de una operación tag_in, tag_out o de
	 * lectura en una antena.
	 * @param tag Etiqueta producida por la lista.
	 */
	public synchronized void notificacionNuevaEtiquetaLectura(etiqueta tag){
		tag.idLector = this.idReader;
		RFIDMain.notificacionNuevaEtiqueta(tag);
	}

	/**
	 * Este método se ejecuta de forma asíncrona cuando una de las listas que
	 * controla el reader termina completamente una operación de lectura.
	 * @param codigoOperacion código de operación de la lectura.
	 * @param l	Lista que controlaba esa operación.
	 */
	public synchronized void notificacionFinLectura(int codigoOperacion, lista l){
		this.operaciones.remove(codigoOperacion);
		this.removeTagListener(l);
		l = null; //Candidato a ser borrado por garbage Collector
		RFIDMain.notificacionFinLecturaReader(codigoOperacion, this.idReader);
	}

	/**
	 * Notificacion fin consulta estado.
	 *
	 * @param codigoOperacion the codigo operacion
	 */
	private void notificacionFinConsultaEstado(int codigoOperacion){
		this.operaciones.remove(codigoOperacion);
	}

	/**
	 * Grabar error log.
	 *
	 * @param descripcion the descripcion
	 * @param desplegarMensaje the desplegar mensaje
	 */
	private void grabarErrorLog(String descripcion, boolean desplegarMensaje){
		if (desplegarMensaje) {
			System.out.println(descripcion);
		}
		comprobarMedidaParaCompresion(this.rutaLogs + "/log_errores.txt");
		log.escribir(descripcion, this.rutaLogs + "/log_errores.txt");
	}

	/**
	 * Grabar general log.
	 *
	 * @param descripcion the descripcion
	 * @param desplegarMensaje the desplegar mensaje
	 */
	private void grabarGeneralLog(String descripcion, boolean desplegarMensaje){
		if (desplegarMensaje) {
			System.out.println(descripcion);
		}
		comprobarMedidaParaCompresion(this.rutaLogs + "/log_general.txt");
		log.escribir(descripcion, this.rutaLogs + "/log_general.txt");
	}

	/**
	 * Grabar tag.
	 *
	 * @param descripcion the descripcion
	 * @param desplegarMensaje the desplegar mensaje
	 */
	private void grabarTag(String descripcion, boolean desplegarMensaje){
		if (desplegarMensaje) {
			System.out.println(descripcion);
		}
		comprobarMedidaParaCompresion(this.rutaLogs + "/log_Tags.txt");
		log.escribir(descripcion, this.rutaLogs + "/log_Tags.txt");
	}

	/**
	 * Grabar comando.
	 *
	 * @param descripcion the descripcion
	 * @param desplegarMensaje the desplegar mensaje
	 */
	private void grabarComando(String descripcion, boolean desplegarMensaje){
		if (desplegarMensaje) {
			System.out.println(descripcion);
		}
		comprobarMedidaParaCompresion(this.rutaLogs + "/log_OTA.txt");
		log.escribir(descripcion, this.rutaLogs + "/log_OTA.txt");
	}

	/**
	 * Comprueba si el tamaño de un archivo es superior al tamaño máximo de log.
	 * Si lo es, lo comprime en la carpeta "Historico" con su nombre y la fecha
	 * en que fue creado.
	 * @param ruta Ruta completa del archivo a comprimir.
	 */
	private void comprobarMedidaParaCompresion(String ruta){	

		File file =new File(ruta);
		double bytes = file.length();
		double megabytes = bytes/1048576;
		String nombre = file.getName();
		nombre = nombre.substring(0,nombre.indexOf("."));

		if (megabytes > this.maxLogSize) {
			SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
			zip_files comprimirArchivos = new zip_files(this.rutaLogs + "/Historico", ruta, nombre + " " + dt.format(new Date()));
			comprimirArchivos.comprimir();
			file.delete();
		}	
	}
	
	/**
	 * Solicita al reader información referente a número y estado de las antenas,
	 * versión de firmware, número de modelo y MAC address. Al hacer esto, se fuerza
	 * la actualización de su estado.
	 * @return Objeto etiqueta con la información del reader.
	 */
	public etiqueta consultarEstadoReader(){

		//Para los handheld no es aplicable la consulta de estado.
		if (this.tipoReader == 1) {

			etiqueta tag = new etiqueta();
			GET_READER_CONFIG config = new GET_READER_CONFIG();
			GET_READER_CAPABILITIES capabilities = new GET_READER_CAPABILITIES();
			GET_READER_CONFIG_RESPONSE respuesta_config;
			GET_READER_CAPABILITIES_RESPONSE respuesta_capabilities;
			capabilities
			.setRequestedData(new GetReaderCapabilitiesRequestedData(
					GetReaderCapabilitiesRequestedData.All));
			config.setRequestedData(new GetReaderConfigRequestedData(
					GetReaderConfigRequestedData.All));
			config.setAntennaID(new UnsignedShort(0));
			config.setGPIPortNum(new UnsignedShort(0));
			config.setGPOPortNum(new UnsignedShort(0));
			try {
				
				respuesta_config = (GET_READER_CONFIG_RESPONSE) reader
						.transact(config, TIMEOUT_MS);
				respuesta_capabilities = (GET_READER_CAPABILITIES_RESPONSE) reader
						.transact(capabilities, TIMEOUT_MS);

				tag.TipoIdentificadorReader = respuesta_config
						.getIdentification().getIDType().toString();
				tag.IdentificadorReader = respuesta_config.getIdentification()
						.getReaderID().toString();
				
				List<AntennaProperties> antenas = respuesta_config
						.getAntennaPropertiesList();
				String confAntenas = "";
				for (AntennaProperties ant : antenas) {
					confAntenas = confAntenas
							+ ant.getAntennaConnected().toString();
				}

				tag.EstadoAntenas = confAntenas;
				tag.FirmwareReader = respuesta_capabilities
						.getGeneralDeviceCapabilities()
						.getReaderFirmwareVersion().toString();
				tag.NombreModeloReader = respuesta_capabilities
						.getGeneralDeviceCapabilities().getModelName()
						.toString();

				if (!(this.estadoReader)) {
					RFIDMain.notificacionReaderConectado(this.idReader,
							tag.EstadoAntenas);
					this.estadoReader = true;
				}

				return tag;

			} catch (TimeoutException | NullPointerException e) {
				grabarErrorLog(
						"consultarEstadoReader: TimeoutException"
								+ e.getMessage(), false);
				tag.TipoIdentificadorReader = "No encontrado";
				tag.IdentificadorReader = "----";
				tag.EstadoAntenas = "----";
				tag.FirmwareReader = "----";
				tag.NombreModeloReader = "----";

				//Como el reader no devolvio informacion, suponemos que el reader se ha desconectado
				//se cierra la conexion y se fuerza la reconexion
				if (this.estadoReader) {
					RFIDMain.notificacionReaderDesconectado(this.idReader,
							this.estadoAntenas);
				}
				return tag;

			} catch (Exception e) {
				grabarErrorLog(
						"Error al consultar el estado del reader: "
								+ e.getMessage(), false);
				return null;
			}
		}

		return null;
	}

	/**
	 * Inicia la conexión con un reader.
	 *
	 * @param hostname Socket remoto del reader (i.e "127.0.0.1").
	 * @throws LLRPConnectionAttemptFailedException Si el reader no esta disponible
	 */
	private void connect(String hostname) throws LLRPConnectionAttemptFailedException
	{

			// Se crea el objeto reader.
			reader = new LLRPConnector(this, hostname);

			// Se inicia la conexión con el reader.
			(( LLRPConnector) reader).connect();
			reader.getHandler().setKeepAliveForward(true);
	}

	/**
	 * Finaliza la conexión con el reader.
	 */
	private void disconnect()
	{
		((LLRPConnector) reader).disconnect();
		this.grabarGeneralLog("Reader " + this.descripcion + " " + "id: " + this.idReader + " desconectado", false);
		if (this.estadoReader) {
			RFIDMain.notificacionReaderDesconectado(this.idReader, this.estadoAntenas);
		}
		this.estadoReader = false;
	}

	/**
	 * Borra todos los ROSpec y se desconecta del reader. borra todas las listas
	 * asociadas a este reader.
	 */
	public void stop()
	{
		//Fuerzo la detencion de las lecturas sobre este reader
		this.finalizarLectura(0);

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			grabarErrorLog("stop: " + e.getMessage(), false);
		}


		if (this.tipoReader == 1) {
			deleteROSpecs();
			disconnect();
		}
	}
	
	/**
	 * Construye el archivo de configuración config.xml que contiene la configuración por
	 * defecto de la potencia de las antenas.
	 */
	private void buildConfigFile(){
			
		
		Element rootnode = new Element("config");
		Document doc = new Document(rootnode);
		doc.setRootElement(rootnode);
		
		
		for (int i = 1; i < 5; i++) {
			
			Element AntennaConfiguration = new Element("AntennaConfiguration");
			
			AntennaConfiguration.addContent(new Element("AntennaID").setText(Integer.toString(i)));
			
			Element RFReceiver = new Element("RFReceiver");
			RFReceiver.addContent(new Element("ReceiverSensitivity").setText("1"));
			AntennaConfiguration.addContent(RFReceiver);
	 
			Element RFTransmitter = new Element("RFTransmitter");
			RFTransmitter.addContent(new Element("HopTableID").setText("1"));
			RFTransmitter.addContent(new Element("ChannelIndex").setText("1"));
			RFTransmitter.addContent(new Element("TransmitPower").setText("75"));
			AntennaConfiguration.addContent(RFTransmitter);
			doc.getRootElement().addContent(AntennaConfiguration);
		}

		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		try {
			xmlOutput.output(doc, new FileWriter(this.rutaLogs + "/config.xml"));
		} catch (IOException e) {
			this.grabarErrorLog("Se encontró un problema a la hora de crear el archivo config.xml: " + e.getMessage(), false);
		}
	}
	
	/**
	 * Gets the antenna config.
	 *
	 * @param solicitud the solicitud
	 * @return the antenna config
	 */
	private AntennaConfiguration getAntennaConfig(Node solicitud){
		
		AntennaConfiguration a = new AntennaConfiguration();
		NodeList info = solicitud.getChildNodes();
		
		if (solicitud.getNodeName().matches("AntennaConfiguration")) {
			
			for(int j=0; j<info.getLength(); j++){

				Node parametro = info.item(j);
				String nombreParametro = parametro.getNodeName();

				switch (nombreParametro) {
				case "AntennaID":
					a.setAntennaID(new UnsignedShort(Integer.parseInt(parametro.getTextContent())));
					break;

				case "RFReceiver":
					NodeList rfrList = parametro.getChildNodes();
					RFReceiver rfr = new RFReceiver();

					for (int k = 0; k < rfrList.getLength(); k++) {
						if (rfrList.item(k).getNodeName().matches("ReceiverSensitivity")) {
							rfr.setReceiverSensitivity(new UnsignedShort(Integer.parseInt(rfrList.item(k).getTextContent())));
							a.setRFReceiver(rfr);
						}
					}

				case "RFTransmitter":
					NodeList RFTList = parametro.getChildNodes();
					RFTransmitter rft = new RFTransmitter();

					for (int k = 0; k < RFTList.getLength(); k++) {

						if (RFTList.item(k).getNodeName().matches("HopTableID")) {
							rft.setHopTableID(new UnsignedShort(Integer.parseInt(RFTList.item(k).getTextContent())));
						}

						if (RFTList.item(k).getNodeName().matches("ChannelIndex")) {
							rft.setChannelIndex((new UnsignedShort(Integer.parseInt(RFTList.item(k).getTextContent()))));
						}

						if (RFTList.item(k).getNodeName().matches("TransmitPower")) {
							rft.setTransmitPower(new UnsignedShort(Integer.parseInt(RFTList.item(k).getTextContent())));
						}
					}
					a.setRFTransmitter(rft);

				default:
					break;
				}
			}
			return a;
		} else {
			return null;
		}
		
	}

	/**
	 * Verifica que exista una configuración de potencia y sensibilidad para las antenas.
	 * Si existe, retorna una lista de objetos AntennaConfiguration
	 *
	 * @return the list
	 */
	private List<AntennaConfiguration> checkForAntennaConfiguration(){
		
		File configFile = new File(this.rutaLogs + "/config.xml");

		if (configFile.exists()) {
			
			org.w3c.dom.Document doc = null;  
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();  
			DocumentBuilder db;
			
			try {
				db = dbf.newDocumentBuilder();
				doc = db.parse(configFile);
				NodeList AntennaConfig = doc.getFirstChild().getChildNodes();
				List<AntennaConfiguration> l = new ArrayList<AntennaConfiguration>();
				
				for(int i=0; i<AntennaConfig.getLength(); i++){

					Node solicitud = AntennaConfig.item(i);
					AntennaConfiguration a = new AntennaConfiguration();
					a = getAntennaConfig(solicitud);
					
					if (!(a == null)) {
						l.add(a);
					}
				}

				return l;
						
			} catch (ParserConfigurationException e) {
				this.grabarErrorLog("Se encontro un problema al leer el archivo config.xml: " + e.getMessage(), false);
			} catch (SAXException e) {
				this.grabarErrorLog("Se encontro un problema al leer el archivo config.xml: " + e.getMessage(), false);
			} catch (IOException e) {
				this.grabarErrorLog("Se encontro un problema al leer el archivo config.xml: " + e.getMessage(), false);
			}
			
		} else {
			buildConfigFile();
			return null;
		}
		return null;

	}


	/**
	 * Construye un ROSpec (Reader Operation Spec). 
	 * Se especifica los triggers de inicio, final, contenidos del reporte, antenas, etc.
	 * @return Objeto tipo Reader Operation Spec (ROSpec)
	 */
	private ROSpec buildROSpec()
	{

		try {
			//Se crea un nuevo ROSpec (Reader Operation Spec).
			ROSpec roSpec = new ROSpec();

			roSpec.setPriority(new UnsignedByte(0));
			roSpec.setCurrentState(new ROSpecState(ROSpecState.Disabled));
			roSpec.setROSpecID(new UnsignedInteger(ROSPEC_ID));

			//Se crea un nuevo ROBoundarySpec. Esto define los triggers de inicio y final.
			ROBoundarySpec roBoundarySpec = new ROBoundarySpec();

			//Trigger de inicio se fija a null. De esta forma el ROSpec se inicia
			//tan pronto como es habilitado
			ROSpecStartTrigger startTrig = new ROSpecStartTrigger();
			startTrig.setROSpecStartTriggerType (new ROSpecStartTriggerType(ROSpecStartTriggerType.Null));
			roBoundarySpec.setROSpecStartTrigger(startTrig);

			//Trigger de fin se fija a null. El ROSpec continuará ejecutándose
			//hasta recibir el mensaje STOP_ROSPEC
			ROSpecStopTrigger stopTrig = new ROSpecStopTrigger();
			stopTrig.setDurationTriggerValue(new UnsignedInteger(0));
			stopTrig.setROSpecStopTriggerType (new ROSpecStopTriggerType(ROSpecStopTriggerType.Null));
			roBoundarySpec.setROSpecStopTrigger(stopTrig);

			roSpec.setROBoundarySpec(roBoundarySpec);

			//Se añade un inventario de antenas.
			AISpec aispec = new AISpec();

			//Se fija el trigger de fin de AI a null, asi AI spec
			//continuara operativo hasta que se detenga el ROSpec
			AISpecStopTrigger aiStopTrigger = new AISpecStopTrigger();
			aiStopTrigger.setAISpecStopTriggerType (new AISpecStopTriggerType(AISpecStopTriggerType.Null));
			aiStopTrigger.setDurationTrigger(new UnsignedInteger(0));
			aispec.setAISpecStopTrigger(aiStopTrigger);

			//Se selecciona que puertos de antena se quieren utilizar
			//Si se fija a cero significa todas las antenas
			UnsignedShortArray antennaIDs = new UnsignedShortArray();
			antennaIDs.add(new UnsignedShort(0));
			aispec.setAntennaIDs(antennaIDs);
				
			//Se configura al reader para leer tags tipo Gen2
			InventoryParameterSpec inventoryParam = new InventoryParameterSpec();
			inventoryParam.setProtocolID (new AirProtocols(AirProtocols.EPCGlobalClass1Gen2));			
			inventoryParam.setInventoryParameterSpecID(new UnsignedShort(1));
			//aispec.addToInventoryParameterSpecList(inventoryParam);
			//roSpec.addToSpecParameterList(aispec);
			
			///*******************************************************************************
			///**********************CONFIGURACION DE ANTENAS*********************************
			///*******************************************************************************

			//Se busca en el archivo config.xml la configuración de la potencia de las antenas
			List<AntennaConfiguration> l = new ArrayList<AntennaConfiguration>();
			l = checkForAntennaConfiguration();
			
			//Si se retorna una lista de objetos AntennaConfiguration, estos se añaden al
			//Rospec
			if (l != null) {
				if (l.size()> 0) {
					for (AntennaConfiguration antennaConfiguration : l) {
						inventoryParam.addToAntennaConfigurationList(antennaConfiguration);
					}	
				}
				
			} else {
				//Si no se retorna una configuracion valida, se carga una por defecto donde
				//todas las antenas funcionan a potencia maxima
				for (int i = 1; i < 5; i++) {

					// Se crea una configuracion de antena
					AntennaConfiguration antConfig = new AntennaConfiguration();
					// Se selecciona el ID de la antena a configurar
					antConfig.setAntennaID(new UnsignedShort(i));

					// Valores de potencia de transmision
					RFTransmitter tx = new RFTransmitter();
					tx.setTransmitPower(new UnsignedShort(75));
					tx.setChannelIndex(new UnsignedShort(1));
					tx.setHopTableID(new UnsignedShort(1));
					antConfig.setRFTransmitter(tx);
					
					// Sensibilidad de antena
					RFReceiver rx = new RFReceiver();
					rx.setReceiverSensitivity(new UnsignedShort(1));
					antConfig.setRFReceiver(rx);

					// Se añade la configuracion de la antena
					inventoryParam.addToAntennaConfigurationList(antConfig);
				}
			}

			aispec.addToInventoryParameterSpecList(inventoryParam);
			roSpec.addToSpecParameterList(aispec);
			//********************************************************************************
			///********************FIN DE CONFIGURACION DE ANTENAS****************************
			///*******************************************************************************


			//Se especifica que tipo de reportes de etiqueta se quiere recibir
			//y cuando recibirlos
			ROReportSpec roReportSpec = new ROReportSpec();

			//Recibir un reporte cada vez que una etiqueta es leída.
			roReportSpec.setROReportTrigger(new ROReportTriggerType
					(ROReportTriggerType.Upon_N_Tags_Or_End_Of_ROSpec));
			roReportSpec.setN(new UnsignedShort(1));
			TagReportContentSelector reportContent = new TagReportContentSelector();
			
			///////////////////////////////////
			/*
			AntennaEvent ae = new AntennaEvent();
			ae.setAntennaID(new UnsignedShort(1));
			ae.setEventType(new AntennaEventType(0));
			
			ReaderEventNotificationData rend = new ReaderEventNotificationData();
			rend.setAntennaEvent(ae);
			*/

			//Se seleccionan los campos que se quieren en el reporte.
			reportContent.setEnableAccessSpecID(new Bit(1));
			reportContent.setEnableAntennaID(new Bit(1));
			reportContent.setEnableChannelIndex(new Bit(1));
			reportContent.setEnableFirstSeenTimestamp(new Bit(1));
			reportContent.setEnableInventoryParameterSpecID(new Bit(1));
			reportContent.setEnableLastSeenTimestamp(new Bit(1));
			reportContent.setEnablePeakRSSI(new Bit(1));
			reportContent.setEnableROSpecID(new Bit(1));
			reportContent.setEnableSpecIndex(new Bit(1));
			reportContent.setEnableTagSeenCount(new Bit(1));
			roReportSpec.setTagReportContentSelector(reportContent);

			C1G2EPCMemorySelector epcMemSel = new C1G2EPCMemorySelector();
			epcMemSel.setEnableCRC(new Bit(1));
			epcMemSel.setEnablePCBits(new Bit(1));

			reportContent.addToAirProtocolEPCMemorySelectorList(epcMemSel);

			roSpec.setROReportSpec(roReportSpec);

			this.grabarGeneralLog("ROSpec creado exitosamente: " + this.hostname, false);
			return roSpec;

		} catch (Exception e) {
			this.grabarErrorLog("Error durante la creación del ROSpec: " + this.hostname + ", "+ e.getMessage(), false);
			return null;
		}

	}
	
	/**
	 * Añade un ROSpec al reader.
	 */
	private void addROSpec()
	{
		ADD_ROSPEC_RESPONSE response;
		ROSpec roSpec = buildROSpec();
		
		this.grabarGeneralLog("Añadiendo el ROSpec: " + this.hostname, false);
		try
		{
			ADD_ROSPEC roSpecMsg = new ADD_ROSPEC();
			roSpecMsg.setROSpec(roSpec);
			response = (ADD_ROSPEC_RESPONSE) reader.transact(roSpecMsg, TIMEOUT_MS);

			//Se verifica que el ROSpec se añadio de forma exitosa.
			StatusCode status = response.getLLRPStatus().getStatusCode();
			if (status.equals(new StatusCode("M_Success")))
			{
				this.grabarGeneralLog("ROSpec añadido de forma exitosa: " + this.hostname, false);
			}
			else
			{
				this.grabarErrorLog("Error añadiendo el ROSpec: " + this.hostname + " ," + response.getLLRPStatus().getErrorDescription().toString() , false);
				this.stop();
			}
		}
		catch (Exception e)
		{
			this.grabarErrorLog("Error añadiendo el ROSpec: " + this.hostname + " " + e.getMessage(),false);
		}
	}

	/**
	 * Habilita el ROSpec.
	 */
	private void enableROSpec()
	{
		ENABLE_ROSPEC_RESPONSE response;
		this.grabarGeneralLog("Habilitando el ROSpec: " + this.hostname, false);

		ENABLE_ROSPEC enable = new ENABLE_ROSPEC();
		enable.setROSpecID(new UnsignedInteger(ROSPEC_ID));
		try
		{
			response = (ENABLE_ROSPEC_RESPONSE)reader.transact(enable, TIMEOUT_MS);		
			
			StatusCode status = response.getLLRPStatus().getStatusCode();
			if (status.equals(new StatusCode("M_Success"))){
				this.grabarGeneralLog("Rospec habilitado exitosamente: " + this.hostname, false);
			} else {
				this.grabarErrorLog("Error habilitando el ROSpec: " + this.hostname + " ," + response.getLLRPStatus().getErrorDescription().toString() , false);
			}			
		}
		catch (Exception e)
		{
			this.grabarErrorLog("Error habilitando el ROSpec: " + this.hostname + ", " + e.getMessage(), false);		
		}
	}

	/**
	 * Inicia el ROSpec.
	 */
	private void startROSpec()
	{
		START_ROSPEC_RESPONSE response;
		this.grabarGeneralLog("Iniciando el ROSpec: " + this.hostname, false);

		START_ROSPEC start = new START_ROSPEC();
		start.setROSpecID(new UnsignedInteger(ROSPEC_ID));
		try
		{
			response = (START_ROSPEC_RESPONSE) reader.transact(start, TIMEOUT_MS);
			
			StatusCode status = response.getLLRPStatus().getStatusCode();
			if (status.equals(new StatusCode("M_Success"))){
				this.grabarGeneralLog("ROSpec iniciado correctamente: " + this.hostname, false);
			} else {
				this.grabarErrorLog("Error iniciando el ROSpec: " + this.hostname + " ," + response.getLLRPStatus().getErrorDescription().toString() , false);
			}
		}
		catch (Exception e)
		{
			this.grabarErrorLog("Error borrando el ROSpec: " + this.hostname + ", " + e.getMessage(), false);
		}
	}

	/**
	 * Borra todos los ROSpec grabados en un reader.
	 */
	private void deleteROSpecs()
	{
		DELETE_ROSPEC_RESPONSE response;
		this.grabarGeneralLog("Borrando todos los ROSpecs: " + this.hostname, false);
		DELETE_ROSPEC del = new DELETE_ROSPEC();
		// Se utiliza cero como ROSpec ID.
		// Esto significa todos los ROSpec.
		del.setROSpecID(new UnsignedInteger(0));

		try
		{
			response = (DELETE_ROSPEC_RESPONSE)	reader.transact(del, TIMEOUT_MS);
			
			StatusCode status = response.getLLRPStatus().getStatusCode();
			if (status.equals(new StatusCode("M_Success"))){
				this.grabarGeneralLog("ROSpecs borrados exitosamente: " + this.hostname, false);
			} else {
				this.grabarErrorLog("Error borrando los ROSpecs: " + this.hostname + " ," + response.getLLRPStatus().getErrorDescription().toString() , false);
			}
		}
		catch (Exception e)
		{
			this.grabarErrorLog("Error al borrar los ROSpec: " + this.hostname + ", " + e.getMessage(), false);
		}
	}

	/**
	 * Se configura el reader.
	 */
	private void setReaderConfig(){

		this.grabarGeneralLog("Iniciando configuracion del Reader: " + this.hostname, false);
		//Configuración para el KeepAlive.
		KeepaliveSpec keepalive = new KeepaliveSpec();

		KeepaliveTriggerType kaTriggerType = new KeepaliveTriggerType(KeepaliveTriggerType.Periodic);
		keepalive.setKeepaliveTriggerType(kaTriggerType);
		keepalive.setPeriodicTriggerValue(new UnsignedInteger(2000));

		//Eventos que generara el reader.
		ReaderEventNotificationSpec RSpec = new ReaderEventNotificationSpec();

		EventNotificationState i = new EventNotificationState();
		i.setEventType(new NotificationEventType(NotificationEventType.Antenna_Event));
		i.setNotificationState(new Bit(1));

		RSpec.addToEventNotificationStateList(i);
		SET_READER_CONFIG config = new SET_READER_CONFIG();	
		SET_READER_CONFIG_RESPONSE response;
		config.setKeepaliveSpec(keepalive);
		config.setReaderEventNotificationSpec(RSpec);
		config.setResetToFactoryDefault(new Bit(0));
		
		try {
			
			response = (SET_READER_CONFIG_RESPONSE) reader.transact(config, TIMEOUT_MS);
			
			StatusCode status = response.getLLRPStatus().getStatusCode();
			if (status.equals(new StatusCode("M_Success"))){
				this.grabarGeneralLog("Reader configurado exitosamente: " + this.hostname, false);
			} else {
				this.grabarErrorLog("Error configurando el Reader: " + this.hostname + " ," + response.getLLRPStatus().getErrorDescription().toString() , false);
			}
		} catch (Exception j) {
			this.grabarErrorLog("Error configurando Reader: " + this.hostname + ", " + j.getMessage(), false);
		}
	}


	/**
	 * Define métodos para la lectura de nuevos tags procedentes de un reader.
	 * @author José Fernández
	 */
	public static interface TagListener extends EventListener{
		/**
		 * Este método se ejecuta de forma asíncrona cuando el reader 
		 * recibe una nueva etiqueta de cualquiera de sus antenas.
		 * @param tag Objeto tipo etiqueta
		 */
		void nuevoTagRecibido(etiqueta tag);

		/**
		 * Este método se ejecuta de forma asíncrona cuando el reader solicita la
		 * finalización de una operación de lectura que se encuentra en curso.
		 *
		 * @param idSolicitud Entero que representa el ID de la solicitud.
		 */
		void finalizarLectura(int idSolicitud);
	}

	/**
	 * Adds the tag listener.
	 *
	 * @param listener the listener
	 */
	public void addTagListener(TagListener listener){
		listeners.add(listener);
	}

	/**
	 * Removes the tag listener.
	 *
	 * @param listener the listener
	 */
	public void removeTagListener(TagListener listener){
		listeners.remove(listener);
	}

	/**
	 * Nuevo tag recibido.
	 *
	 * @param tag the tag
	 */
	protected void nuevoTagRecibido(final etiqueta tag){
		Thread t = new Thread(new Runnable() {
			public void run()
			{	
				for (int i=0;i<listeners.size();i++) {
					listeners.get(i).nuevoTagRecibido(tag);	    
				}
			}
		});
		t.setName("nuevoTagRecibido_clienteReader");
		t.start();		

	}

	/**
	 * Finalizar lectura.
	 *
	 * @param idSolicitud the id solicitud
	 */
	protected void finalizarLectura(final int idSolicitud){

		Thread t = new Thread(new Runnable() {
			public void run()
			{	
				for (int i=0;i<listeners.size();i++) {
					listeners.get(i).finalizarLectura(idSolicitud);	    
				}
			}
		});
		t.setName("finalizarLectura_clienteReader");
		t.start();
	}
}