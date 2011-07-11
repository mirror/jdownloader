//jDownloader - Downloadmanager
//Copyright (C) 2010 JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.jdownloader.extensions.folderwatch.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import jd.controlling.JDLogger;
import name.pachler.nio.file.ClosedWatchServiceException;
import name.pachler.nio.file.FileSystems;
import name.pachler.nio.file.Path;
import name.pachler.nio.file.Paths;
import name.pachler.nio.file.StandardWatchEventKind;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.WatchService;
import name.pachler.nio.file.ext.ExtendedWatchEventModifier;

public class FileMonitoring extends Thread {

    private WatchService                      watchService;

    private Logger                            logger          = JDLogger.getLogger();

    private ArrayList<FileMonitoringListener> listeners       = new ArrayList<FileMonitoringListener>();

    private static int                        instanceNr      = 0;

    private final String                      LOGGER_PREFIX;

    private MonitoringScheduler               importScheduler = null;

    private HashMap<WatchKey, String>         keypathMap      = new HashMap<WatchKey, String>();

    public static class FileEntry {
        public String filename;
        public long   ms;
    }

    public FileMonitoring() {
        LOGGER_PREFIX = "FileMonitor_" + instanceNr + ": ";

        this.watchService = FileSystems.getDefault().newWatchService();

        instanceNr++;
    }

    public FileMonitoring(String path, boolean isRecursive) {
        this();
        register(path, isRecursive);
    }

    public ArrayList<FileMonitoringListener> getListeners() {
        return listeners;
    }

    public void setListeners(ArrayList<FileMonitoringListener> listeners) {
        this.listeners = listeners;
    }

    public void addListener(FileMonitoringListener listener) {
        listeners.add(listener);
    }

    public void register(String path, boolean isRecursive) {
        Path watchedPath = Paths.get(path);
        WatchKey key = null;

        try {
            WatchEvent.Kind<?>[] events = { StandardWatchEventKind.ENTRY_CREATE, StandardWatchEventKind.ENTRY_MODIFY, StandardWatchEventKind.ENTRY_DELETE };

            if (isRecursive) {
                key = watchedPath.register(watchService, events, ExtendedWatchEventModifier.FILE_TREE);
            } else {
                key = watchedPath.register(watchService, events);
            }

            keypathMap.put(key, path);
        } catch (UnsupportedOperationException uox) {
            logger.warning(LOGGER_PREFIX + "File watching not supported");
            // handle this error here
        } catch (IOException iox) {
            logger.warning(LOGGER_PREFIX + iox.toString());
            // handle this error here
        }
    }

    public void done() {
        try {
            this.watchService.close();
        } catch (IOException e) {
        }

        instanceNr--;
    }

    public void run() {
        logger.info(LOGGER_PREFIX + "Watch service started");

        importScheduler = new MonitoringScheduler(this.getListeners(), LOGGER_PREFIX);
        importScheduler.startScheduling();

        while (true) {
            // take() will block until a file has been created/deleted
            WatchKey signalledKey = null;
            try {
                if (importScheduler.allExpired()) {
                    signalledKey = watchService.take();
                } else {
                    signalledKey = watchService.poll();
                }

            } catch (InterruptedException ix) {
                // we'll ignore being interrupted
                continue;
            } catch (ClosedWatchServiceException cwse) {
                // other thread closed watch service
                logger.info(LOGGER_PREFIX + "Watch service closed, terminating");

                importScheduler.stopScheduling();
                importScheduler = null;

                break;
            }

            // get list of events from key
            List<WatchEvent<?>> list = null;

            if (signalledKey != null) {
                list = signalledKey.pollEvents();
                // VERY IMPORTANT! call reset() AFTER pollEvents() to allow the
                // key to be reported again by the watch service
                signalledKey.reset();

                for (WatchEvent<?> e : list) {
                    Path context = (Path) e.context();
                    String filename = context.toString();

                    String path = keypathMap.get(signalledKey);
                    String absPath = path + "/" + filename;

                    if (e.kind() == StandardWatchEventKind.ENTRY_CREATE) {
                        logger.info(LOGGER_PREFIX + filename + " created");

                        importScheduler.schedule(absPath);
                    } else if (e.kind() == StandardWatchEventKind.ENTRY_DELETE) {
                        logger.info(LOGGER_PREFIX + filename + " deleted");

                        for (FileMonitoringListener listener : listeners) {
                            listener.onMonitoringFileDelete(absPath);
                        }
                    } else if (e.kind() == StandardWatchEventKind.ENTRY_MODIFY) {
                        logger.info(LOGGER_PREFIX + filename + " modified");

                        importScheduler.schedule(absPath);
                    } else if (e.kind() == StandardWatchEventKind.OVERFLOW) {
                        logger.info(LOGGER_PREFIX + "Overflow - More changes happened than we could retreive");
                    }
                }
            }
        }
    }
}
