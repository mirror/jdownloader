package jd.unrar;

import java.io.File;

public class testUnzip {
	public static void main(String[] argv) {
	    
		UnZip u = new UnZip(new File("D:/jdtest2/upd/danishRoyalty.zip.new.zip"));
		File[] files = u.extract();
		for (int i = 0; i < files.length; i++) {
			System.out.println(files[i].getAbsolutePath());
		}
	}
}
