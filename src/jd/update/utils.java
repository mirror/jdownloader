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

package jd.update;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class utils {

    public static Object loadObject(File fileInput) {

        Object objectLoaded = null;
        if (fileInput != null) {

            try {
                FileInputStream fis = new FileInputStream(fileInput);
                ObjectInputStream ois = new ObjectInputStream(fis);
                objectLoaded = ois.readObject();
                ois.close();
                return objectLoaded;
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
        return null;

    }

    public static String getLocalFile(File file) {
        if (!file.exists()) { return ""; }
        BufferedReader f;
        try {
            f = new BufferedReader(new FileReader(file));

            String line;
            StringBuilder ret = new StringBuilder();
            String sep = System.getProperty("line.separator");
            while ((line = f.readLine()) != null) {
                ret.append(line + sep);
            }
            f.close();
            return ret.toString();
        } catch (IOException e) {

            e.printStackTrace();
        }
        return "";
    }

    public static void saveObject(Object objectToSave, File fileOutput) {

        if (fileOutput != null) {

            if (fileOutput.exists()) {
                fileOutput.delete();
            }
            try {
                FileOutputStream fos = new FileOutputStream(fileOutput);

                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(objectToSave);
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}