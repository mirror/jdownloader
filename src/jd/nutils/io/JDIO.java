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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import jd.nutils.JDHash;
import jd.nutils.OSDetector;

public class JDIO {

    /**
     * Das aktuelle Verzeichnis (Laden/Speichern)
     */
    private static File currentDirectory;

    /**
     * Schreibt content in eine Lokale textdatei
     * 
     * @param file
     * @param content
     * @return true/False je nach Erfolg des Schreibvorgangs
     */
    public static boolean writeLocalFile(File file, String content) {
        return writeLocalFile(file, content, false);
    }

    /**
     * Schreibt content in eine Lokale textdatei
     * 
     * @param file
     * @param content
     * @return true/False je nach Erfolg des Schreibvorgangs
     */
    public static boolean writeLocalFile(File file, String content, boolean append) {
        try {
            if (!append && file.isFile()) {
                if (!file.delete()) {
                    System.err.println("Konnte Datei nicht löschen " + file);
                    return false;
                }
            }
            if (file.getParent() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!append || !file.isFile()) file.createNewFile();
            BufferedWriter f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), "UTF8"));

            f.write(content);
            f.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String validateFileandPathName(String name) {
        if (name == null) { return null; }
        return name.replaceAll("([\\\\||<|>|\\|\"|:|\\*|\\?|/|\\x00])+", "_");
    }

    /**
     * Speichert ein byteArray in ein file.
     * 
     * @param file
     * @param bytearray
     * @return Erfolg true/false
     */
    public static boolean saveToFile(File file, byte[] b) {
        try {
            if (file.isFile()) {
                if (!file.delete()) {
                    System.err.println("Konnte Datei nicht überschreiben " + file);
                    return false;
                }
            }
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));
            output.write(b, 0, b.length);
            output.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Speichert ein Objekt
     * 
     * @param frame
     *            ein Fenster
     * @param objectToSave
     *            Das zu speichernde Objekt
     * @param fileOutput
     *            Das File, in das geschrieben werden soll. Falls das File ein
     *            Verzeichnis ist, wird darunter eine Datei erstellt Falls keins
     *            angegeben wird, soll der Benutzer eine Datei auswählen
     * @param name
     *            Dateiname
     * @param extension
     *            Dateiendung (mit Punkt)
     * @param asXML
     *            Soll das Objekt in eine XML Datei gespeichert werden?
     */
    public static void saveObject(JFrame frame, Object objectToSave, File fileOutput, String name, String extension, boolean asXML) {
        if (fileOutput == null) {
            JDFileFilter fileFilter = new JDFileFilter(extension, extension, true);
            JFileChooser fileChooserSave = new JFileChooser();
            fileChooserSave.setFileFilter(fileFilter);
            fileChooserSave.setSelectedFile(new File(((name != null) ? name : "*") + ((extension != null) ? extension : ".*")));
            if (currentDirectory != null) {
                fileChooserSave.setCurrentDirectory(currentDirectory);
            }
            if (fileChooserSave.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                fileOutput = fileChooserSave.getSelectedFile();
                currentDirectory = fileChooserSave.getCurrentDirectory();
            }
        }

        if (fileOutput != null) {
            if (fileOutput.isDirectory()) {
                fileOutput = new File(fileOutput, name + extension);

            }

            JDIO.waitOnObject(fileOutput);
            JDIO.saveReadObject.add(fileOutput);

            if (fileOutput.exists()) {
                fileOutput.delete();
            }
            try {
                FileOutputStream fos = new FileOutputStream(fileOutput);
                BufferedOutputStream buff = new BufferedOutputStream(fos);
                if (asXML) {
                    XMLEncoder xmlEncoder = new XMLEncoder(buff);
                    xmlEncoder.writeObject(objectToSave);
                    xmlEncoder.close();
                } else {
                    ObjectOutputStream oos = new ObjectOutputStream(buff);
                    oos.writeObject(objectToSave);
                    oos.close();
                }
                buff.close();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String hashPost = JDHash.getMD5(fileOutput);
            if (hashPost == null) {
                System.err.println("Schreibfehler: " + fileOutput + " Datei wurde nicht erstellt");
            }
            JDIO.saveReadObject.remove(fileOutput);

        } else {
            System.err.println("Schreibfehler: Fileoutput: null");
        }
    }

    public static Vector<File> saveReadObject = new Vector<File>();

    public static void waitOnObject(File file) {
        int c = 0;
        while (saveReadObject.contains(file)) {
            if (c++ > 1000) { return; }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {

                e.printStackTrace();
            }
        }
    }

    /**
     * Lädt ein Objekt aus einer Datei
     * 
     * @param frame
     *            Ein übergeordnetes Fenster
     * @param fileInput
     *            Falls das Objekt aus einer bekannten Datei geladen werden
     *            soll, wird hier die Datei angegeben. Falls nicht, kann der
     *            Benutzer über einen Dialog eine Datei aussuchen
     * @param asXML
     *            Soll das Objekt von einer XML Datei aus geladen werden?
     * @return Das geladene Objekt
     */
    public static Object loadObject(JFrame frame, File fileInput, boolean asXML) {
        Object objectLoaded = null;
        if (fileInput == null) {
            JFileChooser fileChooserLoad = new JFileChooser();
            if (currentDirectory != null) {
                fileChooserLoad.setCurrentDirectory(currentDirectory);
            }
            if (fileChooserLoad.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                fileInput = fileChooserLoad.getSelectedFile();
                currentDirectory = fileChooserLoad.getCurrentDirectory();
            }
        }
        if (fileInput != null) {

            waitOnObject(fileInput);
            saveReadObject.add(fileInput);

            try {
                FileInputStream fis = new FileInputStream(fileInput);
                BufferedInputStream buff = new BufferedInputStream(fis);
                if (asXML) {
                    XMLDecoder xmlDecoder = new XMLDecoder(new BufferedInputStream(buff));
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
                System.err.println(e.getMessage());
            }
            saveReadObject.remove(fileInput);
        }
        return null;
    }

    /**
     * public static String getLocalFile(File file) Liest file über einen
     * bufferdReader ein und gibt den Inhalt asl String zurück
     * 
     * @param file
     * @return File Content als String
     */
    public static String getLocalFile(File file) {
        if (!file.exists()) { return ""; }
        BufferedReader f;
        try {
            f = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

            String line;
            StringBuffer ret = new StringBuffer();
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

    /**
     * Gibt die Endung einer FIle zurück oder null
     * 
     * @param ret
     * @return
     */
    public static String getFileExtension(File ret) {
        if (ret == null) { return null; }
        String str = ret.getAbsolutePath();

        int i3 = str.lastIndexOf(".");

        if (i3 > 0) { return str.substring(i3 + 1); }
        return null;
    }

    /**
     * Zum Kopieren von einem Ort zum anderen
     * 
     * @param in
     * @param out
     * @throws IOException
     */
    public static boolean copyFile(File in, File out) {
        FileChannel inChannel = null;

        FileChannel outChannel = null;
        try {
            if (!out.exists()) {
                out.getParentFile().mkdirs();
                out.createNewFile();
            }
            inChannel = new FileInputStream(in).getChannel();

            outChannel = new FileOutputStream(out).getChannel();

            inChannel.transferTo(0, inChannel.size(), outChannel);

            return true;
        } catch (FileNotFoundException e1) {

            e1.printStackTrace();
            if (inChannel != null) {
                try {
                    inChannel.close();

                    if (outChannel != null) {
                        outChannel.close();
                    }
                } catch (IOException e) {

                    e.printStackTrace();
                    return false;
                }
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (inChannel != null) {
                inChannel.close();
            }

            if (outChannel != null) {
                outChannel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
