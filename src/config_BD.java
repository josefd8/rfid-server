
/**
 * Contiene los parámetros necesarios para iniciar la conexión con la BD.
 * Estos parámetros son obtenidos de un archivo de configuración
 * @author José Fernández
 */
public class config_BD {
	
	private String db_server;
	private String db_name;
	private String db_user;
	private String db_pass;
	
	/**
	 * Servidor de la BD
	 * @return db_server, que representa el servidor remoto
	 */
	public String getDb_server() {
		return db_server;
	}
	/**
	 * Servidor de la BD
	 * @param db_server que representa el servidor remoto
	 */
	public void setDb_server(String db_server) {
		this.db_server = db_server;
	}
	/**
	 * Nombre de la base de datos
	 * @return Nombre de la base de datos
	 */
	public String getDb_name() {
		return db_name;
	}
	/**
	 * Nombre de la base de datos
	 * @param db_name Nombre de la base de datos
	 */
	public void setDb_name(String db_name) {
		this.db_name = db_name;
	}
	/**
	 * Usuario de la base de datos
	 * @return Usuario de la base de datos
	 */
	public String getDb_user() {
		return db_user;
	}
	/**
	 * Usuario de la base de datos
	 * @param db_user Usuario de la base de datos
	 */
	public void setDb_user(String db_user) {
		this.db_user = db_user;
	}
	/**
	 * Contraseña de la base de datos
	 * @return Contraseña de la base de datos
	 */
	public String getDb_pass() {
		return db_pass;
	}
	/**
	 * Contraseña de la base de datos
	 * @param db_pass Contraseña de la base de datos
	 */
	public void setDb_pass(String db_pass) {
		this.db_pass = db_pass;
	}
	
	

}
