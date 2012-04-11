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

import java.io.Serializable;
import java.util.HashMap;

import jd.controlling.JDLogger;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.JsonKeyValueStorage;
import org.appwork.storage.Storage;

public class SubConfiguration extends Property implements Serializable {

    private static final long                                  serialVersionUID = 7803718581558607222L;
    protected String                                           name;
    transient boolean                                          valid            = false;
    private Storage                                            json             = null;
    transient private static HashMap<String, SubConfiguration> SUB_CONFIGS      = new HashMap<String, SubConfiguration>();

    public SubConfiguration() {
    }

    @SuppressWarnings("unchecked")
    private SubConfiguration(final String name) {
        valid = true;
        this.name = name;
        json = JSonStorage.getStorage("subconf_" + name);
        this.setProperties(((JsonKeyValueStorage) json).getInternalStorageMap());
        if (json.size() == 0) {
            final Object props = JDUtilities.getDatabaseConnector().getData(name);
            if (props != null && props instanceof HashMap) {
                HashMap<String, Object> tmp = (HashMap<String, Object>) props;
                /* we save memory by NOT using an empty hashmap */
                if (tmp.isEmpty()) tmp = new HashMap<String, Object>();
                ((JsonKeyValueStorage) json).getInternalStorageMap().putAll(tmp);
                this.setProperties(((JsonKeyValueStorage) json).getInternalStorageMap());
            } else {
                if (props != null) {
                    valid = false;
                    JDLogger.getLogger().severe("Invalid Config Entry for " + name);
                }
            }
        }
        json.put("saveWorkaround", System.currentTimeMillis());
    }

    public void save() {
        if (valid) {
            json.save();
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public synchronized static boolean hasConfig(final String name) {
        if (SUB_CONFIGS.containsKey(name)) {
            return true;
        } else {
            return JDUtilities.getDatabaseConnector().hasData(name);
        }
    }

    public synchronized static SubConfiguration getConfig(final String name) {
        if (SUB_CONFIGS.containsKey(name)) {
            return SUB_CONFIGS.get(name);
        } else {
            final SubConfiguration cfg = new SubConfiguration(name);
            SUB_CONFIGS.put(name, cfg);
            cfg.save();
            return cfg;
        }
    }

    public synchronized static void removeConfig(final String name) {
        SUB_CONFIGS.remove(name);
        JDUtilities.getDatabaseConnector().removeData(name);
    }
}