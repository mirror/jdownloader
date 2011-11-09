package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.JPopupMenu;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.Hash;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeListener;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.filter.FilterRule;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.controlling.filter.LinkgrabberFilterRuleWrapper;
import org.jdownloader.controlling.filter.NoDownloadLinkException;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class QuickFilterExceptionsTable extends FilterTable implements GenericConfigEventListener<Boolean> {

    /**
     * 
     */
    private static final long                                   serialVersionUID = 658947589171018284L;

    private long                                                old              = -1;
    private DelayedRunnable                                     delayedRefresh;
    private PackageControllerTable<CrawledPackage, CrawledLink> table2Filter     = null;
    private final Object                                        LOCK             = new Object();
    private CustomFilterHeader                                  header;

    private HashMap<FilterRule, Filter>                         map;

    private TableModelListener                                  listener;

    public QuickFilterExceptionsTable(CustomFilterHeader exceptions, PackageControllerTable<CrawledPackage, CrawledLink> table) {
        super();
        header = exceptions;
        header.setFilterCount(0);
        this.table2Filter = table;
        delayedRefresh = new DelayedRunnable(IOEQ.TIMINGQUEUE, REFRESH_MIN, REFRESH_MAX) {

            @Override
            public void delayedrun() {
                updateQuickFilerTableData();
            }

        };

        LinkFilterController.getInstance().getEventSender().addListener(new ChangeListener() {

            public void onChangeEvent(ChangeEvent event) {
                updateFilters();
            }
        });
        updateFilters();
        listener = new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                delayedRefresh.run();
            }
        };
        LinkFilterSettings.LG_QUICKFILTER_EXCEPTIONS_VISIBLE.getEventSender().addListener(this);

        onConfigValueModified(null, LinkFilterSettings.LG_QUICKFILTER_EXCEPTIONS_VISIBLE.getValue());

    }

    protected void updateFilters() {
        synchronized (LOCK) {
            ArrayList<LinkgrabberFilterRuleWrapper> fileFilter = LinkFilterController.getInstance().getAcceptFileFilter();
            ArrayList<LinkgrabberFilterRuleWrapper> urlFilter = LinkFilterController.getInstance().getAcceptUrlFilter();

            map = new HashMap<FilterRule, Filter>();
            setup(fileFilter);
            setup(urlFilter);
        }
    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, Filter contextObject, ArrayList<Filter> selection, ExtColumn<Filter> column) {
        ArrayList<String> ret = new ArrayList<String>();
        // for (Filter f : selection) {
        // ret.add(f.getName());
        // }
        // popup.add(new DropHosterAction(ret).toContextMenuAction());
        // popup.add(new KeepOnlyAction(ret).toContextMenuAction());

        return popup;
    }

    private void updateQuickFilerTableData() {
        ArrayList<Filter> newTableData = null;
        // synchronized (LOCK) {
        newTableData = new ArrayList<Filter>(0);

        Iterator<Entry<FilterRule, Filter>> it;
        Entry<FilterRule, Filter> next;

        for (it = map.entrySet().iterator(); it.hasNext();) {
            next = it.next();
            // if (next.getValue().isEnabled()) {
            next.getValue().setCounter(0);

            newTableData.add(next.getValue());
        }
        for (CrawledLink link : ((PackageControllerTableModel<CrawledPackage, CrawledLink>) table2Filter.getExtTableModel()).getAllChildrenNodes()) {
            for (it = map.entrySet().iterator(); it.hasNext();) {
                next = it.next();
                if (next.getValue().isEnabled()) {
                    if (next.getValue().isFiltered(link)) {
                        int c = next.getValue().getCounter();

                        next.getValue().setCounter(c + 1);
                    }
                }
            }

        }
        /* update filter list */
        boolean readL = LinkCollector.getInstance().readLock();
        try {

            for (CrawledPackage pkg : LinkCollector.getInstance().getPackages()) {
                synchronized (pkg) {
                    for (CrawledLink link : pkg.getChildren()) {
                        for (it = map.entrySet().iterator(); it.hasNext();) {
                            next = it.next();
                            if (next.getValue().getCounter() <= 0) {
                                if (!next.getValue().isEnabled() && next.getValue().isFiltered(link)) {
                                    next.getValue().setCounter(next.getValue().getMatchCounter());

                                }
                            }
                        }
                    }
                }
            }
        } finally {
            LinkCollector.getInstance().readUnlock(readL);
        }
        /* update FilterTableModel */
        for (Iterator<Filter> itt = newTableData.iterator(); itt.hasNext();) {
            if (itt.next().getCounter() == 0) itt.remove();
        }
        filters = newTableData;

        // }

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                header.setVisible(filters.size() > 0);
                setVisible(filters.size() > 0 && LinkFilterSettings.LG_QUICKFILTER_EXCEPTIONS_VISIBLE.getValue());
            }
        };
        if (LinkFilterSettings.LG_QUICKFILTER_EXCEPTIONS_VISIBLE.getValue()) {
            QuickFilterExceptionsTable.this.getExtTableModel()._fireTableStructureChanged(newTableData, true);
        }

    }

    private void setup(ArrayList<LinkgrabberFilterRuleWrapper> fileFilter) {
        for (final LinkgrabberFilterRuleWrapper rule : fileFilter) {
            Filter filter = map.get(rule.getRule());
            if (filter == null) {

                filter = new Filter(rule.getName(), null) {
                    public String getDescription() {
                        return rule.getRule().toString();
                    }

                    protected String getID() {
                        return "Custom_" + Hash.getMD5(rule.getName() + ":" + getDescription());
                    }

                    @Override
                    public boolean isFiltered(CrawledLink link) {
                        try {
                            if (!rule.checkHoster(link)) return false;
                        } catch (NoDownloadLinkException e) {

                            throw new WTFException();
                        }
                        if (!rule.checkSource(link)) return false;
                        if (!rule.checkOnlineStatus(link)) return false;

                        if (!rule.checkFileName(link)) return false;
                        if (!rule.checkFileSize(link)) return false;
                        if (!rule.checkFileType(link)) return false;

                        return true;
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
                map.put(rule.getRule(), filter);
            }
            filter.setCounter(0);

        }
    }

    public void reset() {
        Collection<Filter> lfilters = map.values();
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
            table2Filter.getPackageControllerTableModel().addFilter(this);

            this.table2Filter.getModel().addTableModelListener(listener);
        } else {
            this.table2Filter.getModel().removeTableModelListener(listener);

            enabled = false;
            /* filter disabled */
            old = -1;
            table2Filter.getPackageControllerTableModel().removeFilter(this);
        }
        updateQuickFilerTableData();
        table2Filter.getPackageControllerTableModel().recreateModel(false);
    }

}
