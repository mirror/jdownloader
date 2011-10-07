package org.jdownloader.controlling.filter;

import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawlerFilter;
import jd.plugins.DownloadLink;

import org.appwork.exceptions.WTFException;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Files;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeEventSender;
import org.jdownloader.translate._JDT;

public class LinkFilterController implements LinkCrawlerFilter {
    private static final LinkFilterController INSTANCE          = new LinkFilterController();
    public static final LinkgrabberFilterRule VIRTUAL_DENY_RULE = new LinkgrabberFilterRule();
    static {
        VIRTUAL_DENY_RULE.setName(_JDT._.LinkFilterController_deny_all_());
    }

    /**
     * get the only existing instance of LinkFilterController. This is a
     * singleton
     * 
     * @return
     */
    public static LinkFilterController getInstance() {
        return LinkFilterController.INSTANCE;
    }

    private ArrayList<LinkgrabberFilterRule>        filter;
    private LinkFilterSettings                      config;
    private ArrayList<LinkgrabberFilterRuleWrapper> denyFileFilter;
    private ArrayList<LinkgrabberFilterRuleWrapper> acceptFileFilter;
    private ArrayList<LinkgrabberFilterRuleWrapper> denyUrlFilter;
    private ArrayList<LinkgrabberFilterRuleWrapper> acceptUrlFilter;
    private ChangeEventSender                       eventSender;

    /**
     * Create a new instance of LinkFilterController. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private LinkFilterController() {
        config = JsonConfig.create(LinkFilterSettings.class);
        filter = config.getFilterList();
        eventSender = new ChangeEventSender();
        if (filter == null) filter = new ArrayList<LinkgrabberFilterRule>();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                synchronized (LinkFilterController.this) {
                    config.setFilterList(filter);
                }
            }

            @Override
            public String toString() {
                return "save filters...";
            }
        });

        update();
    }

    public ChangeEventSender getEventSender() {
        return eventSender;
    }

    public void update() {
        synchronized (this) {

            // url filter only require the urls, and thus can be done brefore
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
                            acceptUrlFilter.add(compiled);
                        } else {
                            acceptFileFilter.add(compiled);
                        }
                    } else {
                        if (!compiled.isRequiresLinkcheck()) {
                            denyUrlFilter.add(compiled);
                        } else {
                            denyFileFilter.add(compiled);
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
        }
    }

    public ArrayList<LinkgrabberFilterRule> list() {
        synchronized (this) {
            return new ArrayList<LinkgrabberFilterRule>(filter);
        }
    }

    public void addAll(ArrayList<LinkgrabberFilterRule> all) {
        if (all == null) return;
        synchronized (this) {
            filter.addAll(all);
            config.setFilterList(filter);
            update();
        }
        getEventSender().fireEvent(new ChangeEvent(this));
    }

    public void add(LinkgrabberFilterRule linkFilter) {
        if (linkFilter == null) return;
        synchronized (this) {
            filter.add(linkFilter);
            config.setFilterList(filter);
            update();
        }
        getEventSender().fireEvent(new ChangeEvent(this));

    }

    public void remove(LinkgrabberFilterRule lf) {
        if (lf == null) return;
        synchronized (this) {
            filter.remove(lf);
            config.setFilterList(filter);
            update();
        }
        getEventSender().fireEvent(new ChangeEvent(this));
    }

    public boolean dropByUrl(CrawledLink link) {
        if (link.getMatchingFilter() != null) {
            /*
             * links with set matching filtered are allowed, user wants to add
             * them
             */
            return false;
        }
        if (!LinkFilterSettings.LINK_FILTER_ENABLED.getValue()) return false;
        boolean matches = false;
        LinkgrabberFilterRule matchingFilter = null;
        final ArrayList<LinkgrabberFilterRuleWrapper> localdenyUrlFilter = denyUrlFilter;
        final ArrayList<LinkgrabberFilterRuleWrapper> localacceptUrlFilter = acceptUrlFilter;
        final ArrayList<LinkgrabberFilterRuleWrapper> localacceptFileFilter = acceptFileFilter;
        if (localdenyUrlFilter.size() > 0) {

            for (LinkgrabberFilterRuleWrapper lgr : localdenyUrlFilter) {
                try {
                    if (!checkHoster(lgr, link)) continue;
                } catch (NoDownloadLinkException e) {
                    continue;
                }
                if (!checkSource(lgr, link)) continue;
                matches = true;
                matchingFilter = lgr.getRule();

            }

        } else {
            // no filter
            if (localacceptFileFilter.size() > 0 || localacceptUrlFilter.size() > 0) {
                // no deny filter, but accept filter. does not make any sense.
                // add a virtual deny all filter if we have accept filters
                matches = true;
                matchingFilter = VIRTUAL_DENY_RULE;
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
                if (!checkHoster(lgr, link)) continue;
            } catch (NoDownloadLinkException e) {

                return false;
            }
            if (!checkSource(lgr, link)) continue;

            return false;

        }
        for (LinkgrabberFilterRuleWrapper lgr : localacceptFileFilter) {
            try {
                if (!checkHoster(lgr, link)) continue;
            } catch (NoDownloadLinkException e) {
                return false;
            }
            if (!checkSource(lgr, link)) continue;

            return false;
        }
        // System.out.println("true " + link);
        link.setMatchingFilter(matchingFilter);
        return true;
    }

    public boolean dropByFileProperties(CrawledLink link) {
        if (link.getMatchingFilter() != null) {
            /*
             * links with set matching filtered are allowed, user wants to add
             * them
             */
            return false;
        }
        if (!LinkFilterSettings.LINK_FILTER_ENABLED.getValue()) return false;
        DownloadLink dlink = link.getDownloadLink();
        if (dlink == null) { throw new WTFException(); }
        boolean matches = false;
        LinkgrabberFilterRule matchedFilter = null;
        final ArrayList<LinkgrabberFilterRuleWrapper> localdenyUrlFilter = denyUrlFilter;
        final ArrayList<LinkgrabberFilterRuleWrapper> localacceptUrlFilter = acceptUrlFilter;
        final ArrayList<LinkgrabberFilterRuleWrapper> localacceptFileFilter = acceptFileFilter;
        final ArrayList<LinkgrabberFilterRuleWrapper> localdenyFileFilter = denyFileFilter;
        if (localdenyFileFilter.size() > 0 || localdenyUrlFilter.size() > 0) {

            for (LinkgrabberFilterRuleWrapper lgr : localdenyFileFilter) {

                try {
                    if (!checkHoster(lgr, link)) continue;
                } catch (NoDownloadLinkException e) {
                    throw new WTFException();
                }
                if (!checkSource(lgr, link)) continue;
                if (!checkFileName(lgr, link)) continue;
                if (!checkFileSize(lgr, link)) continue;
                if (!checkFileType(lgr, link)) continue;
                matches = true;
                matchedFilter = lgr.getRule();
                break;
            }
            if (!matches) {
                for (LinkgrabberFilterRuleWrapper lgr : localdenyUrlFilter) {
                    try {
                        if (!checkHoster(lgr, link)) continue;
                    } catch (NoDownloadLinkException e) {
                        throw new WTFException();

                    }
                    if (!checkSource(lgr, link)) continue;
                    matches = true;
                    matchedFilter = lgr.getRule();

                }
            }
        } else {

            // no deny filter...add a virtual MATCH ALL one
            if (localacceptFileFilter.size() > 0 || localacceptUrlFilter.size() > 0) {
                // no deny filter, but accept filter. does not make any sense.
                // add a virtual deny all filter if we have accept filters
                matches = true;
                matchedFilter = VIRTUAL_DENY_RULE;
            }
        }
        if (!matches) return false;
        System.out.println("ACCEPT?");
        // now check if we have an accept filter for this link.
        for (LinkgrabberFilterRuleWrapper lgr : localacceptUrlFilter) {

            try {
                if (!checkHoster(lgr, link)) continue;
            } catch (NoDownloadLinkException e) {
                e.printStackTrace();
                throw new WTFException();
            }
            if (!checkSource(lgr, link)) continue;

            return false;

        }
        for (LinkgrabberFilterRuleWrapper lgr : localacceptFileFilter) {
            try {
                if (!checkHoster(lgr, link)) continue;
            } catch (NoDownloadLinkException e) {

                throw new WTFException();
            }
            if (!checkSource(lgr, link)) continue;
            if (!checkFileName(lgr, link)) continue;
            if (!checkFileSize(lgr, link)) continue;
            if (!checkFileType(lgr, link)) continue;
            System.out.println(1);
            return false;
        }

        link.setMatchingFilter(matchedFilter);
        return true;
    }

    private boolean checkFileType(LinkgrabberFilterRuleWrapper lgr, CrawledLink link) {
        if (lgr.getFiletypeFilter() != null) {
            String ext = Files.getExtension(link.getDownloadLink().getName());
            if (ext == null) return true;
            return lgr.getFiletypeFilter().matches(ext);
        }
        return true;
    }

    private boolean checkFileSize(LinkgrabberFilterRuleWrapper lgr, CrawledLink link) {
        if (lgr.getFilesizeRule() != null) {
            // if (link.getDownloadLink().getDownloadSize() <= 0) return true;
            return lgr.getFilesizeRule().matches(link.getDownloadLink().getDownloadSize());
        }
        return true;
    }

    private boolean checkFileName(LinkgrabberFilterRuleWrapper lgr, CrawledLink link) {
        if (lgr.getFileNameRule() != null) { return lgr.getFileNameRule().matches(link.getDownloadLink().getName()); }
        return true;
    }

    private boolean checkHoster(LinkgrabberFilterRuleWrapper lgr, CrawledLink link) throws NoDownloadLinkException {
        if (lgr.getHosterRule() != null) {
            if (link.getDownloadLink() == null) { throw new NoDownloadLinkException(); }
            return lgr.getHosterRule().matches(link.getDownloadLink().getDownloadURL());
        }
        return true;
    }

    private boolean checkSource(LinkgrabberFilterRuleWrapper lgr, CrawledLink link) {
        CrawledLink p = link;
        if (lgr.getSourceRule() != null) {
            do {
                if (lgr.getSourceRule().matches(p.getURL())) { return true; }
            } while ((p = p.getParentLink()) != null);
            return false;
        }
        return true;
    }

}
