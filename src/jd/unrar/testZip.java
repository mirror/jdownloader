package jd.unrar;

import java.io.File;

import jd.utils.JDUtilities;

public class testZip {
public static void main(String[] args) {
	Zip zip = new Zip(new File(JDUtilities.getJDHomeDirectoryFromEnvironment(), "jd"), new File(JDUtilities.getJDHomeDirectoryFromEnvironment(), "jd.zip"));
	zip.excludeFiles.add(new File(new File(JDUtilities.getJDHomeDirectoryFromEnvironment(), "jd"), "config"));
	zip.fillSize=1048576;
	try {
		zip.zip();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
}
