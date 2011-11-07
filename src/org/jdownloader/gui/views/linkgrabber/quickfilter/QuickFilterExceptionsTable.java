package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JPopupMenu;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeListener;
import org.jdownloader.controlling.filter.FilterRule;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.controlling.filter.LinkgrabberFilterRuleWrapper;
import org.jdownloader.controlling.filter.NoDownloadLinkException;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;
import org.jdownloader.gui.views.linkgrabber.Header;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class QuickFilterExceptionsTable extends FilterTable<CrawledPackage, CrawledLink> implements LinkCollectorListener, GenericConfigEventListener<Boolean> {

    /**
     * 
     */
    private static final long                                        serialVersionUID = 658947589171018284L;

    private long                                                     old              = -1;
    private DelayedRunnable                                          delayedRefresh;
    private PackageControllerTable<CrawledPackage, CrawledLink>      table2Filter     = null;
    private final Object                                             LOCK             = new Object();
    private Header                                                   header;

    private HashMap<FilterRule, Filter<CrawledPackage, CrawledLink>> map;

    public QuickFilterExceptionsTable(Header hosterFilter, PackageControllerTable<CrawledPackage, CrawledLink> table) {
        super();
        header = hosterFilter;
        header.setFilterCount(0);
        this.table2Filter = table;
        delayedRefresh = new DelayedRunnable(IOEQ.TIMINGQUEUE, 100l, 1000l) {

            @Override
            public void delayedrun() {
                // updateQuickFilerTableData();
            }

        };
        GraphicalUserInterfaceSettings.LINKGRABBER_SIDEBAR_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                /* we call a different onConfigValueModified here */
                QuickFilterExceptionsTable.this.onConfigValueModified(null, LinkFilterSettings.LG_QUICKFILTER_EXCEPTIONS_VISIBLE.getValue());
            }

        });

        LinkFilterSettings.LG_QUICKFILTER_EXCEPTIONS_VISIBLE.getEventSender().addListener(this);
        LinkCollector.getInstance().addListener(this);
        onConfigValueModified(null, LinkFilterSettings.LG_QUICKFILTER_EXCEPTIONS_VISIBLE.getValue());
        table2Filter.getModel().addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {

                List<CrawledLink> td = ((PackageControllerTableModel<CrawledPackage, CrawledLink>) table2Filter.getExtTableModel()).getAllChildrenNodes();
                updateQuickFilerTableData(td);
            }
        });

        LinkFilterController.getInstance().getEventSender().addListener(new ChangeListener() {

            public void onChangeEvent(ChangeEvent event) {
                updateFilters();
            }
        });
        updateFilters();
    }

    protected void updateFilters() {
        synchronized (LOCK) {
            ArrayList<LinkgrabberFilterRuleWrapper> fileFilter = LinkFilterController.getInstance().getAcceptFileFilter();
            ArrayList<LinkgrabberFilterRuleWrapper> urlFilter = LinkFilterController.getInstance().getAcceptUrlFilter();

            map = new HashMap<FilterRule, Filter<CrawledPackage, CrawledLink>>();
            setup(fileFilter);
            setup(urlFilter);
        }
    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, Filter<CrawledPackage, CrawledLink> contextObject, ArrayList<Filter<CrawledPackage, CrawledLink>> selection, ExtColumn<Filter<CrawledPackage, CrawledLink>> column) {
        ArrayList<String> ret = new ArrayList<String>();
        // for (Filter<CrawledPackage, CrawledLink> f : selection) {
        // ret.add(f.getName());
        // }
        // popup.add(new DropHosterAction(ret).toContextMenuAction());
        // popup.add(new KeepOnlyAction(ret).toContextMenuAction());

        return popup;
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

    private void updateQuickFilerTableData(List<CrawledLink> td) {
        ArrayList<Filter<CrawledPackage, CrawledLink>> newTableData = null;
        synchronized (LOCK) {
            newTableData = new ArrayList<Filter<CrawledPackage, CrawledLink>>(0);

            /* update filter list */
            boolean readL = LinkCollector.getInstance().readLock();
            Iterator<Entry<FilterRule, Filter<CrawledPackage, CrawledLink>>> it;
            Entry<FilterRule, Filter<CrawledPackage, CrawledLink>> next;
            try {
                for (it = map.entrySet().iterator(); it.hasNext();) {
                    next = it.next();
                    next.getValue().setCounter(0);
                    newTableData.add(next.getValue());
                }
                for (CrawledLink link : td) {
                    for (it = map.entrySet().iterator(); it.hasNext();) {
                        next = it.next();
                        if (!next.getValue().isEnabled()) {
                            if (next.getValue().isFiltered(link)) {
                                int c = next.getValue().getCounter();

                                next.getValue().setCounter(c + 1);
                            }
                        } else {
                            next.getValue().setCounter(-1);
                        }
                    }

                }

            } finally {
                LinkCollector.getInstance().readUnlock(readL);
            }
            /* update FilterTableModel */
            for (Iterator<Filter<CrawledPackage, CrawledLink>> itt = newTableData.iterator(); itt.hasNext();) {
                if (itt.next().getCounter() == 0) itt.remove();
            }
            filters = newTableData;

        }
        header.setFilterCount(filters.size());

        if (LinkFilterSettings.LG_QUICKFILTER_EXCEPTIONS_VISIBLE.getValue()) {
            QuickFilterExceptionsTable.this.getExtTableModel()._fireTableStructureChanged(newTableData, true);
        }
    }

    private void setup(ArrayList<LinkgrabberFilterRuleWrapper> fileFilter) {
        for (final LinkgrabberFilterRuleWrapper rule : fileFilter) {
            Filter<CrawledPackage, CrawledLink> filter = map.get(rule.getRule());
            if (filter == null) {

                filter = new Filter<CrawledPackage, CrawledLink>(rule.getName(), null, !rule.isEnabled()) {
                    public String getDescription() {
                        return rule.getRule().toString();
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
                map.put(rule.getRule(), filter);
            }
            filter.setCounter(0);

        }
    }

    @Override
    public boolean isFiltered(CrawledPackage e) {
        /* we do not filter packages */
        return false;
    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (Boolean.TRUE.equals(newValue) && GraphicalUserInterfaceSettings.CFG.isLinkgrabberSidebarEnabled()) {
            enabled = true;
            table2Filter.getPackageControllerTableModel().addFilter(this);
            // updateQuickFilerTableData();
            setVisible(true);
        } else {
            setVisible(false);
            enabled = false;
            /* filter disabled */
            old = -1;
            table2Filter.getPackageControllerTableModel().removeFilter(this);
        }
        table2Filter.getPackageControllerTableModel().recreateModel(false);
    }

}
