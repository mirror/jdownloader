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

package jd.nutils.io;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;

import jd.controlling.JDLogger;

import org.appwork.utils.IO;
import org.appwork.utils.Regex;

public final class JDIO {
    /**
     * Don't let anyone instantiate this class.
     */
    private JDIO() {
    }

    /**
     * Schreibt content in eine Lokale textdatei
     * 
     * @param file
     * @param content
     * @return true/False je nach Erfolg des Schreibvorgangs
     */
    public static boolean writeLocalFile(final File file, final String content) {
        return writeLocalFile(file, content, false);
    }

    /**
     * Schreibt content in eine Lokale textdatei
     * 
     * @param file
     * @param content
     * @return true/False je nach Erfolg des Schreibvorgangs
     */
    public static boolean writeLocalFile(final File file, final String content, final boolean append) {
        OutputStreamWriter ow = null;
        FileOutputStream fo = null;
        try {
            if (!append && file.isFile() && !file.delete()) {
                System.err.println("Konnte Datei nicht löschen " + file);
                return false;
            }
            if (file.getParent() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!append || !file.isFile()) {
                file.createNewFile();
            }

            final BufferedWriter f = new BufferedWriter(ow = new OutputStreamWriter(fo = new FileOutputStream(file, append), "UTF8"));

            f.write(content);
            f.close();
            return true;
        } catch (Exception e) {
            JDLogger.exception(e);
            return false;
        } finally {
            try {
                ow.close();
            } catch (final Throwable e) {
            }
            try {
                fo.close();
            } catch (final Throwable e) {
            }
        }
    }

    public static String validateFileandPathName(final String name) {
        if (name == null) return null;
        return name.replaceAll("([\\\\|<|>|\\||\"|:|\\*|\\?|/|\\x00])+", "_");
    }

    /**
     * Speichert ein byteArray in ein file.
     * 
     * @param file
     * @param bytearray
     * @return Erfolg true/false
     */
    public static boolean saveToFile(final File file, final byte[] b) {
        try {
            IO.writeToFile(file, b);
            return true;
        } catch (Exception e) {
            JDLogger.exception(e);
            return false;
        }
    }

    /**
     * Speichert ein Objekt
     * 
     * @param objectToSave
     *            Das zu speichernde Objekt
     * @param fileOutput
     *            Das File, in das geschrieben werden soll. Falls das File ein
     *            Verzeichnis ist, wird darunter eine Datei erstellt
     * @param name
     *            Dateiname
     * @param extension
     *            Dateiendung (mit Punkt)
     * @param asXML
     *            Soll das Objekt in eine XML Datei gespeichert werden?
     */
    public static void saveObject(final Object objectToSave, File fileOutput, final boolean asXML) {
        if (fileOutput == null || fileOutput.isDirectory()) {
            System.err.println("Schreibfehler: Wrong parameter (" + fileOutput + ")");
            return;
        }

        fileOutput.getParentFile().mkdirs();

        JDIO.waitOnObject(fileOutput);
        JDIO.saveReadObject.add(fileOutput);

        if (fileOutput.exists()) {
            fileOutput.delete();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileOutput);
            final BufferedOutputStream buff = new BufferedOutputStream(fos);
            if (asXML) {
                final XMLEncoder xmlEncoder = new XMLEncoder(buff);
                xmlEncoder.writeObject(objectToSave);
                xmlEncoder.close();
            } else {
                final ObjectOutputStream oos = new ObjectOutputStream(buff);
                oos.writeObject(objectToSave);
                oos.close();
            }
            buff.close();
            fos.close();
        } catch (Exception e) {
            JDLogger.exception(e);
        } finally {
            try {
                fos.close();
            } catch (final Throwable e) {
            }
        }
        JDIO.saveReadObject.remove(fileOutput);
    }

    public static Vector<File> saveReadObject = new Vector<File>();

    public static void waitOnObject(final File file) {
        int c = 0;
        while (saveReadObject.contains(file)) {
            if (c++ > 1000) return;
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                JDLogger.exception(e);
            }
        }
    }

    /**
     * Lädt ein Objekt aus einer Datei
     * 
     * @param fileInput
     *            Falls das Objekt aus einer bekannten Datei geladen werden
     *            soll, wird hier die Datei angegeben.
     * @param asXML
     *            Soll das Objekt von einer XML Datei aus geladen werden?
     * @return Das geladene Objekt
     */
    public static Object loadObject(File fileInput, final boolean asXML) {
        if (fileInput == null || fileInput.isDirectory()) {
            System.err.println("Schreibfehler: Wrong parameter (" + fileInput + ")");
            return null;
        }

        Object objectLoaded = null;

        waitOnObject(fileInput);
        saveReadObject.add(fileInput);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileInput);
            final BufferedInputStream buff = new BufferedInputStream(fis);
            if (asXML) {
                final XMLDecoder xmlDecoder = new XMLDecoder(new BufferedInputStream(buff));
                objectLoaded = xmlDecoder.readObject();
                xmlDecoder.close();
            } else {
                ObjectInputStream ois = new ObjectInputStream(buff);
                objectLoaded = ois.readObject();
                ois.close();
            }
            fis.close();
            buff.close();

            saveReadObject.remove(fileInput);
            return objectLoaded;
        } catch (Exception e) {
            JDLogger.exception(e);
        } finally {
            try {
                fis.close();
            } catch (final Throwable e) {
            }
        }
        saveReadObject.remove(fileInput);
        return null;
    }

    /**
     * public static String getLocalFile(File file) Liest file über einen
     * bufferdReader ein und gibt den Inhalt asl String zurück
     * 
     * @param file
     * @return File Content als String
     */
    public static String readFileToString(final File file) {
        if (file == null) return null;
        if (!file.exists()) return "";
        try {
            return IO.readFileToString(file);
        } catch (IOException e) {
            JDLogger.exception(e);
            return "";
        }

    }

    /**
     * Gibt die Endung einer FIle zurück oder null
     * 
     * @param ret
     * @return
     */
    public static String getFileExtension(final File ret) {
        if (ret == null) return null;
        return getFileExtension(ret.getAbsolutePath());
    }

    public static String getFileExtension(final String str) {
        if (str == null) return null;

        final int i3 = str.lastIndexOf(".");

        if (i3 > 0) return str.substring(i3 + 1);
        return null;
    }

    public static String getFileType(String ext) {
        File file = null;
        try {
            file = File.createTempFile("icon", "." + ext);

            return new JFileChooser().getTypeDescription(file);
        } catch (IOException e) {
            return "." + ext;
        } finally {
            if (file != null) file.delete();
        }
    }

    /**
     * copy one file to another, using channels
     * 
     * @param in
     * @param out
     * @returns boolean whether its succeessfull or not
     */
    public static boolean copyFile(final File in, final File out) {
        if (!in.exists()) return false;
        try {
            IO.copyFile(in, out);
            return true;
        } catch (Exception e) {
            return false;

        }

    }

    public static boolean removeDirectoryOrFile(final File dir) {
        if (dir.isDirectory()) {
            final String[] children = dir.list();
            for (final String element : children) {
                boolean success = removeDirectoryOrFile(new File(dir, element));
                if (!success) return false;
            }
        }
        return dir.delete();
    }

    /**
     * Runs recursive through the dir (directory) and list all files. returns
     * null if dir is a file.
     * 
     * @param dir
     * @return
     */
    public static ArrayList<File> listFiles(final File dir) {
        if (!dir.isDirectory()) return null;
        final ArrayList<File> ret = new ArrayList<File>();

        for (final File f : dir.listFiles()) {
            if (f.isDirectory()) {
                ret.addAll(listFiles(f));
            } else {
                ret.add(f);
            }
        }
        return ret;
    }

    /**
     * removes recursive all files and directories in parentFile if the match
     * pattern
     * 
     * @param parentFile
     * @param string
     */
    public static void removeByPattern(final File parentFile, final Pattern pattern) {
        removeRekursive(parentFile, new FileSelector() {

            @Override
            public boolean doIt(final File file) {
                return Regex.matches(file.getAbsolutePath(), pattern);
            }

        });
    }

    public static abstract class FileSelector {
        public abstract boolean doIt(File file);
    }

    /**
     * Removes all files rekursivly in file, for which fileSelector.doIt returns
     * true
     * 
     * @param file
     * @param fileSelector
     */
    public static void removeRekursive(final File file, final FileSelector fileSelector) {
        if (file == null || !file.exists()) return;
        for (final File f : file.listFiles()) {
            if (f.isDirectory()) {
                removeRekursive(f, fileSelector);
            }
            if (fileSelector.doIt(f)) {
                f.delete();
            }
        }
    }
}
