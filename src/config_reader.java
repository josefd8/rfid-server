	

/**
 * Representa la configuración del reader y parámetros de operación.
 * @author José Fernández
 */
public class config_reader {
		
		/**
		 * Entero que representa el reader en BD.
		 */
		private int idEquipoRFID;
		
		/**
		 *Identifica si se trata de un reader fijo (1), o de un handheld (2)
		 */
		private int tipo;
		
		/**
		 * Dirección IP del reader en la red.
		 */
		private String ip;
		
		/**
		 * Cadena que representa la descripción del reader.
		 */
		private String descripcion;
		
		/**
		 * Para las listas 0, representa el tiempo que debe pasar desde la ultima vez
		 * que una etiqueta fue leída por un reader para esta considerarse fuera de vista.
		 */
		private int tiempoTag;
		
		/**
		 * Establece un tiempo de espera prudencial en segundos antes de cerrar una lista.
		 * Si durante el tiempo de prórroga se lee otra etiqueta, la lista continuara activa
		 * por otro periodo de prórroga.
		 */
		private int tiempoProrroga;
		
		/**
		 * Indica si el reader en cuestión es del tipo Grabador de etiquetas
		 */
		private boolean esGrabadorEtiquetas;
		
		/**
		 * Tiempo sin recibir KeepAlive de un reader para considerarlo fuera de línea.
		 */
		private int tiempoSinActividad;
		
		/**
		 * @return {@link #tiempoSinActividad}
		 */
		public int getTiempoSinActividad() {
			return tiempoSinActividad;
		}

		/**
		 * {@link #tiempoSinActividad}
		 * @param tiempoSinActividad {@link #tiempoSinActividad}
		 */
		public void setTiempoSinActividad(int tiempoSinActividad) {
			this.tiempoSinActividad = tiempoSinActividad;
		}

		/**
		 * Crea nuevo objeto config_reader.
		 */
		public config_reader() {
			super();
		}
		
		/**
		 * @return {@link #tiempoTag}
		 */
		public int getTiempoTag() {
			return tiempoTag;
		}

		/**
		 * @param tiempoTag {@link #tiempoTag}
		 */
		public void setTiempoTag(int tiempoTag) {
			this.tiempoTag = tiempoTag;
		}

		/**
		 * {@link #idEquipoRFID}
		 * @return {@link #idEquipoRFID}
		 */
		public int getIdEquipoRFID() {
			return idEquipoRFID;
		}

		/**
		 * @param idEquipoRFID {@link #idEquipoRFID}
		 */
		public void setIdEquipoRFID(int idEquipoRFID) {
			this.idEquipoRFID = idEquipoRFID;
		}

		/**
		 * {@link #tipo}
		 * @return tipo {@link #tipo}
		 */
		public int getTipo() {
			return tipo;
		}

		/**
		 * @param tipo {@link #tipo}
		 */
		public void setTipo(int tipo) {
			this.tipo = tipo;
		}

		/**
		 * {@link #ip}
		 * @return ip {@link #ip}
		 */
		public String getIp() {
			return ip;
		}

		/**
		 * @param ip {@link #ip}
		 */
		public void setIp(String ip) {
			this.ip = ip;
		}

		/**
		 * {@link #tiempoProrroga}
		 * @return tiempoProrroga {@link #tiempoProrroga}
		 */
		public int getTiempoProrroga() {
			return tiempoProrroga;
		}

		/**
		 * @param tiempoProrroga {@link #tiempoProrroga}
		 */
		public void setTiempoProrroga(int tiempoProrroga) {
			this.tiempoProrroga = tiempoProrroga;
		}

		/**
		 * {@link #descripcion}
		 * @return descripción {@link #descripcion}
		 */
		public String getDescripcion() {
			return descripcion;
		}

		/**
		 * @param descripcion {@link #descripcion}
		 */
		public void setDescripcion(String descripcion) {
			this.descripcion = descripcion;
		}
		
		/**
		 * {@link #esGrabadorEtiquetas}
		 * @return esGrabadorEtiquetas {@link #esGrabadorEtiquetas}
		 */
		public boolean isEsGrabadorEtiquetas() {
			return esGrabadorEtiquetas;
		}

		/**
		 * @param esGrabadorEtiquetas {@link #esGrabadorEtiquetas}
		 */
		public void setEsGrabadorEtiquetas(boolean esGrabadorEtiquetas) {
			this.esGrabadorEtiquetas = esGrabadorEtiquetas;
		}
	}