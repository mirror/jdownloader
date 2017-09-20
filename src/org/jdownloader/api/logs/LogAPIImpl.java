package org.jdownloader.api.logs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.exceptions.WTFException;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogSink.FLUSH;
import org.appwork.utils.logging2.sendlogs.AbstractLogAction;
import org.appwork.utils.logging2.sendlogs.LogFolder;
import org.appwork.utils.zip.ZipIOException;
import org.appwork.utils.zip.ZipIOWriter;
import org.jdownloader.jdserv.JDServUtils;
import org.jdownloader.logging.LogController;

public class LogAPIImpl implements LogAPI {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yy HH.mm.ss", Locale.GERMANY);

    @Override
    public List<LogFolderStorable> getAvailableLogs() {
        final ArrayList<LogFolder> folders = AbstractLogAction.getLogFolders();
        final ArrayList<LogFolderStorable> result = new ArrayList<LogFolderStorable>();
        LogFolder current = null;
        for (final LogFolder folder : folders) {
            if (isCurrentLogFolder(folder.getCreated())) {
                current = folder;
            } else {
                result.add(LogFolderStorable.create(folder));
            }
        }
        if (current != null) {
            final LogFolderStorable storable = LogFolderStorable.create(current);
            storable.setCurrent(true);
            result.add(0, storable);
        }
        return result;
    }

    private boolean isCurrentLogFolder(long timestamp) {
        final long startup = LogController.getInstance().getInitTime();
        return startup == timestamp;
    }

    @Override
    public String sendLogFile(final LogFolderStorable[] selectedFolders) throws BadParameterException {
        if (selectedFolders == null || selectedFolders.length == 0) {
            throw new BadParameterException("selection empty or null");
        }
        final ArrayList<LogFolder> logFolders = AbstractLogAction.getLogFolders();
        final ArrayList<LogFolder> selectedLogFolders = new ArrayList<LogFolder>();
        for (final LogFolderStorable storable : selectedFolders) {
            for (final LogFolder logFolder : logFolders) {
                if (logFolder.getCreated() == storable.getCreated() && logFolder.getLastModified() == storable.getLastModified()) {
                    selectedLogFolders.add(logFolder);
                }
            }
        }
        if (!selectedLogFolders.isEmpty()) {
            final AtomicReference<String> logIDRef = new AtomicReference<String>(null);
            final Thread uploadThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String logID = null;
                    File zip = null;
                    try {
                        for (final LogFolder logFolder : selectedLogFolders) {
                            if (zip != null) {
                                zip.delete();
                            }
                            zip = createPackage(logFolder);
                            logID = JDServUtils.uploadLog(zip, logID);
                            if (logID != null && logIDRef.get() == null) {
                                synchronized (logIDRef) {
                                    logIDRef.set(logID);
                                    logIDRef.notify();
                                }
                            }
                        }
                    } catch (Throwable th) {
                        th.printStackTrace();
                    } finally {
                        synchronized (logIDRef) {
                            logIDRef.notifyAll();
                        }
                        if (zip != null) {
                            zip.delete();
                        }
                    }
                }
            }, "LogAPIImpl:sendLogFile");
            uploadThread.setDaemon(true);
            uploadThread.start();
            synchronized (logIDRef) {
                if (uploadThread.isAlive() && logIDRef.get() == null) {
                    try {
                        logIDRef.wait();
                    } catch (InterruptedException e) {
                    }
                }
                return logIDRef.get();
            }
        }
        return null;
    }

    private File createPackage(final LogFolder lf) throws Exception {
        final File zip = Application.getTempResource("logs/logPackage" + System.currentTimeMillis() + ".zip");
        zip.delete();
        zip.getParentFile().mkdirs();
        ZipIOWriter writer = null;
        try {
            writer = new ZipIOWriter(zip) {
                @Override
                public void addFile(final File addFile, final boolean compress, final String fullPath) throws FileNotFoundException, ZipIOException, IOException {
                    if (addFile.getName().endsWith(".lck") || addFile.isFile() && addFile.length() == 0) {
                        return;
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        throw new WTFException("INterrupted");
                    }
                    super.addFile(addFile, compress, fullPath);
                }
            };
            final String name = lf.getFolder().getName() + "-" + DATE_FORMAT.format(lf.getCreated()) + " to " + DATE_FORMAT.format(lf.getLastModified());
            final File folder = Application.getTempResource("logs/" + name);
            if (lf.isNeedsFlush()) {
                LogController.getInstance().flushSinks(FLUSH.FORCE);
            }
            if (folder.exists()) {
                Files.deleteRecursiv(folder);
            }
            IO.copyFolderRecursive(lf.getFolder(), folder, true);
            writer.addDirectory(folder, true, null);
        } finally {
            try {
                writer.close();
            } catch (final Throwable e) {
            }
        }
        return zip;
    }
}
