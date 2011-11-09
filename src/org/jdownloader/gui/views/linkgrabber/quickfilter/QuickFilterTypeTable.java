package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Files;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ArchiveExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.AudioExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ImageExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.VideoExtensions;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;
import org.jdownloader.gui.views.linkgrabber.Header;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class QuickFilterTypeTable extends FilterTable implements GenericConfigEventListener<Boolean> {
    private static final long                                   serialVersionUID = 2109715691047942399L;

    private PackageControllerTable<CrawledPackage, CrawledLink> table2Filter;
    private DelayedRunnable                                     delayedRefresh;
    private final Object                                        LOCK             = new Object();
    private long                                                old              = -1;
    private Header                                              header;
    private TableModelListener                                  listener;

    public QuickFilterTypeTable(Header filetypeFilter, PackageControllerTable<CrawledPackage, CrawledLink> table2Filter) {
        super();
        this.table2Filter = table2Filter;
        header = filetypeFilter;
        header.setFilterCount(0);
        final ArrayList<ExtensionFilter> knownExtensionFilters = new ArrayList<ExtensionFilter>();
        ExtensionFilter filter;

        filters.add(filter = new ExtensionFilter(AudioExtensions.AA) {
            protected String getID() {
                return "Type_Audio";
            }

            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                QuickFilterTypeTable.this.table2Filter.getPackageControllerTableModel().recreateModel(false);
            }

        });
        knownExtensionFilters.add(filter);
        filters.add(filter = new ExtensionFilter(VideoExtensions.ASF) {
            protected String getID() {
                return "Type_Video";
            }

            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                QuickFilterTypeTable.this.table2Filter.getPackageControllerTableModel().recreateModel(false);
            }
        });
        knownExtensionFilters.add(filter);
        filters.add(filter = new ExtensionFilter(ImageExtensions.BMP) {
            protected String getID() {
                return "Type_Image";
            }

            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                QuickFilterTypeTable.this.table2Filter.getPackageControllerTableModel().recreateModel(false);
            }
        });
        knownExtensionFilters.add(filter);
        filters.add(filter = new ExtensionFilter(ArchiveExtensions.ACE) {
            protected String getID() {
                return "Type_Archive";
            }

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
        filters.add(new ExtensionFilter(_GUI._.settings_linkgrabber_filter_others(), NewTheme.I().getIcon("help", 16), false) {
            protected String getID() {
                return "Type_Others";
            }

            @Override
            public boolean isFiltered(String ext) {
                if (ext == null) return true;
                for (ExtensionFilter filter : knownExtensionFilters) {
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
        delayedRefresh = new DelayedRunnable(IOEQ.TIMINGQUEUE, REFRESH_MIN, REFRESH_MAX) {

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
        listener = new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                delayedRefresh.run();
            }
        };
        LinkFilterSettings.LG_QUICKFILTER_TYPE_VISIBLE.getEventSender().addListener(this);

        onConfigValueModified(null, LinkFilterSettings.LG_QUICKFILTER_TYPE_VISIBLE.getValue());

    }

    private void updateQuickFilerTableData() {

        // synchronized (LOCK) {
        /* reset existing filter counters */
        for (Filter filter : filters) {
            filter.setCounter(0);
        }
        /* update filter list */

        for (CrawledLink link : ((PackageControllerTableModel<CrawledPackage, CrawledLink>) table2Filter.getExtTableModel()).getAllChildrenNodes()) {

            /*
             * speed optimization, we dont want to get extension several times
             */
            String ext = Files.getExtension(link.getName());
            for (Filter filter : filters) {
                if (((ExtensionFilter) filter).isFiltered(ext)) {
                    filter.setCounter(filter.getCounter() + 1);
                    break;
                }
            }
        }
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
                        for (Filter filter : filters) {
                            if (((ExtensionFilter) filter).isFiltered(ext)) {

                                if (filter.getCounter() == 0 && !filter.isEnabled()) {
                                    filter.setCounter(filter.getMatchCounter());

                                }

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
        final ArrayList<Filter> newTableData = new ArrayList<Filter>(QuickFilterTypeTable.this.getExtTableModel().getTableData().size());
        for (Filter filter : filters) {
            if (filter.getCounter() != 0) {
                /* only add entries with counter >0 to visible table */
                newTableData.add(filter);
            }
        }
        // }

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                header.setVisible(newTableData.size() > 0);
                setVisible(newTableData.size() > 0 && LinkFilterSettings.LG_QUICKFILTER_TYPE_VISIBLE.getValue());
            }
        };
        if (LinkFilterSettings.LG_QUICKFILTER_TYPE_VISIBLE.getValue()) {
            QuickFilterTypeTable.this.getExtTableModel()._fireTableStructureChanged(newTableData, true);
        }

    }

    public void reset() {
        Collection<Filter> lfilters = filters;
        for (Filter filter : lfilters) {
            filter.setMatchCounter(0);
            filter.setCounter(0);
        }
    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (Boolean.TRUE.equals(newValue) && GraphicalUserInterfaceSettings.CFG.isLinkgrabberSidebarEnabled()) {
            enabled = true;
            /* filter is enabled, add listener and run again */
            table2Filter.getPackageControllerTableModel().addFilter(this);
            this.table2Filter.getModel().addTableModelListener(listener);
        } else {
            this.table2Filter.getModel().removeTableModelListener(listener);
            enabled = false;
            /* filter disabled, remove listener */
            old = -1;
            table2Filter.getPackageControllerTableModel().removeFilter(this);

        }
        updateQuickFilerTableData();
        table2Filter.getPackageControllerTableModel().recreateModel(false);
    }

    @Override
    public boolean isFiltered(CrawledLink v) {
        /*
         * speed optimization, we dont want to get extension several times
         */
        if (enabled == false) return false;
        String ext = Files.getExtension(v.getName());
        ArrayList<Filter> lfilters = filters;
        for (Filter filter : lfilters) {
            if (filter.isEnabled()) continue;
            if (((ExtensionFilter) filter).isFiltered(ext)) {
                filter.setMatchCounter(filter.getMatchCounter() + 1);

                return true;
            }
        }
        return false;
    }

}
