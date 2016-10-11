package org.jdownloader.controlling.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.controlling.TaskQueue;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawlerFilter;

import org.appwork.exceptions.WTFException;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.ConfigEvent;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.event.EventSuppressor;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeEventSender;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.logging.LogController;

public class LinkFilterController implements LinkCrawlerFilter {
    private static final LinkFilterController INSTANCE = new LinkFilterController(false);

    /**
     * get the only existing instance of LinkFilterController. This is a singleton
     *
     * @return
     */
    public static LinkFilterController getInstance() {
        return LinkFilterController.INSTANCE;
    }

    public static LinkFilterController createEmptyTestInstance() {
        return new LinkFilterController(true);
    }

    private volatile ArrayList<LinkgrabberFilterRule> filter;
    private final LinkFilterSettings                  config;

    public List<LinkgrabberFilterRuleWrapper> getAcceptFilters() {
        return acceptFilters;
    }

    private volatile List<LinkgrabberFilterRuleWrapper> acceptFilters = null;
    private volatile List<LinkgrabberFilterRuleWrapper> denyFilters   = null;
    private final KeyHandler<Object>                    filterListHandler;

    private final ChangeEventSender                     eventSender;
    private final boolean                               testInstance;

    /**
     * Create a new instance of LinkFilterController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    public LinkFilterController(boolean testInstance) {
        eventSender = new ChangeEventSender();
        this.testInstance = testInstance;
        if (isTestInstance() == false) {
            config = JsonConfig.create(LinkFilterSettings.class);
            filterListHandler = config._getStorageHandler().getKeyHandler("FilterList");
            filter = readConfig();
            filterListHandler.getEventSender().addListener(new GenericConfigEventListener<Object>() {

                @Override
                public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
                    filter = readConfig();
                    update();
                }

                @Override
                public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
                }
            });
            ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

                @Override
                public void onShutdown(final ShutdownRequest shutdownRequest) {
                    save(filter);
                }

                @Override
                public long getMaxDuration() {
                    return 0;
                }

                @Override
                public String toString() {
                    return "save filters...";
                }
            });
            updateInternal();
        } else {
            this.config = null;
            filterListHandler = null;
            filter = new ArrayList<LinkgrabberFilterRule>();
        }
    }

    public ChangeEventSender getEventSender() {
        return eventSender;
    }

    private ArrayList<LinkgrabberFilterRule> readConfig() {
        final ArrayList<LinkgrabberFilterRule> newList = new ArrayList<LinkgrabberFilterRule>();
        if (config != null) {
            ArrayList<LinkgrabberFilterRule> filter = config.getFilterList();
            if (filter == null) {
                filter = new ArrayList<LinkgrabberFilterRule>();
            }
            boolean dupesView = false;
            boolean offlineRule = false;
            boolean directHttpView = false;
            HashSet<String> dupefinder = new HashSet<String>();

            for (LinkgrabberFilterRule rule : filter) {
                LinkgrabberFilterRule clone = JSonStorage.restoreFromString(JSonStorage.serializeToJson(rule), new TypeRef<LinkgrabberFilterRule>() {
                });
                clone.setCreated(-1);
                if (!dupefinder.add(JSonStorage.serializeToJson(clone))) {
                    //
                    continue;
                }
                if (OfflineView.ID.equals(rule.getId())) {
                    OfflineView r;
                    newList.add(r = new OfflineView());
                    r.init();
                    r.setEnabled(rule.isEnabled());
                    offlineRule = true;
                    continue;

                }
                if (DirectHTTPView.ID.equals(rule.getId())) {
                    DirectHTTPView r;
                    newList.add(r = new DirectHTTPView());
                    r.init();
                    r.setEnabled(rule.isEnabled());
                    directHttpView = true;
                    continue;

                }
                if (DupesView.ID.equals(rule.getId())) {
                    DupesView r;
                    newList.add(r = new DupesView());
                    r.init();
                    r.setEnabled(rule.isEnabled());
                    dupesView = true;
                    continue;

                }
                newList.add(rule);
            }
            if (!directHttpView) {
                newList.add(new DirectHTTPView().init());
            }
            if (!offlineRule) {
                newList.add(new OfflineView().init());
            }
            if (!dupesView) {
                newList.add(new DupesView().init());
            }
        }
        return newList;
    }

    private void updateInternal() {
        // url filter only require the urls, and thus can be done
        // brefore
        // linkcheck
        final ArrayList<LinkgrabberFilterRuleWrapper> newDenyFilters = new ArrayList<LinkgrabberFilterRuleWrapper>();
        final ArrayList<LinkgrabberFilterRuleWrapper> newAcceptlFilters = new ArrayList<LinkgrabberFilterRuleWrapper>();
        synchronized (this) {
            for (final LinkgrabberFilterRule lgr : filter) {
                if (lgr.isEnabled() && lgr.isValid()) {
                    try {
                        final LinkgrabberFilterRuleWrapper compiled = lgr.compile();
                        lgr._setBroken(false);
                        if (lgr.isAccept()) {
                            newAcceptlFilters.add(compiled);
                        } else {
                            newDenyFilters.add(compiled);
                        }
                    } catch (final Throwable e) {
                        lgr.setEnabled(false);
                        lgr._setBroken(true);
                        LogController.CL().log(e);
                    }
                }
            }
            if (!isTestInstance()) {
                if (denyFilters != null && denyFilters.size() != newDenyFilters.size()) {
                    save(filter);
                } else if (acceptFilters != null && acceptFilters.size() != newAcceptlFilters.size()) {
                    save(filter);
                }
            }
        }
        denyFilters = newDenyFilters;
        acceptFilters = newAcceptlFilters;
        if (getEventSender().hasListener()) {
            getEventSender().fireEvent(new ChangeEvent(LinkFilterController.this));
        }
    }

    public void update() {
        if (isTestInstance()) {
            updateInternal();
        } else {
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    updateInternal();
                    return null;
                }

            });
        }
    }

    public boolean isTestInstance() {
        return testInstance;
    }

    public java.util.List<LinkgrabberFilterRule> list() {
        synchronized (this) {
            return new ArrayList<LinkgrabberFilterRule>(filter);
        }
    }

    public void addAll(java.util.List<LinkgrabberFilterRule> all) {
        if (all == null) {
            return;
        }
        synchronized (this) {
            filter.addAll(all);
            save(filter);
            update();
        }
    }

    private synchronized final void save(ArrayList<LinkgrabberFilterRule> filter) {
        if (config != null) {
            final EventSuppressor<ConfigEvent> eventSuppressor;
            if (filterListHandler != null) {
                final Thread thread = Thread.currentThread();
                eventSuppressor = new EventSuppressor<ConfigEvent>() {

                    @Override
                    public boolean suppressEvent(ConfigEvent eventType) {
                        return Thread.currentThread() == thread;
                    }
                };
                filterListHandler.getEventSender().addEventSuppressor(eventSuppressor);
            } else {
                eventSuppressor = null;
            }
            try {
                config.setFilterList(filter);
            } finally {
                if (filterListHandler != null) {
                    filterListHandler.getEventSender().removeEventSuppressor(eventSuppressor);
                }
            }
        }
    }

    public void add(LinkgrabberFilterRule linkFilter) {
        if (linkFilter == null) {
            return;
        }
        synchronized (this) {
            filter.add(linkFilter);
            save(filter);
        }
        update();
    }

    public void remove(LinkgrabberFilterRule lf) {
        if (lf == null) {
            return;
        }
        synchronized (this) {
            filter.remove(lf);
            save(filter);
        }
        update();
    }

    public boolean dropByUrl(CrawledLink link) {
        if (link.getMatchingFilter() != null) {
            /*
             * links with set matching filtered are allowed, user wants to add them
             */
            return false;
        }
        if (isTestInstance() == false && !org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINK_FILTER_ENABLED.isEnabled()) {
            return false;
        }
        for (final LinkgrabberFilterRuleWrapper lgr : denyFilters) {
            if (matches(link, lgr, false)) {
                link.setMatchingFilter(lgr.getRule());
                return true;
            }
        }
        return false;
    }

    private boolean matches(CrawledLink link, LinkgrabberFilterRuleWrapper rule, final boolean afterOnlineCheck) {
        if (!rule.checkHoster(link)) {
            return false;
        }
        if (!rule.checkPluginStatus(link)) {
            return false;
        }
        if (!isTestInstance()) {
            if (!rule.checkOrigin(link)) {
                return false;
            }
            if (!rule.checkConditions(link)) {
                return false;
            }
        }
        if (!rule.checkSource(link)) {
            return false;
        }
        if (!rule.checkOnlineStatus(link)) {
            return false;
        }
        if (!rule.checkFileName(link)) {
            return false;
        }
        if (!rule.checkPackageName(link)) {
            return false;
        }
        if (!rule.checkFileSize(link)) {
            return false;
        }
        if (!rule.checkFileType(link)) {
            return false;
        }
        return true;
    }

    public boolean dropByFileProperties(CrawledLink link) {
        if (link.getMatchingFilter() != null && link.getMatchingFilter() instanceof LinkgrabberFilterRule && !((LinkgrabberFilterRule) link.getMatchingFilter()).isAccept()) {
            /*
             * links with set matching filtered are allowed, user wants to add them
             */
            return false;
        }
        if (isTestInstance() == false && !org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINK_FILTER_ENABLED.isEnabled()) {
            return false;
        }
        if (link.getDownloadLink() == null) {
            throw new WTFException();
        }
        for (final LinkgrabberFilterRuleWrapper lgr : denyFilters) {
            if (matches(link, lgr, true)) {
                link.setMatchingFilter(lgr.getRule());
                return true;
            }
        }
        return false;
    }

    public java.util.List<LinkgrabberFilterRule> listFilters() {
        synchronized (this) {
            final List<LinkgrabberFilterRule> lst = filter;
            final List<LinkgrabberFilterRule> ret = new ArrayList<LinkgrabberFilterRule>();
            for (final LinkgrabberFilterRule l : lst) {
                if (!l.isAccept()) {
                    ret.add(l);
                }
            }
            return ret;
        }
    }

    public java.util.List<LinkgrabberFilterRule> listExceptions() {
        synchronized (this) {
            final List<LinkgrabberFilterRule> lst = filter;
            final List<LinkgrabberFilterRule> ret = new ArrayList<LinkgrabberFilterRule>();
            for (final LinkgrabberFilterRule l : lst) {
                if (l.isAccept()) {
                    ret.add(l);
                }
            }
            return ret;
        }
    }
}
