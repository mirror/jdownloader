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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jd.utils.JDUtilities;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;

@Deprecated
/**
 * @deprecated use protected static final Storage CONFIG  = JSonStorage.getPlainStorage("quickfilters"); instead
 * @author Thomas
 *
 */
public class SubConfiguration extends Property implements Serializable {
    private static final long                                   serialVersionUID = 7803718581558607222L;
    protected String                                            name;
    protected transient boolean                                 valid            = false;
    protected transient final File                              file;
    private final AtomicLong                                    setMark          = new AtomicLong(0);
    private final AtomicLong                                    writeMark        = new AtomicLong(0);
    protected static volatile HashMap<String, SubConfiguration> SUB_CONFIGS      = new HashMap<String, SubConfiguration>();
    protected static final HashMap<String, AtomicInteger>       LOCKS            = new HashMap<String, AtomicInteger>();
    protected static final byte[]                               KEY              = new byte[] { 0x01, 0x02, 0x11, 0x01, 0x01, 0x54, 0x01, 0x01, 0x01, 0x01, 0x12, 0x01, 0x01, 0x01, 0x22, 0x01 };
    protected static final DelayedRunnable                      SAVEDELAYER      = new DelayedRunnable(5000, 30000) {
        @Override
        public void delayedrun() {
            saveAll();
        }
    };
    static {
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            @Override
            public long getMaxDuration() {
                return 0;
            }

            @Override
            public String toString() {
                return "ShutdownEvent: SaveAllSubconfigurations";
            }

            @Override
            public void onShutdown(ShutdownRequest shutdownRequest) {
                saveAll();
            }
        });
    }

    private static void saveAll() {
        HashMap<String, SubConfiguration> localSubConfigs = SUB_CONFIGS;
        Iterator<Entry<String, SubConfiguration>> it = localSubConfigs.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().save();
        }
    }

    public SubConfiguration() {
        /* keep for serialization */
        file = null;
        valid = false;
    }

    public void reset() {
        this.setProperties(null);
    }

    private SubConfiguration(final String name, boolean importOnly) {
        this.name = name;
        file = Application.getResource("cfg/subconf_" + name + ".ejs");
        if (file.exists()) {
            writeMark.set(0);
            /* load existing file */
            try {
                final Map<String, Object> load = JSonStorage.restoreFrom(this.file, false, KEY, TypeRef.HASHMAP, new HashMap<String, Object>());
                if (load != null) {
                    load.remove("saveWorkaround");
                }
                setProperties(load);
            } catch (final Throwable e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            }
        } else {
            /* import old DataBase if existing */
            try {
                final Object props = JDUtilities.getDatabaseConnector().getData(name);
                if (props != null && props instanceof Map) {
                    final Map<String, Object> tmp = (Map<String, Object>) props;
                    /* remove obsolet variables from old stable (09581) */
                    tmp.remove("USE_PLUGIN");
                    tmp.remove("AGB_CHECKED");
                    if (tmp.size() > 0) {
                        writeMark.set(-1);
                        setProperties(tmp);
                    }
                }
            } catch (final NoOldJDDataBaseFoundException e) {
            } catch (final Throwable e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            }
        }
        valid = !importOnly;
    }

    public void save() {
        if (valid && file != null) {
            long lastSetMark = setMark.get();
            if (writeMark.getAndSet(lastSetMark) != lastSetMark) {
                try {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("Save Name:" + getName() + "|SetMark:" + lastSetMark + "|File:" + file);
                    final byte[] json = JSonStorage.getMapper().objectToByteArray(getProperties());
                    writeMark.set(setMark.get());
                    final Runnable run = new Runnable() {
                        @Override
                        public void run() {
                            JSonStorage.saveTo(file, false, KEY, json);
                        }
                    };
                    StorageHandler.enqueueWrite(run, file.getAbsolutePath(), true);
                } catch (final Throwable e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean setProperty(final String key, final Object value) {
        boolean change = super.setProperty(key, value);
        if (valid && change) {
            setMark.incrementAndGet();
            SAVEDELAYER.run();
        }
        return change;
    }

    public boolean setPropertyWithoutMark(final String key, final Object value) {
        return super.setProperty(key, value);
    }

    @Override
    public void setProperties(Map<String, Object> properties) {
        super.setProperties(properties);
        if (valid) {
            setMark.incrementAndGet();
            SAVEDELAYER.run();
        }
    }

    @Override
    public String toString() {
        return "SubConfig: " + name + "->" + super.toString();
    }

    private static synchronized Object requestLock(String name) {
        AtomicInteger lock = LOCKS.get(name);
        if (lock == null) {
            lock = new AtomicInteger(0);
            LOCKS.put(name, lock);
        }
        lock.incrementAndGet();
        return lock;
    }

    private static synchronized void unLock(String name) {
        AtomicInteger lock = LOCKS.get(name);
        if (lock != null) {
            if (lock.decrementAndGet() == 0) {
                LOCKS.remove(name);
            }
        }
    }

    public static SubConfiguration getConfig(final String name, boolean importOnly) {
        SubConfiguration ret = SUB_CONFIGS.get(name);
        if (ret != null) {
            return ret;
        } else {
            Object lock = requestLock(name);
            try {
                synchronized (lock) {
                    /* shared lock to allow parallel creation of new SubConfigurations */
                    ret = SUB_CONFIGS.get(name);
                    if (ret != null) {
                        return ret;
                    }
                    final SubConfiguration cfg = new SubConfiguration(name, importOnly);
                    synchronized (LOCKS) {
                        /* global lock to replace the SUB_CONFIGS */
                        HashMap<String, SubConfiguration> newSUB_CONFIGS = new HashMap<String, SubConfiguration>(SUB_CONFIGS);
                        newSUB_CONFIGS.put(name, cfg);
                        SUB_CONFIGS = newSUB_CONFIGS;
                    }
                    return cfg;
                }
            } finally {
                unLock(name);
            }
        }
    }

    public static SubConfiguration getConfig(final String name) {
        return getConfig(name, false);
    }
}