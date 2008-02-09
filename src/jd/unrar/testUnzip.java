package jd.unrar;

import java.io.File;

public class testUnzip {
	public static void main(String[] argv) {
		UnZip u = new UnZip(new File("/home/dwd/c/z.zip"));
		File[] files = u.extract();
		for (int i = 0; i < files.length; i++) {
			System.out.println(files[i].getAbsolutePath());
		}
	}
}
