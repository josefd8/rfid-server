import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.llrp.ltk.exceptions.InvalidLLRPMessageException;
import org.llrp.ltk.types.LLRPMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Esta clase toma las etiquetas enviadas desde el reader y los analiza según 
 * su tipo (RO_ACCESS_REPORT, KEEP_ALIVE, etc). Retorna una lista de objetos
 * tipo etiqueta.
 * @author José Fernández
 */

public class decoMensajeRecibido {

	private static List<etiqueta> etiquetas = new ArrayList<etiqueta>();

	public decoMensajeRecibido() {
		super();
	}

	public List<etiqueta> decodificar(LLRPMessage message){
			
		etiquetas.clear();

		try {
			Document doc = stringToDom(message.toXMLString());
			org.w3c.dom.Node nodoprincipal = doc.getFirstChild();
			if (nodoprincipal.getNodeName() == "llrp:RO_ACCESS_REPORT") {

				etiqueta nuevaEtiqueta = new etiqueta();

				Element c = (Element)nodoprincipal;
				nuevaEtiqueta.MessageID = c.getAttribute("MessageID");
				nuevaEtiqueta.descripcionMensaje = "RO_ACCESS_REPORT";
				nuevaEtiqueta.idMensaje = 1;
				nuevaEtiqueta.descripcionMensaje = "Nuevo tag desde reader";

				NodeList tagreportdata = nodoprincipal.getChildNodes();

				for(int i=0; i < tagreportdata.getLength(); i++){

					NodeList informacionTag = tagreportdata.item(i).getChildNodes();

					for(int j=0; j < informacionTag.getLength(); j++){

						String NombreTag = informacionTag.item(j).getNodeName();						
						String infotag = informacionTag.item(j).getTextContent().trim();

						if (NombreTag.matches("llrp:EPCData")) {
							nuevaEtiqueta.epc = infotag;
						}

						if (NombreTag.matches("llrp:EPC_96")) {
							nuevaEtiqueta.largoepc = "96";
							nuevaEtiqueta.epc = infotag;

						}

						if (NombreTag.matches("llrp:EPC_128")) {
							nuevaEtiqueta.largoepc = "128";
							nuevaEtiqueta.epc = infotag;
						}

						if (NombreTag.matches("llrp:ROSpecID")) {
							//ignore
						}

						if (NombreTag.matches("llrp:SpecIndex")) {
							//ignore
						}

						if (NombreTag.matches("llrp:InventoryParameterSpecID")) {
							//ignore
						}

						if (NombreTag.matches("llrp:AntennaID")) {
							nuevaEtiqueta.idAntena =Integer.parseInt(infotag);
						}


						if (NombreTag.matches("llrp:PeakRSSI")) {
							nuevaEtiqueta.PeakRSSI=infotag;
						}

						if (NombreTag.matches("llrp:ChannelIndex")) {
							//ignore
						}

						if (NombreTag.matches("llrp:FirstSeenTimestampUTC")) {
							//
						}

						if (NombreTag.matches("llrp:LastSeenTimestampUTC")) {
							nuevaEtiqueta.ultimaVezFecha = new Date();	
						}

						if (NombreTag.matches("llrp:TagSeenCount")) {
							//ignore
						}

						if (NombreTag.matches("llrp:AccessSpecID")) {
							//ignore
						}
					}

				}
				
				if (nuevaEtiqueta.ultimaVezFecha == null) {
					nuevaEtiqueta.ultimaVezFecha = new Date();
				}
				
				if (!(nuevaEtiqueta.largoepc == null)) {
					//Si la etiqueta es de 96 bits, debe producir
					//un epc de 24 digitos, si no es asi, se completa
					//con ceros
					if (nuevaEtiqueta.largoepc.matches("96")) {
						nuevaEtiqueta.epc = completar(nuevaEtiqueta.epc, 24);
					}
					if (nuevaEtiqueta.largoepc.matches("128")) {
						nuevaEtiqueta.epc = completar(nuevaEtiqueta.epc, 32);
					}
				}
				
				etiquetas.add(nuevaEtiqueta);
			}
			
			if (nodoprincipal.getNodeName() == "llrp:KEEPALIVE") {
				etiqueta nuevaEtiqueta = new etiqueta();
				Element c = (Element)nodoprincipal;
				nuevaEtiqueta.MessageID = c.getAttribute("MessageID");
				nuevaEtiqueta.descripcionMensaje = "KEEPALIVE";
				nuevaEtiqueta.idMensaje = 2;
				nuevaEtiqueta.descripcionMensaje = "Mensaje de KeepAlive";
				etiquetas.add(nuevaEtiqueta);
			}
			
			if (nodoprincipal.getNodeName() == "llrp:READER_EVENT_NOTIFICATION") {
				
				etiqueta nuevaEtiqueta = new etiqueta();
				Element c = (Element)nodoprincipal;
				nuevaEtiqueta.MessageID = c.getAttribute("MessageID");
				
				NodeList tagreportdata = nodoprincipal.getChildNodes();
				
				for(int i=0; i < tagreportdata.getLength(); i++){
					
					if (tagreportdata.item(i).getNodeName().matches("llrp:ReaderEventNotificationData")) {
						
						NodeList data = tagreportdata.item(i).getChildNodes();
						
						for(int j=0; i < data.getLength(); j++){
							if (data.item(j).getNodeName().matches("llrp:AntennaEvent")) {
								
								NodeList eventoAntena = data.item(j).getChildNodes();
								
								for(int k=0; i < eventoAntena.getLength(); k++){
									
									if (eventoAntena.item(k).getNodeName().matches("llrp:EventType")) {
										
										if (eventoAntena.item(k).getTextContent().matches("Antenna_Connected")) {
											nuevaEtiqueta.descripcionMensaje = "Antena conectada";
											nuevaEtiqueta.idMensaje = 3;
										} else {
											nuevaEtiqueta.descripcionMensaje = "Antena desconectada";
											nuevaEtiqueta.idMensaje = 4;
										}	
									}

									if (eventoAntena.item(k).getNodeName().matches("llrp:AntennaID")) {
										nuevaEtiqueta.idAntena = Integer.parseInt(eventoAntena.item(k).getTextContent());
										etiquetas.add(nuevaEtiqueta);
										return etiquetas;
										
									}
								}
							}
						}
					}
				}
				etiquetas.add(nuevaEtiqueta);
			}
			

		} catch (SAXException | ParserConfigurationException | IOException
				| InvalidLLRPMessageException e) {
			e.printStackTrace();
		}


		return etiquetas;
	}

	private Document stringToDom(String xmlSource)      
			throws SAXException, ParserConfigurationException, IOException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new InputSource(new StringReader(xmlSource)));
	}
	
	private String completar(String epc, int mascara){
		String mask = "";
		int i;
		
		i = mascara - epc.length();
		
		for (int j = 1; j <= i; j++) {
			mask = mask + "0";
		}
		
		return (mask + epc);
	}

}
