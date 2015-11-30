package com.sigis.tlc.rfid.tests;
public class HelloJavaLtkMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		HelloJavaLtk app = new HelloJavaLtk();
        
        System.out.println("Starting reader.");
        app.run("172.22.22.208");   
	}

}
