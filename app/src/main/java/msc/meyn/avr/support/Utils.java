package msc.meyn.avr.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {

	public static void copyFile(InputStream source, OutputStream destination)
			throws IOException {
		byte[] buffer = new byte[source.available()];
		source.read(buffer);
		source.close();
		destination.write(buffer);
		destination.close();
	}
	
}
