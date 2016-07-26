package be.limero.file;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;

public class FileManager {
	private final static Logger log = Logger.getLogger(FileManager.class.toString());

	public static byte[] loadBinaryFile(String fileName) {
		File file;

		try {
			file = new File(fileName);
			byte[] binary = new byte[(int) file.length()];
			FileInputStream inputStream = new FileInputStream(file);
			inputStream.read(binary, 0, binary.length);
			inputStream.close();
			return binary;
		} catch (Exception e) {
			log.log(Level.SEVERE, " file loading fails ", e);
		}
		return null;
	}

}
