package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import jd.SecondLevelLaunch;
import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.ExceptionsRuleDialog;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.FilterRuleDialog;

import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeListener;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.controlling.filter.LinkgrabberFilterRuleWrapper;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;

public class QuickFilterExceptionsTable extends FilterTable {

    /**
     * 
     */
    private static final long                              serialVersionUID = 658947589171018284L;
    private volatile CopyOnWriteArrayList<ExceptionFilter> allFilters       = new CopyOnWriteArrayList<ExceptionFilter>();
    private volatile CopyOnWriteArraySet<Filter>           enabledFilters   = new CopyOnWriteArraySet<Filter>();

    public QuickFilterExceptionsTable(CustomFilterHeader exceptions, LinkGrabberTable table) {
        super(exceptions, table, org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_EXCEPTIONS_QUICKFILTER_ENABLED);
        init();
    }

    private void init() {
        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                LinkFilterController.getInstance().getEventSender().addListener(new ChangeListener() {

                    public void onChangeEvent(ChangeEvent event) {
                        convertLinkgrabberFilterRuleWrapper();
                    }
                });
                convertLinkgrabberFilterRuleWrapper();
            }
        });
    }

    private void convertLinkgrabberFilterRuleWrapper() {
        List<LinkgrabberFilterRuleWrapper> filtersToSetup = new ArrayList<LinkgrabberFilterRuleWrapper>();
        List<LinkgrabberFilterRuleWrapper> filters = LinkFilterController.getInstance().getAcceptFileFilter();
        if (filters != null) filtersToSetup.addAll(filters);
        filters = LinkFilterController.getInstance().getAcceptUrlFilter();
        if (filters != null) filtersToSetup.addAll(filters);
        CopyOnWriteArrayList<ExceptionFilter> newAllFilters = new CopyOnWriteArrayList<ExceptionFilter>();
        CopyOnWriteArraySet<Filter> newEnabledFilters = new CopyOnWriteArraySet<Filter>();
        for (final LinkgrabberFilterRuleWrapper rule : filtersToSetup) {
            ExceptionFilter filter = new ExceptionFilter(rule) {
                @Override
                public void setEnabled(boolean enabled) {
                    super.setEnabled(enabled);
                    QuickFilterExceptionsTable.this.setEnabled(enabled, this);
                }
            };
            newAllFilters.add(filter);
            if (!filter.isEnabled()) newEnabledFilters.add(filter);
        }
        allFilters = newAllFilters;
        enabledFilters = newEnabledFilters;
        requestUpdate();
        getLinkgrabberTable().getModel().recreateModel(false);
    }

    private void setEnabled(boolean enabled, Filter filter) {
        if (!enabled) {
            enabledFilters.add(filter);
        } else {
            enabledFilters.remove(filter);
        }
        getLinkgrabberTable().getModel().recreateModel(false);
    }

    protected boolean onDoubleClick(final MouseEvent e, final Filter obj) {
        for (ExceptionFilter exceptionFilter : allFilters) {
            if (exceptionFilter == obj) {
                try {
                    LinkgrabberFilterRule filterRule = exceptionFilter.getWrapperRule().getRule();
                    if (filterRule.isAccept()) {
                        Dialog.getInstance().showDialog(new ExceptionsRuleDialog(filterRule));
                    } else {
                        Dialog.getInstance().showDialog(new FilterRuleDialog(filterRule));
                    }
                    LinkFilterController.getInstance().update();
                } catch (DialogNoAnswerException e1) {
                    e1.printStackTrace();
                }
                return false;
            }
        }
        return false;
    }

    @Override
    protected FilterTableDataUpdater getFilterTableDataUpdater() {
        return new FilterTableDataUpdater() {
            List<Filter> availableFilters = new ArrayList<Filter>(allFilters);
            Set<Filter>  usedFilters      = new HashSet<Filter>();

            @Override
            public void updateVisible(CrawledLink link) {
                for (Filter filter : availableFilters) {
                    if (filter.isFiltered(link)) {
                        usedFilters.add(filter);
                        filter.increaseCounter();
                    }
                }
            }

            @Override
            public void updateFiltered(CrawledLink link) {
                if (availableFilters.size() > 0) {
                    Iterator<Filter> it = availableFilters.iterator();
                    while (it.hasNext()) {
                        Filter filter = it.next();
                        if (((ExceptionFilter) filter).isFiltered(link)) {
                            usedFilters.add(filter);
                            it.remove();
                            break;
                        }
                    }
                }
            }

            @Override
            public void reset() {
                for (Filter filter : availableFilters) {
                    filter.resetCounter();
                }
            }

            @Override
            public FilterTable getFilterTable() {
                return QuickFilterExceptionsTable.this;
            }

            @Override
            public List<Filter> finalizeUpdater() {
                return new ArrayList<Filter>(usedFilters);
            }

            @Override
            public void afterVisible() {
                availableFilters.removeAll(usedFilters);
            }

            @Override
            public boolean hasNewDisabledFilters() {
                return false;
            }
        };
    }

    @Override
    public int getComplexity() {
        return 100;
    }

    @Override
    public boolean isFiltered(CrawledLink v) {
        Filter exception = getFilterException();
        for (Filter enabledFilter : enabledFilters) {
            if (enabledFilter == exception) continue;
            if (enabledFilter.isFiltered(v)) return true;
        }
        return false;
    }

    @Override
    public boolean isFilteringChildrenNodes() {
        return isEnabled() && enabledFilters.size() > 0;
    }

}
