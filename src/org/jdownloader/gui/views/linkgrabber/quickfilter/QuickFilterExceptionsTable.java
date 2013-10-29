package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.ImageIcon;

import jd.SecondLevelLaunch;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.ExceptionsRuleDialog;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.FilterRuleDialog;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Hash;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeListener;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.filter.FilterRule;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.controlling.filter.LinkgrabberFilterRuleWrapper;
import org.jdownloader.controlling.filter.NoDownloadLinkException;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.images.NewTheme;

public class QuickFilterExceptionsTable extends FilterTable {

    /**
     * 
     */
    private static final long           serialVersionUID = 658947589171018284L;
    private HashMap<FilterRule, Filter> filterMap        = new HashMap<FilterRule, Filter>(); ;

    public QuickFilterExceptionsTable(CustomFilterHeader exceptions, LinkGrabberTable table) {
        super(exceptions, table, org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_EXCEPTIONS_QUICKFILTER_ENABLED);
        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                LinkFilterController.getInstance().getEventSender().addListener(new ChangeListener() {

                    public void onChangeEvent(ChangeEvent event) {
                        updateFilters();
                        updateNow();
                    }
                });
                updateFilters();
            }
        });
    }

    protected void updateFilters() {
        filterMap = new HashMap<FilterRule, Filter>();
        java.util.List<LinkgrabberFilterRuleWrapper> fileFilter = LinkFilterController.getInstance().getAcceptFileFilter();
        java.util.List<LinkgrabberFilterRuleWrapper> urlFilter = LinkFilterController.getInstance().getAcceptUrlFilter();

        setup(fileFilter);
        setup(urlFilter);

    }

    protected java.util.List<Filter> updateQuickFilerTableData() {
        java.util.List<Filter> newTableData = null;
        // synchronized (LOCK) {
        newTableData = new ArrayList<Filter>(0);

        Iterator<Entry<FilterRule, Filter>> it;
        Entry<FilterRule, Filter> next;
        HashSet<Filter> filtersInUse = new HashSet<Filter>();
        HashSet<CrawledLink> map = new HashSet<CrawledLink>();
        java.util.List<CrawledLink> filteredLinks = new ArrayList<CrawledLink>();

        for (CrawledLink link : getVisibleLinks()) {
            map.add(link);
            for (it = filterMap.entrySet().iterator(); it.hasNext();) {
                next = it.next();
                if (next.getValue().isEnabled()) {
                    if (next.getValue().isFiltered(link)) {
                        int c = next.getValue().getCounter();
                        next.getValue().setCounter(c + 1);
                        filtersInUse.add(next.getValue());
                    }
                }
            }

        }
        boolean readL = LinkCollector.getInstance().readLock();
        try {

            for (CrawledPackage pkg : LinkCollector.getInstance().getPackages()) {
                synchronized (pkg) {
                    for (CrawledLink link : pkg.getChildren()) {
                        if (map.add(link)) {
                            filteredLinks.add(link);
                        }
                        for (Filter filter : filterMap.values()) {

                            if (filter.isFiltered(link)) {
                                filtersInUse.add(filter);
                            }
                        }
                    }
                }
            }
        } finally {
            LinkCollector.getInstance().readUnlock(readL);
        }

        for (Filter filter : filtersInUse) {
            if (filter.getCounter() == 0) {
                filter.setCounter(getCountWithout(filter, filteredLinks));
            }
        }
        /* update FilterTableModel */
        // java.util.List<Filter> newfilters = new
        // java.util.List<Filter>();

        return new ArrayList<Filter>(filtersInUse);

    }

    protected boolean onDoubleClick(final MouseEvent e, final Filter obj) {
        // JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
        // JDGui.getInstance().setContent(ConfigurationView.getInstance(),
        // true);
        // LinkgrabberFilter.getInstance().setSelectedIndex(1);
        // ConfigurationView.getInstance().setSelectedSubPanel(jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.Linkgrabber.class);

        for (Iterator<Entry<FilterRule, Filter>> it = filterMap.entrySet().iterator(); it.hasNext();) {
            Entry<FilterRule, Filter> next = it.next();
            if (next.getValue() == obj) {
                try {
                    LinkgrabberFilterRule filterRule = ((LinkgrabberFilterRule) next.getKey());
                    if (filterRule.isAccept()) {
                        Dialog.getInstance().showDialog(new ExceptionsRuleDialog(filterRule));
                    } else {
                        Dialog.getInstance().showDialog(new FilterRuleDialog(filterRule));
                    }
                    LinkFilterController.getInstance().update();
                    // update?
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
                return false;
            }
        }
        return false;
    }

    private void setup(java.util.List<LinkgrabberFilterRuleWrapper> fileFilter) {
        for (final LinkgrabberFilterRuleWrapper rule : fileFilter) {
            Filter filter = filterMap.get(rule.getRule());
            if (filter == null) {

                filter = new Filter(rule.getName(), null) {
                    public String getDescription() {
                        return rule.getRule().toString();

                    }

                    public ImageIcon getIcon() {
                        if (rule.getRule().getIconKey() == null) {
                            return null;
                        } else {
                            return NewTheme.I().getIcon(rule.getRule().getIconKey(), 16);
                        }
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
                        try {
                            if (!rule.checkPluginStatus(link)) return false;
                        } catch (NoDownloadLinkException e) {
                            throw new WTFException();
                        }
                        if (!rule.checkOrigin(link)) return false;
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
                        getLinkgrabberTable().getModel().recreateModel(false);
                        updateAllFiltersInstant();
                    }
                };
                filterMap.put(rule.getRule(), filter);
            }
            filter.setCounter(0);

        }
    }

    public void reset() {
        Collection<Filter> lfilters = filterMap.values();
        for (Filter filter : lfilters) {
            filter.setCounter(0);
        }
    }

    @Override
    java.util.List<Filter> getAllFilters() {
        return new ArrayList<Filter>(filterMap.values());
    }

    public boolean highlightFilter() {
        return false;
    }

}
