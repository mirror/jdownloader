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

import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.JsonKeyValueStorage;
import org.appwork.storage.Storage;
import org.jdownloader.logging.LogController;

public class SubConfiguration extends Property implements Serializable {

    private static final long                          serialVersionUID = 7803718581558607222L;
    protected String                                   name;
    protected transient boolean                        valid            = false;
    protected transient Storage                        json             = null;
    protected static HashMap<String, SubConfiguration> SUB_CONFIGS      = new HashMap<String, SubConfiguration>();

    public SubConfiguration() {
    }

    @SuppressWarnings("unchecked")
    private SubConfiguration(final String name) {
        valid = true;
        this.name = name;
        json = JSonStorage.getStorage("subconf_" + name);
        this.setProperties(((JsonKeyValueStorage) json).getInternalStorageMap());
        if (json.size() == 0) {
            try {
                final Object props = JDUtilities.getDatabaseConnector().getData(name);
                if (props != null && props instanceof HashMap) {
                    HashMap<String, Object> tmp = (HashMap<String, Object>) props;
                    /* remove obsolet variables from old stable (09581) */
                    tmp.remove("USE_PLUGIN");
                    tmp.remove("AGB_CHECKED");
                    /* Workaround to make sure the internal HashMap of JSonStorage is set to Property HashMap */
                    ((JsonKeyValueStorage) json).getInternalStorageMap().put("addWorkaround", true);
                    this.setProperties(((JsonKeyValueStorage) json).getInternalStorageMap());
                    ((JsonKeyValueStorage) json).getInternalStorageMap().remove("addWorkaround");
                    getProperties().putAll(tmp);
                } else {
                    if (props != null) {
                        valid = false;
                        LogController.CL().severe("Invalid Config Entry for " + name);
                    }
                }
                /* this avoids fresh conversions on next startup as we load the JSonStorage */
                json.put("saveWorkaround", System.currentTimeMillis());
            } catch (final NoOldJDDataBaseFoundException e) {
            }
        }
    }

    public void save() {
        if (valid) {
            json.save();
        }
    }

    @Override
    public void setProperty(final String key, final Object value) {
        super.setProperty(key, value);
        /* this changes changeFlag in JSonStorage to signal that it must be saved */
        json.put("saveWorkaround", System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return name;
    }

    public synchronized static SubConfiguration getConfig(final String name, boolean ImportOnly) {
        if (SUB_CONFIGS.containsKey(name)) {
            return SUB_CONFIGS.get(name);
        } else {
            final SubConfiguration cfg = new SubConfiguration(name);
            SUB_CONFIGS.put(name, cfg);
            if (ImportOnly) {
                /* importOnly dont get saved */
                /* used to convert old hsqldb to json */
                /* never save any data */
                JSonStorage.removeStorage(cfg.json);
                cfg.json.close();
            } else {
                cfg.save();
            }
            return cfg;
        }
    }

    public synchronized static SubConfiguration getConfig(final String name) {
        return getConfig(name, false);
    }

}