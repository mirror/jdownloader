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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class getFileSignatures {

    private static void firl(File file)
    {
        int ad;
        FileInputStream fin;
        try {
            fin = new FileInputStream(file);
            int c = 0;
            BufferedReader myInput = new BufferedReader(new InputStreamReader(fin));
            System.out.print("{");
            while ((ad = myInput.read()) != -1) {
                System.out.print(ad+((c==5)?"":","));
                
                if(c++==5)
                    break;
            }
            System.out.print("}");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    public static void main(String[] args) {
        File[] file = new File("/home/dwd/.jd_home").listFiles();
        for (int i = 0; i < file.length; i++) {
            if(file[i].isFile())
            {
                System.out.println("\n"+file[i].getName()+" ");
                firl(file[i]);
            }
        }

    }

}
