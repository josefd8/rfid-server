import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Esta clase implementa funciones para la compresión de archivos en ficheros .zip
 * @author José Fernández
 */
public class zip_files implements Runnable{

	List<String> fileList;
	private String rutaArchivoZipFinal;
	private String rutaDirectorioComprimir;
	private String nombreFinalZip;
	private String zipFile;

	/**
	 * Crea nuevo objeto para la compresión de archivos contenidos en una carpeta.
	 * @param rutaArchivoZipFinal Ruta donde se almacenará el archivo zip final,
	 * ejemplo: C\carpeta. Si la ruta no existe, se creará automáticamente.
	 * @param rutaDirectorioComprimir Ruta del directorio donde se encuentran los
	 * archivos a comprimir, ejemplo C\carpeta. No se incluiran carpetas/directorios
	 * en la compresión.
	 * @param nombreFinalZip Nombre que tendrá el archivo zip final.
	 */
	public zip_files(String rutaArchivoZipFinal,
			String rutaDirectorioComprimir, String nombreFinalZip) {
		super();
		this.fileList = new ArrayList<String>();
		this.rutaArchivoZipFinal = rutaArchivoZipFinal;
		this.rutaDirectorioComprimir = rutaDirectorioComprimir;
		this.nombreFinalZip = nombreFinalZip;
	}

	/**
	 * Inicia la compresión de archivos. Si la ruta no existe, 
	 * se crea automáticamente.
	 */
	public void comprimir(){
		
		Thread hilo1 = new Thread(this);
		hilo1.setName("comprimir_zip_files");
		hilo1.start();

	}

	/**
	 * Comprime los archivos cargados en la lista de archivos a comprimir
	 * @param zipFile dirección final y nombre que tendrá el archivo .zip 
	 * (ejemplo: "C:/archivos/comprimido.zip")
	 */
	private void zipIt(String zipFile){

		byte[] buffer = new byte[1024];

		try{

			FileOutputStream fos = new FileOutputStream(zipFile);
			ZipOutputStream zos = new ZipOutputStream(fos);

			for(String file : this.fileList){

				ZipEntry ze= new ZipEntry(file);
				zos.putNextEntry(ze);

				FileInputStream in = 
						new FileInputStream(rutaDirectorioComprimir + File.separator + file);

				int len;
				while ((len = in.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}

				in.close();
			}

			zos.closeEntry();
			zos.close();

		}catch(IOException ex){
			ex.printStackTrace();   
		}
	}

	/**
	 * Crea una lista de los archivos a comprimir. Si File es la dirección a un
	 * archivo, solo se comprimirá ese archivo, si es un directorio, se comprimira 
	 * todo el contenido del directorio. Si dentro del directorio hay otros directorios
	 * estos se ignoran, solo comprimirán archivos.
	 * @param node Ruta donde ubicar el archivo/directorio.
	 */
	private void generateFileList(File node){

		if(node.isFile()){
			fileList.add(generateZipEntry(node.toString()));
		}

		if(node.isDirectory()){

			File[] files = node.listFiles();

			for(File f : files){
				if (f.isFile()) {
					generateFileList(new File(node, f.getName()));
				}
			}
		}
	}

	private String generateZipEntry(String file){
		return file.substring(rutaDirectorioComprimir.length(), file.length());
	}

	@Override
	public void run() {
		generateFileList(new File(this.rutaDirectorioComprimir));
		File dir = new File(this.rutaArchivoZipFinal);
		dir.mkdirs(); 
		this.zipFile = this.rutaArchivoZipFinal + "/" + this.nombreFinalZip + ".zip";
		zipIt(this.zipFile);	
	}

}
