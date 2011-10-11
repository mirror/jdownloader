package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.ArrayList;
import java.util.HashMap;

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
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeListener;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.controlling.filter.LinkgrabberFilterRuleWrapper;
import org.jdownloader.controlling.filter.NoDownloadLinkException;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;

public class QuickFilterCustomTable extends FilterTable<CrawledPackage, CrawledLink> implements LinkCollectorListener, GenericConfigEventListener<Boolean>, ChangeListener {

    private static final long                                                             serialVersionUID = 6532789226116928058L;
    private PackageControllerTable<CrawledPackage, CrawledLink>                           table2Filter;
    private HashMap<LinkgrabberFilterRule, CustomizedFilter<CrawledPackage, CrawledLink>> filterMap        = new HashMap<LinkgrabberFilterRule, CustomizedFilter<CrawledPackage, CrawledLink>>();
    private DelayedRunnable                                                               delayedRefresh;
    private final Object                                                                  LOCK             = new Object();
    private long                                                                          old              = -1;

    public QuickFilterCustomTable(PackageControllerTable<CrawledPackage, CrawledLink> table2Filter) {
        super();
        this.table2Filter = table2Filter;

        delayedRefresh = new DelayedRunnable(IOEQ.TIMINGQUEUE, 100l, 1000l) {

            @Override
            public void delayedrun() {
                updateQuickFilerTableData();
            }

        };

        LinkFilterSettings.LG_QUICKFILTER_CUSTOM_VISIBLE.getEventSender().addListener(this);
        onConfigValueModified(null, LinkFilterSettings.LG_QUICKFILTER_CUSTOM_VISIBLE.getValue());
    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (Boolean.TRUE.equals(newValue)) {
            enabled = true;
            /* filter is enabled, add listener and run again */
            LinkFilterController.getInstance().getEventSender().addListener(this);
            LinkCollector.getInstance().addListener(this);
            table2Filter.getPackageControllerTableModel().addFilter(this);
            updateQuickFilerTableData();
        } else {
            enabled = false;
            /* filter disabled, remove listener */
            old = -1;
            table2Filter.getPackageControllerTableModel().removeFilter(this);
            LinkFilterController.getInstance().getEventSender().removeListener(this);
            LinkCollector.getInstance().removeListener(this);
        }
        table2Filter.getPackageControllerTableModel().recreateModel(false);
    }

    private void updateQuickFilerTableData() {
        ArrayList<Filter<CrawledPackage, CrawledLink>> newTableData = null;
        synchronized (LOCK) {
            ArrayList<LinkgrabberFilterRuleWrapper> customFilters = LinkFilterController.getInstance().listQuickFilter();
            HashMap<LinkgrabberFilterRule, CustomizedFilter<CrawledPackage, CrawledLink>> newfilterMap = new HashMap<LinkgrabberFilterRule, CustomizedFilter<CrawledPackage, CrawledLink>>();
            ArrayList<CustomizedFilter<CrawledPackage, CrawledLink>> filters = new ArrayList<CustomizedFilter<CrawledPackage, CrawledLink>>();
            /*
             * update filterMap first,we want to reuse existing filters to
             * maintain enabled status
             */
            for (LinkgrabberFilterRuleWrapper customFilter : customFilters) {
                CustomizedFilter<CrawledPackage, CrawledLink> filter = filterMap.get(customFilter.getRule());
                if (filter == null) {
                    filter = new CustomizedFilter<CrawledPackage, CrawledLink>(customFilter) {

                        @Override
                        public boolean isFiltered(CrawledLink link) {
                            boolean ret = _isFiltered(link);
                            if (lgr.getRule().isAccept()) {
                                return ret;
                            } else {
                                return !ret;
                            }
                        }

                        private boolean _isFiltered(CrawledLink link) {
                            try {
                                if (!lgr.checkHoster(link)) return false;
                            } catch (NoDownloadLinkException e) {
                                return false;
                            }
                            if (!lgr.checkSource(link)) return false;
                            if (!lgr.checkFileName(link)) return false;
                            if (!lgr.checkFileSize(link)) return false;
                            if (!lgr.checkFileType(link)) return false;
                            return true;
                        }

                        @Override
                        public boolean isFiltered(CrawledPackage link) {
                            /* we do not filter packages */
                            return false;
                        }

                        @Override
                        public void setEnabled(boolean enabled) {
                            super.setEnabled(enabled);
                            /*
                             * request recreate the model of filtered view
                             */
                            table2Filter.getPackageControllerTableModel().recreateModel(false);
                        }

                    };
                }
                filter.setCounter(0);
                filters.add(filter);
                newfilterMap.put(customFilter.getRule(), filter);
            }
            filterMap = newfilterMap;
            /* update filter list */
            boolean readL = LinkCollector.getInstance().readLock();
            try {
                for (CrawledPackage pkg : LinkCollector.getInstance().getPackages()) {
                    synchronized (pkg) {
                        for (CrawledLink link : pkg.getChildren()) {
                            for (Filter<CrawledPackage, CrawledLink> filter : filters) {
                                if (((CustomizedFilter<CrawledPackage, CrawledLink>) filter).isFiltered(link)) {
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
            ArrayList<Filter<CrawledPackage, CrawledLink>> newfilters = new ArrayList<Filter<CrawledPackage, CrawledLink>>();
            newTableData = new ArrayList<Filter<CrawledPackage, CrawledLink>>(QuickFilterCustomTable.this.getExtTableModel().getTableData().size());
            for (Filter<CrawledPackage, CrawledLink> filter : filters) {
                if (filter.getCounter() > 0) {
                    /* only add entries with counter >0 to visible table */
                    newTableData.add(filter);
                }
                newfilters.add(filter);
            }
            this.filters = newfilters;
        }
        QuickFilterCustomTable.this.getExtTableModel()._fireTableStructureChanged(newTableData, true);
    }

    @Override
    public boolean isFiltered(CrawledPackage e) {
        /* we do not filter packages */
        return false;
    }

    public void onLinkCollectorEvent(LinkCollectorEvent event) {
        switch (event.getType()) {
        case REMOVE_CONTENT:
        case REFRESH_STRUCTURE:
            if (Boolean.TRUE.equals(LinkFilterSettings.LG_QUICKFILTER_CUSTOM_VISIBLE.getValue()) && old != LinkCollector.getInstance().getChildrenChanges()) {
                old = LinkCollector.getInstance().getChildrenChanges();
                delayedRefresh.run();
            }
            break;
        }
    }

    public void onChangeEvent(ChangeEvent event) {
        delayedRefresh.run();
    }
}
