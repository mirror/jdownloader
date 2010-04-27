//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jd.config.DatabaseConnector;
import jd.utils.JDUtilities;

public class JDUpdateUtils {

    public static File backupDataBase() {
        synchronized (DatabaseConnector.LOCK) {
            String[] filenames = new String[] { "JDU.cfg", "WEBUPDATE.cfg", "database.properties", "database.script" };
            byte[] buf = new byte[8192];
            File file = JDUtilities.getResourceFile("backup/database.zip");
            if (file.exists()) {
                File old = JDUtilities.getResourceFile("backup/database_" + file.lastModified() + ".zip");
                file.getParentFile().mkdirs();
                if (file.exists()) {
                    file.renameTo(old);
                }
                file.delete();
            } else {
                file.getParentFile().mkdirs();
            }
            try {
                String outFilename = file.getAbsolutePath();
                ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));

                for (int i = 0; i < filenames.length; i++) {
                    File filein = JDUtilities.getResourceFile("config/" + filenames[i]);
                    if (!filein.exists()) continue;
                    FileInputStream in = new FileInputStream(filein.getAbsoluteFile());
                    out.putNextEntry(new ZipEntry(filenames[i]));
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.closeEntry();
                    in.close();
                }
                out.close();
            } catch (Exception e) {
                return null;
            }
            return file;
        }
    }

}
