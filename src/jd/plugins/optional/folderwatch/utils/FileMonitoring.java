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

package jd.plugins.optional.folderwatch.utils;

import java.io.IOException;
import java.util.ArrayList;
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

public class FileMonitoring extends Thread {

    private WatchService                      watchService;

    private Logger                            logger          = JDLogger.getLogger();

    private ArrayList<FileMonitoringListener> listeners       = new ArrayList<FileMonitoringListener>();

    private static int                        instanceNr      = 0;

    private final String                      LOGGER_PREFIX;

    private MonitoringScheduler               importScheduler = null;

    public class FileEntry {
        public String filename;
        public long   ms;
    }

    public FileMonitoring() {
        LOGGER_PREFIX = "FileMonitor_" + instanceNr + ": ";

        this.watchService = FileSystems.getDefault().newWatchService();

        instanceNr++;
    }

    public FileMonitoring(String path) {
        this();
        register(path);
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

    public void register(String path) {
        Path watchedPath = Paths.get(path);
        @SuppressWarnings("unused")
        WatchKey key = null;

        try {
            key = watchedPath.register(watchService, StandardWatchEventKind.ENTRY_CREATE, StandardWatchEventKind.ENTRY_MODIFY, StandardWatchEventKind.ENTRY_DELETE);
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
                    if (e.kind() == StandardWatchEventKind.ENTRY_CREATE) {
                        Path context = (Path) e.context();
                        String filename = context.toString();

                        logger.info(LOGGER_PREFIX + filename + " created");

                        importScheduler.schedule(filename);
                    } else if (e.kind() == StandardWatchEventKind.ENTRY_DELETE) {
                        Path context = (Path) e.context();
                        String filename = context.toString();

                        logger.info(LOGGER_PREFIX + filename + " deleted");

                        for (FileMonitoringListener listener : listeners) {
                            listener.onMonitoringFileDelete(filename);
                        }
                    } else if (e.kind() == StandardWatchEventKind.ENTRY_MODIFY) {
                        Path context = (Path) e.context();
                        String filename = context.toString();

                        logger.info(LOGGER_PREFIX + filename + " modified");

                        importScheduler.schedule(filename);
                    } else if (e.kind() == StandardWatchEventKind.OVERFLOW) {
                        logger.info(LOGGER_PREFIX + "Overflow - More changes happened than we could retreive");
                    }
                }
            }
        }
    }
}
