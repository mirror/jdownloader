//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


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
