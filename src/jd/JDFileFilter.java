package jd;

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
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

import java.io.File;
import java.io.FileFilter;

/**
 * Mit dieser Klasse kann man sowohl bestimmte Dateien aus einem Verzeichnis
 * auflisten als auch einen FileFilter in einem JDFileChooser nutzen
 * 
 * @author astaldo
 */
public class JDFileFilter extends javax.swing.filechooser.FileFilter implements FileFilter {
    /**
     * Sollen Verzeichnisse akzeptiert werden?
     */
    private boolean acceptDirectories = true;

    /**
     * Extension der Datei (mit Punkt)
     */
    private String[] extension = null;
    private String extensions = null;

    /**
     * Name der Datei ohne Extension
     */
    private String name = null;

    /**
     * Erstellt einen neuen JDFileFilter
     * 
     * @param name
     *            Name der Datei ohne Extension
     * @param extension
     *            Extension der Datei (mit Punkt)
     * @param acceptDirectories
     *            Sollen Verzeichnisse akzeptiert werden?
     */
    public JDFileFilter(String name, String extension, boolean acceptDirectories) {
        this.name = name;
        if (extension != null) {

            this.extension = extension.split("\\|");

            extensions = extension;
        }
        this.acceptDirectories = acceptDirectories;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
     */
    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) { return acceptDirectories; }
        if (extension != null) {
            for (String element : extension) {
                if (f.getName().toLowerCase().endsWith(element.toLowerCase())) { return true; }
            }
            return false;
        }
        if (name != null) {
            if (!f.getName().startsWith(name)) { return false; }
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.filechooser.FileFilter#getDescription()
     */
    /**
     * Gibt die Filefilter beschreibung zurück
     */

    @Override
    public String getDescription() {
        return "Containerfiles";
    }

    /**
     * Liefert ein FileObjekt zurück, daß aus dem Name und der Extension
     * zusammengesetzt wird
     * 
     * @return Ein FileObjekt
     */
    public File getFile() {
        StringBuffer filename = new StringBuffer();
        if (name != null) {
            filename.append(name);
        } else {
            filename.append("*");
        }
        if (extension != null) {
            filename.append(extensions);
        } else {
            filename.append(".*");
        }
        return new File(filename.toString());
    }
}
