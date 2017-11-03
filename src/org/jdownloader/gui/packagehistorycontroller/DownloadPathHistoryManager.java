package org.jdownloader.gui.packagehistorycontroller;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.ConfigEvent;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Lists;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.EventSuppressor;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;

public class DownloadPathHistoryManager extends HistoryManager<DownloadPath> implements GenericConfigEventListener<Object> {
    private static final DownloadPathHistoryManager INSTANCE = new DownloadPathHistoryManager();

    /**
     * get the only existing instance of PackageHistoryManager. This is a singleton
     *
     * @return
     */
    public static DownloadPathHistoryManager getInstance() {
        return DownloadPathHistoryManager.INSTANCE;
    }

    /**
     * Create a new instance of PackageHistoryManager. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private DownloadPathHistoryManager() {
        super(CFG_LINKGRABBER.CFG.getDownloadDestinationHistory(), CFG_GENERAL.CFG.getDownloadDestinationHistoryLength());
        CFG_LINKGRABBER.DOWNLOAD_DESTINATION_HISTORY.getEventSender().addListener(this);
    }

    @Override
    public void add(String packageName) {
        if (isValid(packageName)) {
            CFG_LINKGRABBER.CFG.setLatestDownloadDestinationFolder(packageName);
            super.add(packageName);
        }
    }

    @Override
    protected boolean isValid(String input) {
        return !StringUtils.isEmpty(input) && new File(input).isAbsolute();
    }

    public synchronized boolean setLastIfExists(String packageName) {
        final DownloadPath existing = get(packageName);
        if (existing != null) {
            existing.setTime(System.currentTimeMillis());
            Collections.sort(packageHistory);
            return true;
        }
        return false;
    }

    private void superadd(String packageName) {
        super.add(packageName);
    }

    @Override
    protected void save(List<DownloadPath> list) {
        final Thread thread = Thread.currentThread();
        final EventSuppressor<ConfigEvent> eventSuppressor = new EventSuppressor<ConfigEvent>() {
            @Override
            public boolean suppressEvent(ConfigEvent eventType) {
                return Thread.currentThread() == thread;
            }
        };
        CFG_LINKGRABBER.DOWNLOAD_DESTINATION_HISTORY.getEventSender().addEventSuppressor(eventSuppressor);
        try {
            CFG_LINKGRABBER.CFG.setDownloadDestinationHistory(list);
        } finally {
            CFG_LINKGRABBER.DOWNLOAD_DESTINATION_HISTORY.getEventSender().removeEventSuppressor(eventSuppressor);
        }
    }

    @Override
    protected DownloadPath createNew(String name) {
        return new DownloadPath(name);
    }

    public List<String> listPaths(String... strings) {
        return listPathes(false, strings);
    }

    public List<String> listPathes(boolean atTop, String... strings) {
        final List<DownloadPath> l = list();
        final LinkedHashSet<String> dupe = new LinkedHashSet<String>();
        if (atTop && strings != null) {
            for (String s : strings) {
                if (StringUtils.isNotEmpty(s)) {
                    dupe.add(s);
                }
            }
        }
        for (DownloadPath p : l) {
            if (p != null && StringUtils.isNotEmpty(p.getName())) {
                dupe.add(p.getName());
            }
        }
        if (!atTop && strings != null) {
            for (String s : strings) {
                if (StringUtils.isNotEmpty(s)) {
                    dupe.add(s);
                }
            }
        }
        return Lists.unique(dupe);
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
        if (newValue == null) {
            clear();
        } else if (newValue instanceof List) {
            final List<Object> list = (List<Object>) newValue;
            if (list.size() == 0) {
                clear();
            } else {
                synchronized (this) {
                    clear();
                    for (int i = list.size() - 1; i >= 0; i--) {
                        final Object item = list.get(i);
                        if (item != null && item instanceof DownloadPath) {
                            superadd(((DownloadPath) item).getName());
                        }
                    }
                }
            }
        }
    }
}
