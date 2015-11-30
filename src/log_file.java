import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Esta clase implementa funcionalidades para la creación de archivos en disco
 * @author José Fernández
 */
public class log_file{

	public log_file() {
		super();
	}

	private String getFechaActual() {
        Date ahora = new Date();
        SimpleDateFormat formateador = new SimpleDateFormat("dd-MM-yyyy");
        return formateador.format(ahora);
    }

	private String getHoraActual() {
        Date ahora = new Date();
        SimpleDateFormat formateador = new SimpleDateFormat("hh:mm:ss");
        return formateador.format(ahora);
    }
	
	/**
	 * Genera un archivo en la ruta especificada. 
	 * Por ejemplo escribir ("test", "historial/log_general.txt") creará una carpeta historial
	 * para almacenar el archivo log_general.txt, con el contenido "test".
	 * Si el archivo ya existe, añadirá "test" al contenido del mismo. Si no se especifica
	 * una ruta sino solo un nombre de archivo (archivolog.txt), se creará una carpeta log
	 * por defecto para almanenar el archivo
	 * @param contenido Texto a escribir en el archivo.
	 * @param ruta Ruta completa y nombre del archivo.
	 */
	public void escribir (String contenido, String ruta){
		
		if (ruta.matches("")) {
			ruta = "log/log_general.txt";
		}
		
		File new_archivo = new File(ruta);
		FileOutputStream fileStream;
		
		File rutasola = new File(ruta.replace(new_archivo.getName(), ""));
		rutasola.mkdirs();
		
		try {
			fileStream = new FileOutputStream(new_archivo, true);
			OutputStreamWriter writer = new OutputStreamWriter(fileStream, "UTF-8");
			writer.write(getFechaActual() + ' ' + getHoraActual() + '\t' + contenido + '\n');
			writer.close();
	    }catch ( IOException io ) {
	        System.out.println(io.getMessage() );
	    }
	
	}
}
