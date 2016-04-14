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
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.nutils.io;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

/**
 * Mit dieser Klasse kann man, sowohl bestimmte Dateien aus einem Verzeichnis auflisten, als auch einen FileFilter in einem JDFileChooser
 * nutzen
 *
 * @author astaldo
 */
public class JDFileFilter extends javax.swing.filechooser.FileFilter implements FileFilter {
    /**
     * Sollen Verzeichnisse akzeptiert werden?
     */
    private final boolean  acceptDirectories;

    /**
     * Beschreibung vom FileFilter
     */
    private final String   description;

    /**
     * Zu akzeptierende Dateiendung (mit Punkt)
     */
    private final String[] extension;

    private final Pattern  pattern;

    /**
     * Erstellt einen neuen JDFileFilter
     *
     * @param description
     *            Beschreibung vom FileFilter oder null, wenn der Defaultname (Containerfiles) genommen werden soll
     * @param extension
     *            Zu akzeptierende Dateiendungen (mit Punkt und mit | getrennt)
     * @param acceptDirectories
     *            Sollen Verzeichnisse akzeptiert werden?
     */
    public JDFileFilter(String description, String extension, boolean acceptDirectories) {
        if (description != null) {
            this.description = description;
        } else {
            this.description = "Container files";
        }
        this.extension = extension.split("\\|");
        this.pattern = null;
        this.acceptDirectories = acceptDirectories;
    }

    public JDFileFilter(String description, Pattern pattern, boolean acceptDirectories) {
        if (description != null) {
            this.description = description;
        } else {
            this.description = "Container files";
        }
        this.pattern = pattern;
        extension = null;
        this.acceptDirectories = acceptDirectories;
    }

    // @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return acceptDirectories;
        } else {
            if (pattern != null) {
                return pattern.matcher(f.getName()).matches();
            } else {
                final String check = f.getName().toLowerCase();
                for (String element : extension) {
                    if (check.endsWith(element)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /**
     * Gibt die Filefilter Beschreibung zurück
     */
    // @Override
    public String getDescription() {
        return this.description;
    }

}
