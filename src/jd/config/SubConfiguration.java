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

public class SubConfiguration extends Property implements Serializable {

    private static final long                                  serialVersionUID = 7803718581558607222L;
    transient private static boolean                           SUBCONFIG_LOCK   = false;
    protected String                                           name;
    transient boolean                                          valid            = false;
    transient private static HashMap<String, SubConfiguration> SUB_CONFIGS      = new HashMap<String, SubConfiguration>();

    public SubConfiguration() {
    }

    @SuppressWarnings("unchecked")
    public SubConfiguration(final String name) {
        valid = true;
        this.name = name;
        final Object props = JDUtilities.getDatabaseConnector().getData(name);
        if (props != null && props instanceof HashMap) {
            HashMap<String, Object> tmp = (HashMap<String, Object>) props;
            /* we save memory by NOT using an empty hashmap */
            if (tmp.isEmpty()) tmp = null;
            this.setProperties(tmp);
        } else {
            if (props != null) {
                valid = false;
                JDLogger.getLogger().severe("Invalid Config Entry for " + name);
            }
        }
    }

    public void save() {
        if (valid) {
            HashMap<String, Object> props = this.getProperties();
            if (props == null) {
                /* saving null might cause issues */
                props = new HashMap<String, Object>();
            }
            JDUtilities.getDatabaseConnector().saveConfiguration(name, props);
            changes = false;
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public synchronized static boolean hasConfig(final String name) {
        if (SUBCONFIG_LOCK) {
            JDLogger.exception(new Exception("Static Database init error!!"));
        }
        SUBCONFIG_LOCK = true;
        try {
            if (SUB_CONFIGS.containsKey(name)) {
                return true;
            } else {
                return JDUtilities.getDatabaseConnector().hasData(name);
            }
        } finally {
            SubConfiguration.SUBCONFIG_LOCK = false;
        }
    }

    public synchronized static SubConfiguration getConfig(final String name) {
        if (SUBCONFIG_LOCK) {
            JDLogger.exception(new Exception("Static Database init error!!"));
        }
        SUBCONFIG_LOCK = true;
        try {
            if (SUB_CONFIGS.containsKey(name)) {
                return SUB_CONFIGS.get(name);
            } else {
                final SubConfiguration cfg = new SubConfiguration(name);
                SUB_CONFIGS.put(name, cfg);
                cfg.save();
                return cfg;
            }
        } finally {
            SubConfiguration.SUBCONFIG_LOCK = false;
        }
    }

    public synchronized static void removeConfig(final String name) {
        if (SUBCONFIG_LOCK) {
            JDLogger.exception(new Exception("Static Database init error!!"));
        }
        SUBCONFIG_LOCK = true;
        try {
            SUB_CONFIGS.remove(name);
            JDUtilities.getDatabaseConnector().removeData(name);
        } finally {
            SubConfiguration.SUBCONFIG_LOCK = false;
        }
    }
}