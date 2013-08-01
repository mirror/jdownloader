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

package jd.nutils.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.appwork.utils.Regex;
import org.jdownloader.controlling.FileCreationManager;

public class Zip {
    private File            destinationFile;
    /**
     * Dateien/Ordner die nicht hinzugefügt werden sollen
     */
    public LinkedList<File> excludeFiles = new LinkedList<File>();
    private Pattern         excludeFilter;
    /**
     * Füllt die zipDatei auf die gewünschte größe auf
     */
    public int              fillSize     = 0;
    private File[]          srcFiles;
    private boolean         deleteAfterPack;

    /**
     * 
     * @param srcFiles
     *            Datei oder Ordner die dem Ziparchiv hinzugefügt werden sollen.
     * @param destinationFile
     *            die Zieldatei
     */
    public Zip(File srcFile, File destinationFile) {
        this(new File[] { srcFile }, destinationFile);
    }

    /**
     * 
     * @param srcFiles
     *            Dateien oder Ordner die dem Ziparchiv hinzugefügt werden sollen.
     * @param destinationFile
     *            die Zieldatei
     */
    public Zip(File[] srcFiles, File destinationFile) {
        this.srcFiles = srcFiles;
        this.destinationFile = destinationFile;

    }

    private java.util.List<File> addFileToZip(String path, String srcFile, ZipOutputStream zip) throws Exception {
        ArrayList<File> ret = new ArrayList<File>();
        if (srcFile.endsWith("Thumbs.db")) { return null; }
        if (excludeFilter != null) {
            if (Regex.matches(srcFile, excludeFilter)) {

                System.out.println("Filtered: " + srcFile);
                return ret;
            }
        }
        File folder = new File(srcFile);
        if (excludeFiles != null && excludeFiles.contains(folder)) { return ret; }
        if (folder.isDirectory()) {
            ret.addAll(addFolderToZip(path, srcFile, zip));
            if (this.deleteAfterPack) FileCreationManager.getInstance().delete(new File(srcFile));
        } else {
            byte[] buf = new byte[1024];
            int len;
            FileInputStream in = new FileInputStream(srcFile);
            if (path == null || path.trim().length() == 0) {
                zip.putNextEntry(new ZipEntry(folder.getName()));
            } else {
                zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
            }

            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }
            in.close();
            if (this.deleteAfterPack) FileCreationManager.getInstance().delete(new File(srcFile));
            ret.add(new File(srcFile));
        }
        return ret;

    }

    private java.util.List<File> addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws Exception {
        ArrayList<File> ret = new ArrayList<File>();
        File folder = new File(srcFolder);
        if (excludeFiles.contains(folder)) { return ret; }
        for (String fileName : folder.list()) {
            if (excludeFilter != null) {
                if (Regex.matches(fileName, excludeFilter)) {
                    System.out.println("Filtered: " + fileName);
                    continue;
                }
            }
            if (path.equals("")) {
                ret.addAll(addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip));
            } else {
                ret.addAll(addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip));
            }
        }
        return ret;

    }

    public void setExcludeFilter(Pattern compile) {
        excludeFilter = compile;

    }

    /**
     * wird aufgerufen um die Zip zu erstellen
     * 
     * @throws Exception
     */
    public java.util.List<File> zip() throws Exception {
        ArrayList<File> ret = new ArrayList<File>();
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;

        fileWriter = new FileOutputStream(destinationFile);
        zip = new ZipOutputStream(fileWriter);
        for (File element : srcFiles) {
            if (element.isDirectory()) {
                ret.addAll(addFolderToZip("", element.getAbsolutePath(), zip));
            } else if (element.isFile()) {
                ret.addAll(addFileToZip("", element.getAbsolutePath(), zip));
            }
        }

        zip.flush();
        zip.close();
        int toFill = (int) (fillSize - destinationFile.length());

        if (toFill > 0) {
            byte[] sig = new byte[] { 80, 75, 3, 4, 20, 0, 8, 0, 8, 0 };
            toFill -= sig.length;
            FileInputStream in = new FileInputStream(destinationFile);
            File newTarget = new File(destinationFile.getAbsolutePath() + ".jd");
            FileOutputStream out = new FileOutputStream(newTarget);
            out.write(sig);
            out.write(new byte[toFill]);
            int c;
            while ((c = in.read()) != -1) {
                out.write(c);
            }
            in.close();
            out.close();
            FileCreationManager.getInstance().delete(destinationFile);
            newTarget.renameTo(destinationFile);
        }
        return ret;

    }

    public void setDeleteAfterPack(boolean b) {
        deleteAfterPack = b;

    }
}
