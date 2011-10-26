package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.ArrayList;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Files;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ArchiveExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.AudioExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ImageExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.VideoExtensions;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.linkgrabber.Header;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class QuickFilterTypeTable extends FilterTable<CrawledPackage, CrawledLink> implements LinkCollectorListener, GenericConfigEventListener<Boolean> {
    private static final long                                   serialVersionUID = 2109715691047942399L;
    private PackageControllerTable<CrawledPackage, CrawledLink> table2Filter;
    private DelayedRunnable                                     delayedRefresh;
    private final Object                                        LOCK             = new Object();
    private long                                                old              = -1;
    private Header                                              header;

    public QuickFilterTypeTable(Header filetypeFilter, PackageControllerTable<CrawledPackage, CrawledLink> table2Filter) {
        super();
        this.table2Filter = table2Filter;
        header = filetypeFilter;
        header.setFilterCount(0);
        final ArrayList<ExtensionFilter<CrawledPackage, CrawledLink>> knownExtensionFilters = new ArrayList<ExtensionFilter<CrawledPackage, CrawledLink>>();
        ExtensionFilter<CrawledPackage, CrawledLink> filter;
        filters.add(filter = new ExtensionFilter<CrawledPackage, CrawledLink>(AudioExtensions.AA) {

            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                QuickFilterTypeTable.this.table2Filter.getPackageControllerTableModel().recreateModel(false);
            }

        });
        knownExtensionFilters.add(filter);
        filters.add(filter = new ExtensionFilter<CrawledPackage, CrawledLink>(VideoExtensions.ASF) {
            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                QuickFilterTypeTable.this.table2Filter.getPackageControllerTableModel().recreateModel(false);
            }
        });
        knownExtensionFilters.add(filter);
        filters.add(filter = new ExtensionFilter<CrawledPackage, CrawledLink>(ImageExtensions.BMP) {
            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                QuickFilterTypeTable.this.table2Filter.getPackageControllerTableModel().recreateModel(false);
            }
        });
        knownExtensionFilters.add(filter);
        filters.add(filter = new ExtensionFilter<CrawledPackage, CrawledLink>(ArchiveExtensions.ACE) {
            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                QuickFilterTypeTable.this.table2Filter.getPackageControllerTableModel().recreateModel(false);
            }
        });
        knownExtensionFilters.add(filter);
        knownExtensionFilters.trimToSize();
        /*
         * now we add special extensionfilter which will handle all unknown
         * extensions
         */
        filters.add(new ExtensionFilter<CrawledPackage, CrawledLink>(_GUI._.settings_linkgrabber_filter_others(), NewTheme.I().getIcon("help", 16), false) {

            @Override
            public boolean isFiltered(String ext) {
                if (ext == null) return true;
                for (ExtensionFilter<CrawledPackage, CrawledLink> filter : knownExtensionFilters) {
                    if (filter.isFiltered(ext)) return false;
                }
                return true;
            }

            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                QuickFilterTypeTable.this.table2Filter.getPackageControllerTableModel().recreateModel(false);
            }
        });
        delayedRefresh = new DelayedRunnable(IOEQ.TIMINGQUEUE, 100l, 1000l) {

            @Override
            public void delayedrun() {
                updateQuickFilerTableData();
            }

        };
        GraphicalUserInterfaceSettings.LINKGRABBER_SIDEBAR_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                /* we call a different onConfigValueModified here */
                QuickFilterTypeTable.this.onConfigValueModified(null, LinkFilterSettings.LG_QUICKFILTER_TYPE_VISIBLE.getValue());
            }

        });

        LinkFilterSettings.LG_QUICKFILTER_TYPE_VISIBLE.getEventSender().addListener(this);
        LinkCollector.getInstance().addListener(this);
        onConfigValueModified(null, LinkFilterSettings.LG_QUICKFILTER_TYPE_VISIBLE.getValue());
    }

    private void updateQuickFilerTableData() {
        ArrayList<Filter<CrawledPackage, CrawledLink>> newTableData = null;
        synchronized (LOCK) {
            /* reset existing filter counters */
            for (Filter<CrawledPackage, CrawledLink> filter : filters) {
                filter.setCounter(0);
            }
            /* update filter list */
            boolean readL = LinkCollector.getInstance().readLock();
            try {
                for (CrawledPackage pkg : LinkCollector.getInstance().getPackages()) {
                    synchronized (pkg) {
                        for (CrawledLink link : pkg.getChildren()) {
                            /*
                             * speed optimization, we dont want to get extension
                             * several times
                             */
                            String ext = Files.getExtension(link.getName());
                            for (Filter<CrawledPackage, CrawledLink> filter : filters) {
                                if (((ExtensionFilter<CrawledPackage, CrawledLink>) filter).isFiltered(ext)) {
                                    filter.setCounter(filter.getCounter() + 1);
                                    break;
                                }
                            }
                        }
                    }
                }
            } finally {
                LinkCollector.getInstance().readUnlock(readL);
            }
            /* update FilterTableModel */
            newTableData = new ArrayList<Filter<CrawledPackage, CrawledLink>>(QuickFilterTypeTable.this.getExtTableModel().getTableData().size());
            for (Filter<CrawledPackage, CrawledLink> filter : filters) {
                if (filter.getCounter() > 0) {
                    /* only add entries with counter >0 to visible table */
                    newTableData.add(filter);
                }
            }
        }
        header.setFilterCount(newTableData.size());
        if (LinkFilterSettings.LG_QUICKFILTER_TYPE_VISIBLE.getValue()) QuickFilterTypeTable.this.getExtTableModel()._fireTableStructureChanged(newTableData, true);
    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (Boolean.TRUE.equals(newValue) && GraphicalUserInterfaceSettings.CFG.isLinkgrabberSidebarEnabled()) {
            enabled = true;
            /* filter is enabled, add listener and run again */
            table2Filter.getPackageControllerTableModel().addFilter(this);
            updateQuickFilerTableData();
            setVisible(true);
        } else {
            enabled = false;
            /* filter disabled, remove listener */
            old = -1;
            // table2Filter.getPackageControllerTableModel().removeFilter(this);
            // LinkCollector.getInstance().removeListener(this);
            setVisible(false);
            table2Filter.getPackageControllerTableModel().removeFilter(this);
        }
        table2Filter.getPackageControllerTableModel().recreateModel(false);
    }

    public void onLinkCollectorEvent(LinkCollectorEvent event) {
        switch (event.getType()) {
        case REMOVE_CONTENT:
        case REFRESH_STRUCTURE:
            if (old != LinkCollector.getInstance().getChildrenChanges()) {
                old = LinkCollector.getInstance().getChildrenChanges();
                delayedRefresh.run();
            }
            break;
        }
    }

    @Override
    public boolean isFiltered(CrawledPackage link) {
        /* we do not filter packages */
        return false;
    }

    @Override
    public boolean isFiltered(CrawledLink v) {
        /*
         * speed optimization, we dont want to get extension several times
         */
        if (enabled == false) return false;
        String ext = Files.getExtension(v.getName());
        ArrayList<Filter<CrawledPackage, CrawledLink>> lfilters = filters;
        for (Filter<CrawledPackage, CrawledLink> filter : lfilters) {
            if (!filter.isEnabled()) continue;
            if (((ExtensionFilter<CrawledPackage, CrawledLink>) filter).isFiltered(ext)) { return true; }
        }
        return false;
    }

}
