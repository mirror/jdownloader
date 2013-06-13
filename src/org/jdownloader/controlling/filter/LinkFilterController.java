package org.jdownloader.controlling.filter;

import java.util.ArrayList;

import jd.controlling.IOEQ;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawlerFilter;
import jd.plugins.DownloadLink;

import org.appwork.exceptions.WTFException;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeEventSender;

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

    private ArrayList<LinkgrabberFilterRule>             filter;
    private LinkFilterSettings                           config;
    private java.util.List<LinkgrabberFilterRuleWrapper> denyFileFilter;

    public java.util.List<LinkgrabberFilterRuleWrapper> getAcceptFileFilter() {
        return acceptFileFilter;
    }

    public java.util.List<LinkgrabberFilterRuleWrapper> getAcceptUrlFilter() {
        return acceptUrlFilter;
    }

    private java.util.List<LinkgrabberFilterRuleWrapper> acceptFileFilter;
    private java.util.List<LinkgrabberFilterRuleWrapper> denyUrlFilter;
    private java.util.List<LinkgrabberFilterRuleWrapper> acceptUrlFilter;

    private ChangeEventSender                            eventSender;
    private boolean                                      testInstance = false;

    /**
     * Create a new instance of LinkFilterController. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    public LinkFilterController(boolean testInstance) {
        eventSender = new ChangeEventSender();
        this.testInstance = testInstance;
        if (isTestInstance() == false) {
            config = JsonConfig.create(LinkFilterSettings.class);
            filter = config.getFilterList();
            if (filter == null) filter = new ArrayList<LinkgrabberFilterRule>();
            ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

                @Override
                public void onShutdown(final Object shutdownRequest) {
                    synchronized (LinkFilterController.this) {
                        if (config != null) config.setFilterList(filter);
                    }
                }

                @Override
                public String toString() {
                    return "save filters...";
                }
            });
            updateInternal();
        } else {
            filter = new ArrayList<LinkgrabberFilterRule>();
        }
    }

    public ChangeEventSender getEventSender() {
        return eventSender;
    }

    private void updateInternal() {
        // url filter only require the urls, and thus can be done
        // brefore
        // linkcheck
        ArrayList<LinkgrabberFilterRuleWrapper> newdenyUrlFilter = new ArrayList<LinkgrabberFilterRuleWrapper>();
        ArrayList<LinkgrabberFilterRuleWrapper> newacceptUrlFilter = new ArrayList<LinkgrabberFilterRuleWrapper>();

        // FIlefilters require the full file information available after
        // linkcheck
        ArrayList<LinkgrabberFilterRuleWrapper> newdenyFileFilter = new ArrayList<LinkgrabberFilterRuleWrapper>();
        ArrayList<LinkgrabberFilterRuleWrapper> newacceptFileFilter = new ArrayList<LinkgrabberFilterRuleWrapper>();

        for (LinkgrabberFilterRule lgr : filter) {
            if (lgr.isEnabled() && lgr.isValid()) {

                LinkgrabberFilterRuleWrapper compiled = lgr.compile();
                if (lgr.isAccept()) {
                    if (!compiled.isRequiresLinkcheck()) {
                        newacceptUrlFilter.add(compiled);
                    } else {
                        newacceptFileFilter.add(compiled);
                    }
                } else {
                    if (!compiled.isRequiresLinkcheck()) {
                        newdenyUrlFilter.add(compiled);
                    } else {
                        newdenyFileFilter.add(compiled);
                    }
                }
            }
        }

        newdenyUrlFilter.trimToSize();

        denyUrlFilter = newdenyUrlFilter;

        newacceptUrlFilter.trimToSize();

        acceptUrlFilter = newacceptUrlFilter;

        newdenyFileFilter.trimToSize();

        denyFileFilter = newdenyFileFilter;

        newacceptFileFilter.trimToSize();

        acceptFileFilter = newacceptFileFilter;
        getEventSender().fireEvent(new ChangeEvent(LinkFilterController.this));
    }

    public void update() {
        if (isTestInstance()) {
            updateInternal();
        } else {
            IOEQ.add(new Runnable() {

                public void run() {
                    updateInternal();
                }

            }, true);
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
        if (all == null) return;
        synchronized (this) {
            filter.addAll(all);
            if (config != null) config.setFilterList(filter);
            update();
        }
    }

    public void add(LinkgrabberFilterRule linkFilter) {
        if (linkFilter == null) return;
        synchronized (this) {
            filter.add(linkFilter);
            if (config != null) config.setFilterList(filter);
            update();
        }
    }

    public void remove(LinkgrabberFilterRule lf) {
        if (lf == null) return;
        synchronized (this) {
            filter.remove(lf);
            if (config != null) config.setFilterList(filter);
            update();
        }
    }

    public boolean dropByUrl(CrawledLink link) {
        if (link.getMatchingFilter() != null) {
            /*
             * links with set matching filtered are allowed, user wants to add them
             */
            return false;
        }
        if (isTestInstance() == false && !org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINK_FILTER_ENABLED.getValue()) return false;
        boolean matches = false;
        LinkgrabberFilterRule matchingFilter = null;
        final java.util.List<LinkgrabberFilterRuleWrapper> localdenyUrlFilter = denyUrlFilter;
        final java.util.List<LinkgrabberFilterRuleWrapper> localacceptUrlFilter = acceptUrlFilter;
        final java.util.List<LinkgrabberFilterRuleWrapper> localacceptFileFilter = acceptFileFilter;
        if (localdenyUrlFilter.size() > 0) {

            for (LinkgrabberFilterRuleWrapper lgr : localdenyUrlFilter) {
                try {
                    if (!lgr.checkHoster(link)) continue;
                } catch (NoDownloadLinkException e) {
                    continue;
                }
                try {
                    if (!lgr.checkPluginStatus(link)) continue;
                } catch (NoDownloadLinkException e) {
                    continue;
                }
                if (!lgr.checkSource(link)) continue;
                matches = true;
                matchingFilter = lgr.getRule();

            }

        }
        // no deny filter match. We can return here
        if (!matches) {
            // System.out.println("false " + link);
            return false;
        }

        // now check if we have an accept filter for this link.
        for (LinkgrabberFilterRuleWrapper lgr : localacceptUrlFilter) {

            try {
                if (!lgr.checkHoster(link)) continue;
            } catch (NoDownloadLinkException e) {

                return false;
            }

            try {
                if (!lgr.checkPluginStatus(link)) continue;
            } catch (NoDownloadLinkException e) {
                return false;
            }
            if (!lgr.checkSource(link)) continue;
            link.setMatchingFilter(lgr.getRule());
            return false;

        }
        for (LinkgrabberFilterRuleWrapper lgr : localacceptFileFilter) {
            try {
                if (!lgr.checkHoster(link)) continue;
            } catch (NoDownloadLinkException e) {
                return false;
            }

            try {
                if (!lgr.checkPluginStatus(link)) continue;
            } catch (NoDownloadLinkException e) {
                return false;
            }
            if (!lgr.checkSource(link)) continue;
            link.setMatchingFilter(lgr.getRule());
            return false;
        }
        // System.out.println("true " + link);
        link.setMatchingFilter(matchingFilter);
        return true;
    }

    public boolean dropByFileProperties(CrawledLink link) {
        if (link.getMatchingFilter() != null && link.getMatchingFilter() instanceof LinkgrabberFilterRule && !((LinkgrabberFilterRule) link.getMatchingFilter()).isAccept()) {
            /*
             * links with set matching filtered are allowed, user wants to add them
             */
            return false;
        }
        if (isTestInstance() == false && !org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINK_FILTER_ENABLED.getValue()) return false;
        DownloadLink dlink = link.getDownloadLink();
        if (dlink == null) { throw new WTFException(); }
        boolean matches = false;
        LinkgrabberFilterRule matchedFilter = null;
        final java.util.List<LinkgrabberFilterRuleWrapper> localdenyUrlFilter = denyUrlFilter;
        final java.util.List<LinkgrabberFilterRuleWrapper> localacceptUrlFilter = acceptUrlFilter;
        final java.util.List<LinkgrabberFilterRuleWrapper> localacceptFileFilter = acceptFileFilter;
        final java.util.List<LinkgrabberFilterRuleWrapper> localdenyFileFilter = denyFileFilter;
        if (localdenyFileFilter.size() > 0 || localdenyUrlFilter.size() > 0) {

            for (LinkgrabberFilterRuleWrapper lgr : localdenyFileFilter) {

                try {
                    if (!lgr.checkHoster(link)) continue;
                } catch (NoDownloadLinkException e) {
                    throw new WTFException();
                }
                try {
                    if (!lgr.checkPluginStatus(link)) continue;
                } catch (NoDownloadLinkException e) {
                    throw new WTFException();
                }
                if (!lgr.checkSource(link)) continue;
                if (!lgr.checkOnlineStatus(link)) continue;

                if (!lgr.checkFileName(link)) continue;
                if (!lgr.checkFileSize(link)) continue;
                if (!lgr.checkFileType(link)) continue;

                matches = true;
                matchedFilter = lgr.getRule();
                break;
            }
            if (!matches) {
                for (LinkgrabberFilterRuleWrapper lgr : localdenyUrlFilter) {
                    try {
                        if (!lgr.checkHoster(link)) continue;
                    } catch (NoDownloadLinkException e) {
                        throw new WTFException();

                    }
                    try {
                        if (!lgr.checkPluginStatus(link)) continue;
                    } catch (NoDownloadLinkException e) {
                        throw new WTFException();
                    }
                    if (!lgr.checkSource(link)) continue;
                    matches = true;
                    matchedFilter = lgr.getRule();

                }
            }
        }
        if (!matches) return false;

        // now check if we have an accept filter for this link.
        for (LinkgrabberFilterRuleWrapper lgr : localacceptUrlFilter) {

            try {
                if (!lgr.checkHoster(link)) continue;
            } catch (NoDownloadLinkException e) {
                e.printStackTrace();
                throw new WTFException();
            }
            try {
                if (!lgr.checkPluginStatus(link)) continue;
            } catch (NoDownloadLinkException e) {
                throw new WTFException();
            }
            if (!lgr.checkSource(link)) continue;
            link.setMatchingFilter(lgr.getRule());
            return false;

        }
        for (LinkgrabberFilterRuleWrapper lgr : localacceptFileFilter) {
            try {
                if (!lgr.checkHoster(link)) continue;
            } catch (NoDownloadLinkException e) {

                throw new WTFException();
            }
            try {
                if (!lgr.checkPluginStatus(link)) continue;
            } catch (NoDownloadLinkException e) {
                throw new WTFException();
            }
            if (!lgr.checkSource(link)) continue;
            if (!lgr.checkOnlineStatus(link)) continue;

            if (!lgr.checkFileName(link)) continue;
            if (!lgr.checkFileSize(link)) continue;
            if (!lgr.checkFileType(link)) continue;

            link.setMatchingFilter(lgr.getRule());
            return false;
        }

        link.setMatchingFilter(matchedFilter);
        return true;
    }

    public java.util.List<LinkgrabberFilterRule> listFilters() {
        synchronized (this) {
            java.util.List<LinkgrabberFilterRule> lst = filter;
            java.util.List<LinkgrabberFilterRule> ret = new ArrayList<LinkgrabberFilterRule>();
            for (LinkgrabberFilterRule l : lst) {
                if (!l.isAccept()) ret.add(l);
            }
            return ret;
        }
    }

    public java.util.List<LinkgrabberFilterRule> listExceptions() {
        synchronized (this) {
            java.util.List<LinkgrabberFilterRule> lst = filter;
            java.util.List<LinkgrabberFilterRule> ret = new ArrayList<LinkgrabberFilterRule>();
            for (LinkgrabberFilterRule l : lst) {
                if (l.isAccept()) ret.add(l);
            }
            return ret;
        }
    }
}
