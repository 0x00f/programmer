package be.limero.file;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileManager {
	private final static Logger log = Logger.getLogger(FileManager.class.toString());

	File file;

	byte[] loadBinaryFile(String fileName) {

		try {
			file = new File(fileName);
			byte[] binary = new byte[(int) file.length()];
			FileInputStream inputStream = new FileInputStream(file);
			inputStream.read(binary, 0, binary.length);
			inputStream.close();
		} catch (Exception e) {
			log.log(Level.SEVERE, " file loading fails ", e);
		}
		return null;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
