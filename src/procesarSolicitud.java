import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Esta clase se encarga de procesar los comandos a distancia que recibe el
 * parser. Los comandos poseen una estructura XML y son recibidos como una cadena
 * de carácteres (String).
 * @author José Fernández
 */
public class procesarSolicitud {

	private String error = "";
	private String comando;
	private int idOperacion;
	public String requiereRespuesta;
	private Node nodo;
	private String idsEstado;
	private String tipoLectura;
	private String filtroLecturaOFF;
	private List<String> filtros = new ArrayList<String>();
	private int tiempoLectura;
	private String automatico;
	private String tipoConsulta;
	private String codigosOpConsulta;
	private String tipoComando;
	private String accionComando;
	private int tipoHandheld;
	private int idHandheld;
	private int antenaHandheld;
	private String epcHandheld;
	private String horaHandheld;
	

	public void setTipoHandheld(int tipoHandheld) {
		this.tipoHandheld = tipoHandheld;
	}
	
	public void setError(String error) {
		this.error = error;
	}

	public void setComando(String comando) {
		this.comando = comando;
	}

	public void setIdOperacion(int idOperacion) {
		this.idOperacion = idOperacion;
	}

	public void setRequiereRespuesta(String requiereRespuesta) {
		this.requiereRespuesta = requiereRespuesta;
	}

	public void setIdsEstado(String idsEstado) {
		this.idsEstado = idsEstado;
	}

	public void setTipoLectura(String tipoLectura) {
		this.tipoLectura = tipoLectura;
	}

	public void setFiltroLecturaOFF(String filtroLecturaOFF) {
		this.filtroLecturaOFF = filtroLecturaOFF;
	}

	public void setFiltros(List<String> filtros) {
		this.filtros = filtros;
	}

	public void setTiempoLectura(int tiempoLectura) {
		this.tiempoLectura = tiempoLectura;
	}

	public void setAutomatico(String automatico) {
		this.automatico = automatico;
	}

	public void setTipoConsulta(String tipoConsulta) {
		this.tipoConsulta = tipoConsulta;
	}

	public void setCodigosOpConsulta(String codigosOpConsulta) {
		this.codigosOpConsulta = codigosOpConsulta;
	}

	public void setTipoComando(String tipoComando) {
		this.tipoComando = tipoComando;
	}

	public void setAccionComando(String accionComando) {
		this.accionComando = accionComando;
	}

	public void setIdHandheld(int idHandheld) {
		this.idHandheld = idHandheld;
	}


	public void setAntenaHandheld(int antenaHandheld) {
		this.antenaHandheld = antenaHandheld;
	}


	public void setEpcHandheld(String epcHandheld) {
		this.epcHandheld = epcHandheld;
	}


	public void setHoraHandheld(String horaHandheld) {
		this.horaHandheld = horaHandheld;
	}


	//****************Comunes a todas las solicitudes*************************************
	/**
	 * Si existe un error durante la decodificación de la solicitud, retorna un String
	 * especificando la razón del error. Si no hay error, se retorna vacío "".
	 * @return Descripción del error.
	 */
	public String getError() {
		return error;
	}

	/**
	 * Especifica el tipo de solicitud recibida (LECTURA, CONSULTA, ESTADO o COMANDO)
	 * Es un elemento común a todas las solicitudes.
	 * @see #getIdOperacion
	 */
	public String getComando() {
		return comando;
	}

	/**
	 * Especifica el ID de operación. Este valor es único para cada solicitud
	 * Es un elemento común a todas las solicitudes.
	 * @see #getComando
	 */
	public int getIdOperacion() {
		return idOperacion;
	}
	
	/**
	 * Especifica si la solicitud debe generar una respuesta hacia el webservice
	 * a traves de la conexión TCP.
	 */
	public String getRequiereRespuesta(){
		return requiereRespuesta;
	}

	/**
	 * Retorna la solicitud en formato Node (xml)
	 * Es un elemento común a todas las solicitudes.
	 */
	public Node getNodo() {
		return nodo;
	}
	//***********************************************************************************


	//****************Para las solicitudes de LECTURA*************************************
	/**
	 * Para las consultas de estado (comando = ESTADO), retorna los ID's de los readers
	 * a los que se les consulta el estado como números separados por coma (1,2,3...n).
	 * de estado
	 * @see #getIdsEstado
	 * @see #getTipoLectura 
	 * @see #getFiltroLecturaOFF
	 * @see #getFiltros
	 * @see #getTiempoLectura
	 * @see #getAutomatico
	 */
	public String getIdsEstado() {
		return idsEstado;
	}

	/**
	 * Retorna "ON" para solicitar un inicio de nueva lectura, "OFF" para detener
	 * una lectura actual. válido para comando = "LECTURA".
	 * @see #getIdsEstado
	 * @see #getTipoLectura 
	 * @see #getFiltroLecturaOFF
	 * @see #getFiltros
	 * @see #getTiempoLectura
	 * @see #getAutomatico
	 */
	public String getTipoLectura() {
		return tipoLectura;
	}

	/**
	 * Si tipoLectura = OFF, filtroLecturaOFFcontiene los códigos de operación a detener.
	 * válido para comando = "LECTURA".
	 * @see #getIdsEstado
	 * @see #getTipoLectura 
	 * @see #getFiltroLecturaOFF
	 * @see #getFiltros
	 * @see #getTiempoLectura
	 * @see #getAutomatico
	 */
	public String getFiltroLecturaOFF() {
		return filtroLecturaOFF;
	}

	/** 
	 * Si tipoLectura = "ON", se retorna una lista de readers|antenas, ejemplo "20,21,22|1,2,3.
	 * válido para comando = "LECTURA".
	 * @see #getIdsEstado
	 * @see #getTipoLectura 
	 * @see #getFiltroLecturaOFF
	 * @see #getFiltros
	 * @see #getTiempoLectura
	 * @see #getAutomatico
	 */
	public List<String> getFiltros() {
		return filtros;
	}

	/**
	 * Si tipoLectura = "ON", retorna el tiempo que durará la lectura.
	 * válido para comando = "LECTURA".
	 * @see #getIdsEstado
	 * @see #getTipoLectura 
	 * @see #getFiltroLecturaOFF
	 * @see #getFiltros
	 * @see #getTiempoLectura
	 * @see #getAutomatico
	 */
	public int getTiempoLectura() {
		return tiempoLectura;
	}

	/**
	 * Si tipoLectura = "ON", retorna "TRUE" si la lectura debe retornar las etiquetas
	 * leídas en tiempo real, "FALSE", si deben ser enviadas todas como lote al final de
	 * la operación. válido para comando = "LECTURA".
	 * @see #getIdsEstado
	 * @see #getTipoLectura 
	 * @see #getFiltroLecturaOFF
	 * @see #getFiltros
	 * @see #getTiempoLectura
	 * @see #getAutomatico
	 */
	public String getAutomatico() {
		return automatico;
	}
	//***********************************************************************************


	//****************Para las solicitudes de CONSULTA*************************************	
	/**
	 * Especifica el tipo de consulta. Si "SIMPLE", codigosOpConsulta especifica los
	 * códigos de operación a consultar. Si "LISTA", se consultan todos los códigos.
	 * @see #getTipoConsulta
	 * @see #getCodigosOpConsulta
	 */
	public String getTipoConsulta() {
		return tipoConsulta;
	}

	/**
	 * Para consultas tipo "SIMPLE", especifica los códigos de operación a consultar.
	 * @see #getTipoConsulta
	 * @see #getCodigosOpConsulta
	 */
	public String getCodigosOpConsulta() {
		return codigosOpConsulta;
	}
	//***********************************************************************************

	//****************Para las solicitudes de COMANDO*************************************	
	/**
	 * Indica el tipo de comando. Este parámetro indica operaciones sobre el parser.
	 * @see #getTipoComando
	 * @see #getAccionComando
	 */
	public String getTipoComando() {
		return tipoComando;
	}

	/**
	 * Si tipoComando = "COMANDO", especifica la acción a realizar sobre el parser.
	 * @see #getTipoComando
	 * @see #getAccionComando
	 */
	public String getAccionComando() {
		return accionComando;
	}
	//***********************************************************************************

	
	//****************Para las solicitudes de HANDHELD***********************************
	/**
	 * Indica el tipo de operacion que realiza el handheld. 
	 * 0: Lectura de nueva etiqueta
	 * 1: Handheld conectado
	 * 2: Handheld desconectado
	 * 3: keepAlive desde Handheld
	 */
	public int getTipoHandheld() {
		return tipoHandheld;
	}

	/**
	 * Codigo numerico que representa el ID del Handheld en Base de datos.
	 */
	public int getIdHandheld() {
		return idHandheld;
	}

	/**
	 * Indica la antena por la que fue leido una etiqueta en particular en el Handheld.
	 */
	public int getAntenaHandheld() {
		return antenaHandheld;
	}

	/**
	 * Indica el EPC de la etiqueta leída.
	 */
	public String getEpcHandheld() {
		return epcHandheld;
	}

	/**
	 * Hora en la que fue leida la etiqueta
	 */
	public String getHoraHandheld() {
		return horaHandheld;
	}

	//***********************************************************************************

	public void setNodo(Node nodo) {
		this.nodo = nodo;
	}
	
	/**
	 * Procesa la solicitud OTA. Si el formato y la información contenida en el comando es
	 * válida, getError retorna vacío (""), de lo contrario indica la razón del error. 
	 * @see #getError
	 * @param comando solicitud xml en formato String.
	 */
	public void procesar(String comando){

		try {
			Document d = stringToDom(comando);
			org.w3c.dom.Node nodoprincipal = d.getFirstChild();

			if (nodoprincipal.getNodeName() == "solicitud") {
				this.setNodo(nodoprincipal);
				NodeList nodos = nodoprincipal.getChildNodes();
				analizar_comando(nodos);
				obtener_id_operacion(nodos);
				obtener_requiereRespuesta(nodos);
				obtener_parametros(nodos);
			} else{
				this.error = "'" +nodoprincipal.getNodeName() + "' no se reconoce como un encabezado válido.";
			}

		} catch (SAXException | ParserConfigurationException | IOException e) {
			this.error= "La solicitud no posee un formato XML válido.";
		}
	}

	private void analizar_comando(NodeList nodos){

		if (this.getError().matches("")) {
			try {

				if (nodos.item(0).getNodeName().matches("comando")) {
					this.comando = nodos.item(0).getTextContent();
					boolean desconocido = true;
					if (this.comando.matches("ESTADO")) {
						desconocido = false;
					}
					if (this.comando.matches("LECTURA")) {
						desconocido = false;
					}
					if (this.comando.matches("CONSULTA")) {
						desconocido = false;
					}
					if (this.comando.matches("CONFIGURACION")) {
						desconocido = false;
					}
					if (this.comando.matches("HANDHELD")) {
						desconocido = false;
					}
					if (desconocido == true) {
						if (this.comando.matches("")) {
							this.error = "El valor del identificador del comando no puede ser vacío.";
							return;
						} else {
							this.error = "Tipo de comando '" + this.comando + "' no reconocido.";
							return;
						}
					}
				} else {
					this.error = "No se encontró el identificador de comando para esta solicitud.";
					return;			
				}
			} catch (DOMException e) {
				this.error = "Se encontró un error al procesar el tipo de comando.";
				return;
			} catch (NullPointerException e){
				this.error = "Error al procesar la solicitud. El cuerpo de la solicitud esta vacío.";
				return;
			}
		}
	}

	private void obtener_id_operacion(NodeList nodos){

		if (this.getError().matches("")) {

			try {
				if (nodos.item(1).getNodeName().matches("cod_ope")) {

					String id = nodos.item(1).getTextContent();

					try {
						this.idOperacion = Integer.parseInt(id);
					} catch (NumberFormatException e) {
						this.error = "El código de operación no posee formato válido.";
					}
				} else {
					this.error = "No se encontró código de operación.";
					return;
				}
			} catch (DOMException e) {
				this.error = "Error al procesar el código de la operación: "
						+ e.getMessage();
				return;
			} catch(NullPointerException e){
				this.error = "Error al procesar la solicitud. No se encontró identificador del código de operación.";
				return;
			}
		}
	}
	
	private void obtener_requiereRespuesta(NodeList nodos){
		
		if (this.getError().matches("")) {

			try {
				if (nodos.item(2).getNodeName().matches("respuesta")) {

					String respuesta = nodos.item(2).getTextContent();

					if (respuesta.matches("TRUE") || (respuesta.matches("FALSE"))) {
						this.requiereRespuesta = respuesta;
					} else {
						this.error = "No se reconoce '" + respuesta + "' como " +
								"un valor válido para <respuesta>, se esperaba TRUE/FALSE.";
						return;
					}
					
				} else {
					this.error = "No se reconoce " + nodos.item(2).getNodeName() + " como " +
							"una etiqueta válida de respuesta, se esperaba <respuesta>.";
					return;
				}
			} catch (DOMException e) {
				this.error = "Error al procesar la etiqueta de respuesta: "
						+ e.getMessage();
				return;
			} catch(NullPointerException e){
				this.error = "Error al procesar la solicitud. No se encontró etiqueta de <respuesta>.";
				return;
			}
		}
		
		
	}

	private void obtener_parametros(NodeList nodos){

		if (this.getError().matches("")) {

			NodeList parametros = null;

			try {
				if (nodos.item(3).getNodeName().matches("parametros")) {		

					try {
						parametros = nodos.item(3).getChildNodes();

						if (parametros.getLength() == 0) {
							this.error = "Lista de parámetros no puede ser vacía.";
							return;
						}						
					} catch (Exception e1) {
						this.error = "No se encontró lista de parámetros.";
						return;
					}

					if (this.comando.matches("ESTADO")) {
						try {
							if (parametros.item(0).getNodeName().equalsIgnoreCase("id")) {

								String cadenaIds = parametros.item(0).getTextContent();
								this.idsEstado = cadenaIds;

								//verificamos si los readers son numeros
								String[] r = cadenaIds.split(",");
								if (!(cadenaIds.matches(""))) {
									for (String s : r) {
										try {
											@SuppressWarnings("unused")
											int i = Integer.valueOf(s);
										} catch (NumberFormatException e) {
											this.error = "Los readers proporcionados no poseen formato válido.";
											return;
										}
									}
								}

							} else {
								this.error = "No se encontró identificador <id> para tipo de comando ESTADO.";
								return;
							}
						} catch (DOMException | NullPointerException e) {
							this.error = "No se encontró identificador <id> para tipo de comando ESTADO.";
							return;
						}
					}

					if (this.comando.matches("LECTURA")) {
						try {

							this.tipoLectura = parametros.item(0).getTextContent();

							if (!(parametros.item(0).getNodeName().matches("tipo"))) {
								this.error = "No se encontró el identificador <tipo> para la solicitud de lectura.";
								return;
							}

							if (parametros.item(1).getNodeName().matches("filtro")) {
								NodeList nodosFiltro = parametros.item(1).getChildNodes();

								if (nodosFiltro.getLength() == 0) {
									this.error = "No se encuentran filtros válidos en la solicitud de lectura.";
									return;
								}


								if (this.tipoLectura.matches("ON")) {

									for (int i = 0; i < nodosFiltro.getLength(); i++) {
										Element f = (Element) nodosFiltro.item(i);
										this.filtros.add(f.getAttribute("nombre") + "|" + f.getAttribute("antena"));

										if (((f.getAttribute("nombre").isEmpty())) || (f.getAttribute("antena").isEmpty())) {
											this.error = "Uno de los parámetros para la solicitud de lectura no tiene el formato correcto.";
											return;
										}
									}
								}

								if (this.tipoLectura.matches("OFF")) {
									Element c = (Element) nodosFiltro.item(0);
									this.filtroLecturaOFF = c.getAttribute("cod_ope");
									return;
								}
							} else {
								this.error = "No se encontró la etiqueta de <filtro> para la solicitud de lectura.";
								return;
							}

							if ((!(this.tipoLectura.matches("OFF"))) && (!(this.tipoLectura.matches("ON")))){
								this.error = this.tipoLectura + " No se reconoce como un identificador válido" + 
										" para la solicitud de lectura. Los valores válidos son ON/OFF.";
								return;
							}



							try {	
								if (parametros.item(2).getNodeName().matches("tiempo")) {
									this.tiempoLectura = Integer.parseInt(parametros.item(2).getTextContent());
								} else {
									this.error = "No se reconoce " + parametros.item(2).getNodeName() + " como" +
											"identificador válido para tiempo de lectura.";
									return;
								}
							} catch (NumberFormatException e) {
								this.error = "La variable de tiempo no posee un formato válido.";
								return;
							} catch (NullPointerException e){
								this.error = "No se encontró el identificador de tiempo.";
								return;
							}


							try {	
								if (parametros.item(3).getNodeName().matches("automatico")) {
									
									String valor = parametros.item(3).getTextContent();

									if (((valor.matches("TRUE"))) || ((valor.matches("FALSE")))) {
										this.automatico = valor;
									} else {
										this.error = "No se reconoce " + valor + " como" +
												" identificador válido para automático. Solo admite TRUE/FALSE.";
										return;
										
									}

								} else {
									this.error = "No se encontro la etiqueta <automatico>";
									return;
								}
							} catch (NullPointerException e){
								this.error = "No se encontró el identificador para automático.";
								return;
							}

						} catch (NullPointerException e) {
							this.error = "No se encontró el identificador para el tipo de lectura (ON/OFF).";
							return;
						} catch (DOMException e) {
							this.error = "Se encontró un problema para procesar la solicitud de lectura. Los parámetros no " +
									"tienen el formato correcto.";
							return;
						}
					}


					try {
						if (this.comando.matches("CONSULTA")) {
							this.tipoConsulta = parametros.item(0).getTextContent();

							if (this.tipoConsulta.matches("SIMPLE")) {
								this.codigosOpConsulta = parametros.item(1).getTextContent();
								
								//Consultamos que los ID de las operaciones sean numeros
								String[] rc = this.codigosOpConsulta.split(",");
								if (!(this.codigosOpConsulta.matches(""))) {
									for (String sc : rc) {
										try {
											@SuppressWarnings("unused")
											int i = Integer.valueOf(sc);
										} catch (NumberFormatException e) {
											this.error = "Los códigos de operación proporcionados no poseen formato válido.";
											return;
										}
									}
								}

							}

							if (this.tipoConsulta.matches("LISTA")) {
								@SuppressWarnings("unused")
								Element f = (Element) parametros.item(1);
								this.codigosOpConsulta = "";
							}

							if ((!(this.tipoConsulta.matches("LISTA"))) && (!(this.tipoConsulta.matches("SIMPLE")))) {
								this.error = "No se reconoce el tipo de consulta. Los valores válidos son LISTA/SIMPLE";
								return;
							}
						}
					} catch (DOMException | NullPointerException e) {
						this.error = "No se encontró etiqueta para el tipo de consulta.";
						return;
					}


					try {
						if (this.comando.matches("CONFIGURACION")) {
							this.tipoComando = parametros.item(0).getTextContent();
							this.accionComando = parametros.item(1).getTextContent();
						}
					} catch (DOMException | NullPointerException e) {
						this.error = "Uno o varios parámetros no fueron encontrados para el comando CONFIGURACION.";
						return;
					}
					
					
					if (this.comando.matches("HANDHELD")) {
						try {
							this.tipoHandheld = Integer.valueOf(parametros.item(0).getTextContent());
							this.idHandheld = Integer.valueOf(parametros.item(1).getTextContent());
							
							if (this.tipoHandheld == 0) {
								this.antenaHandheld = Integer.valueOf(parametros.item(2).getTextContent());
								this.epcHandheld = parametros.item(3).getTextContent();
								this.horaHandheld = parametros.item(4).getTextContent();
							} 

						} catch (DOMException | NullPointerException | NumberFormatException e) {
							this.error = "Uno o varios parámetros no fueron encontrados para el comando HANDHELD.";
							return;
						}
					}

				} else {
					this.error = "No se reconoce " + nodos.item(3).getNodeName() + " como un identificador " +
							"válido, se esperaba <parametros>.";
					return;
				}
			} catch (DOMException e) {
				this.error = "Error al procesar los parámetros de la solicitud: " + e.getMessage();
				return;
			} catch (NullPointerException e){
				this.error = "No se encontraron parámetros en la solicitud.";
				return;		
			}
		}		
	}

	private Document stringToDom(String xmlSource)      
			throws SAXException, ParserConfigurationException, IOException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new InputSource(new StringReader(xmlSource)));
	}
}
