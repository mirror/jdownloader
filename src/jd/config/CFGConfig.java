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

package jd.config;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

public class CFGConfig extends SubConfiguration implements Serializable {

    private static final HashMap<String, CFGConfig> CONFIGS = new HashMap<String, CFGConfig>();

    /**
     * 
     */
    private static final long serialVersionUID = 9187069483565313810L;

    /**
     * Diese Klasse stellt Die Configuration als eine Configfile zur verfügung.
     * Diese infos werden mit Absicht! nicht in der Datenbank gespeichert!
     * 
     * @param name
     */
    @SuppressWarnings("unchecked")
    private CFGConfig(final String name) {
        this.name = name;
        final File file = JDUtilities.getResourceFile("config/" + name + ".cfg");
        final Object props = JDIO.loadObject(file, false);
        file.getParentFile().mkdirs();
        if (props != null) {
            setProperties((HashMap<String, Object>) props);
        }
    }

    public void save() {
        JDIO.saveObject(getProperties(), JDUtilities.getResourceFile("config/" + name + ".cfg"), false);
    }

    public static CFGConfig getConfig(final String string) {
        if (CONFIGS.containsKey(string)) {
            return CONFIGS.get(string);
        } else {
            final CFGConfig ret = new CFGConfig(string);
            CONFIGS.put(string, ret);
            return ret;
        }
    }

}