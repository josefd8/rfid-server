import java.sql.*;
import java.util.concurrent.ArrayBlockingQueue;


/**
 *  Implementa funcionalidades para la conexión con una BD, y la inserción de sentencias
 * SQL
 * @author José Fernández
 */
public class PostgreSQL implements Runnable{

	private String db_server;
	private String db_name;
	private String db_user;
	private String db_pass;
	private String db_connection_string;
	private log_file log = new log_file();
	private boolean last_state;
	private Connection conexion;
	private int reintentosConexion = 1;
	private static ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(1024);

	public boolean isLast_state() {
		return last_state;
	}

	public PostgreSQL(String connection_string) {
		this.db_connection_string = connection_string;
	}

	public PostgreSQL(String db_server, String db_name, String db_user, String db_pass) { 
		super();
		this.db_server = db_server;
		this.db_name = db_name;
		this.db_user = db_user;
		this.db_pass = db_pass;
		this.db_connection_string = "jdbc:postgresql://" + this.db_server + ":5432/" + this.db_name + "?user=" + this.db_user + "&password=" + this.db_pass + "";
		iniciarCola();
	}

	private void iniciarCola(){
		Thread hilo = new Thread(this);
		hilo.setName("query_queue_PostgreSQL");
		hilo.start();
	}

	@Override
	public void run() {

		while(true){
			String nv = null;
			try {					
				nv = queue.take();
				insertar(nv);
				RFIDMain.ResultadoInsercionBD(nv,"OK");
				Thread.sleep(100);	
			} catch (InterruptedException e) {
				RFIDMain.ResultadoInsercionBD(nv,e.getMessage());
			} catch (SQLException e){
				RFIDMain.ResultadoInsercionBD(nv,e.getMessage());
			} catch (Exception e){
				RFIDMain.ResultadoInsercionBD(nv,e.getMessage());
				break;
			}
		}
		RFIDMain.ResultadoInsercionBD("Ha terminado de forma inesperada el hilo de procesamiento ","");

	}

	/**
	 * Coloca un nuevo query en cola para su posterior inserción. Esta cola se maneja en
	 * un hilo de ejecución independiente. Las sentencias son administradas por el método
	 * FIFO (First in, first out). 
	 * @throws NullPointerException Si el elemento a ser insertado en nulo.
	 * @throws InterruptedException Si ocurrió una interrupción durante la espera.
	 * @param sql Sentencia SQL a insertar
	 */
	public void insertarEnCola(String sql) throws InterruptedException, NullPointerException {
		queue.put(sql);	
	}

	private void iniciarConexion() throws NoConnectionException{

		try {
			Class.forName("org.postgresql.Driver");
			this.conexion = DriverManager.getConnection(this.db_connection_string);
			this.reintentosConexion = 1;
		} catch (SQLException e) {

			this.reintentosConexion = this.reintentosConexion + 1;

			if (this.reintentosConexion ==3) {
				this.conexion = null;
				this.reintentosConexion = 1;
				throw new NoConnectionException(e.getMessage());
			} else {
				iniciarConexion();
			}

		} catch (ClassNotFoundException e) {
			System.out.println("iniciarConexion: " + e.getMessage());
		}

	} 

	public ResultSet consulta(String sql){
		ResultSet resultado = null;

		try {
			iniciarConexion();
			Class.forName("org.postgresql.Driver");
			Statement comando = this.conexion.createStatement();
			resultado = comando.executeQuery(sql);
			conexion.close();
			last_state=true;
		} catch(Exception e) {
			System.out.println(e.getMessage());
			log.escribir("PostgreSQL: " + e.getMessage(),"");
			last_state=false;
		}
		return resultado;
	}

	/**
	 * Inserta directamente a la BD la sentencia SQL pasada como String.
	 * @param sql Query a insertar en la BD.
	 * @return Verdadero si el primer resultado es un objeto ResultSet, falso si
	 * no hay resultado.
	 * @throws SQLException Si se encuentra un error a la hora de accesar la BD.
	 * @throws Exception Si algún otro error ocurre.
	 */
	public boolean insertar(String sql) throws SQLException, Exception {
		boolean codigo;

		if (!(this.conexion == null)) {
			iniciarConexion();
			Statement comando = this.conexion.createStatement();
			codigo = comando.execute(sql);
			conexion.close();
			return codigo;
		}
		return false;
	}

	public String insertar_s(String sql) throws SQLException, NoConnectionException {
		String codigo;

		iniciarConexion();
		Statement comando = this.conexion.createStatement();
		ResultSet rs = comando.executeQuery(sql);
		rs.next();
		codigo = rs.getString(1);
		conexion.close();

		return codigo;	
	}

}
