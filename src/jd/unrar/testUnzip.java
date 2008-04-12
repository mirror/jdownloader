//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.unrar;

import java.io.File;

public class testUnzip {
	public static void main(String[] argv) {
	    
		UnZip u = new UnZip(new File("D:/jd_theme_tango.jdu"), new File("d:/extract/"));
		File[] files;
        try {
            files = u.extract();
     
		for (int i = 0; i < files.length; i++) {
			System.out.println(files[i].getAbsolutePath());
		}
		
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        }
}
