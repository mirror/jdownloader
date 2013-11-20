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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.logging.LogController;

public class UnZip {
    public boolean              autoDelete  = false;

    // The buffer for reading/writing the ZipFile data //
    protected byte[]            b;

    protected SortedSet<String> dirsMade;

    // Der Zielpfad in dem entpackt werden soll

    private File                targetPath  = null;

    protected boolean           warnedMkDir = false;

    protected ZipFile           zipF;

    private File                zipFile     = null;

    private boolean             overwrite   = true;

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * Konstruktor in dem nur das zipFile angegeben wird
     * 
     * @param zipFile
     */

    public UnZip(File zipFile) {
        this(zipFile, null);
    }

    /**
     * Konstruktor mit einem bestimmten Ziel
     * 
     * @param zipFile
     * @param targetPath
     */
    public UnZip(File zipFile, File targetPath) {
        b = new byte[8092];
        this.zipFile = zipFile;
        if (targetPath == null) {
            this.targetPath = zipFile.getParentFile();
        } else {
            this.targetPath = targetPath;
        }

    }

    public File[] extract() throws Exception {
        dirsMade = new TreeSet<String>();

        zipF = new ZipFile(zipFile);

        Enumeration<?> all = zipF.entries();
        LinkedList<File> ret = new LinkedList<File>();
        while (all.hasMoreElements()) {
            File file = getFile((ZipEntry) all.nextElement());
            if (file != null) {
                ret.add(file);
            }
        }
        zipF.close();
        if (autoDelete) {
            FileCreationManager.getInstance().delete(zipFile, null);
        }
        return ret.toArray(new File[ret.size()]);

    }

    protected File getFile(ZipEntry e) throws IOException {
        String zipName = e.getName();
        if (zipName.startsWith("/")) {
            if (!warnedMkDir) {
                System.out.println("Ignoring absolute paths");
            }
            warnedMkDir = true;
            zipName = zipName.substring(1);
        }
        if (zipName.endsWith("/")) { return null; }
        int ix = zipName.lastIndexOf('/');
        if (ix > 0) {
            String dirName = zipName.substring(0, ix);
            if (!dirsMade.contains(dirName)) {
                File d = new File(targetPath, dirName);
                if (!(d.exists() && d.isDirectory())) {
                    if (!FileCreationManager.getInstance().mkdir(d)) {
                        System.err.println("Warning: unable to mkdir " + dirName);
                    }
                    dirsMade.add(dirName);
                }
            }
        }
        // System.out.println(targetPath+"Creating " + zipName);
        File toExtract = new File(targetPath, zipName);
        if (!overwrite && toExtract.exists()) {
            System.out.println("Exists skip " + zipName);
            return null;
        } else {
            FileCreationManager.getInstance().delete(toExtract, null);
        }
        FileOutputStream os = new FileOutputStream(toExtract);
        InputStream is = zipF.getInputStream(e);
        int n = 0;
        while ((n = is.read(b)) > 0) {
            os.write(b, 0, n);
        }
        is.close();
        os.close();
        return toExtract;
    }

    public String[] listFiles() {
        try {
            zipF = new ZipFile(zipFile);
            Enumeration<?> all = zipF.entries();
            LinkedList<String> ret = new LinkedList<String>();
            while (all.hasMoreElements()) {
                ret.add(((ZipEntry) all.nextElement()).getName());
            }
            return ret.toArray(new String[ret.size()]);

        } catch (IOException err) {
            LogController.CL().log(err);
        }
        return null;
    }

}