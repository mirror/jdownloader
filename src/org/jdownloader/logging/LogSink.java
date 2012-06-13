package org.jdownloader.logging;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogSink extends Logger {

    protected ArrayList<SoftReference<LogSource>> logSources     = new ArrayList<SoftReference<LogSource>>();
    protected FileHandler                         fileHandler    = null;
    protected ConsoleHandler                      consoleHandler = null;

    protected LogSink(String name, String resourceBundleName) {
        super(name, resourceBundleName);
    }

    protected LogSink(String name) {
        this(name, (String) null);
        this.setLevel(Level.ALL);
    }

    protected void addLogSource(LogSource source) {
        synchronized (logSources) {
            logSources.add(new SoftReference<LogSource>(source));
            source.setParent(this);
            if (consoleHandler != null) {
                source.removeHandler(consoleHandler);
                source.addHandler(consoleHandler);
            }
        }
    }

    protected void flushSources() {
        ArrayList<LogSource> sources = new ArrayList<LogSource>();
        synchronized (logSources) {
            Iterator<SoftReference<LogSource>> it = logSources.iterator();
            while (it.hasNext()) {
                SoftReference<LogSource> next = it.next();
                LogSource item = next.get();
                if (item == null || item.isClosed()) {
                    it.remove();
                    continue;
                } else {
                    sources.add(item);
                }
            }
        }
        for (LogSource source : sources) {
            if (source.isAllowTimeoutFlush()) source.flush();
        }
    }

    protected boolean hasLogSources() {
        synchronized (logSources) {
            Iterator<SoftReference<LogSource>> it = logSources.iterator();
            while (it.hasNext()) {
                SoftReference<LogSource> next = it.next();
                LogSource item = next.get();
                if (item == null || item.isClosed()) {
                    it.remove();
                    continue;
                }
            }
            return logSources.size() > 0;
        }
    }

    protected synchronized void close() {
        flushSources();
        if (fileHandler != null) super.removeHandler(fileHandler);
        try {
            fileHandler.close();
        } catch (final Throwable e) {
        } finally {
            fileHandler = null;
        }
    }

    @Override
    public void removeHandler(Handler handler) throws SecurityException {
        super.removeHandler(handler);
        if (fileHandler != null && fileHandler == handler) {
            close();
        } else if (consoleHandler != null && handler == consoleHandler) {
            consoleHandler = null;
            synchronized (logSources) {
                Iterator<SoftReference<LogSource>> it = logSources.iterator();
                while (it.hasNext()) {
                    SoftReference<LogSource> next = it.next();
                    LogSource item = next.get();
                    if (item == null || item.isClosed()) {
                        it.remove();
                        continue;
                    } else {
                        item.removeHandler(handler);
                    }
                }
            }
        }
    }

    @Override
    public void addHandler(Handler handler) throws SecurityException {
        super.addHandler(handler);
        if (fileHandler == null && handler instanceof FileHandler) {
            fileHandler = (FileHandler) handler;
        } else if (consoleHandler == null && handler instanceof ConsoleHandler) {
            consoleHandler = (ConsoleHandler) handler;
            synchronized (logSources) {
                Iterator<SoftReference<LogSource>> it = logSources.iterator();
                while (it.hasNext()) {
                    SoftReference<LogSource> next = it.next();
                    LogSource item = next.get();
                    if (item == null || item.isClosed()) {
                        it.remove();
                        continue;
                    } else {
                        item.removeHandler(consoleHandler);
                        item.addHandler(consoleHandler);
                    }
                }
            }
        }
    }

}
