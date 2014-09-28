package org.jdownloader.controlling;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.plugins.DownloadLink;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.UrlDisplayEntry;
import org.jdownloader.settings.UrlDisplayType;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class DefaultDownloadLinkViewImpl implements DownloadLinkView {
    private static class ChangeListener implements GenericConfigEventListener<Object> {

        private LogSource logger = LogController.getInstance().getLogger(DefaultDownloadLinkViewImpl.class.getName());

        public void update() {

            List<UrlDisplayType> lst = new ArrayList<UrlDisplayType>();
            UrlDisplayEntry[] newOrder = CFG_GENERAL.CFG.getUrlOrder();
            //
            HashSet<String> dupe = new HashSet<String>();
            if (newOrder != null) {
                for (UrlDisplayEntry e : newOrder) {
                    if (e.isEnabled()) {
                        try {
                            if (dupe.add(e.getType())) {
                                lst.add(UrlDisplayType.valueOf(e.getType()));
                            }
                        } catch (Throwable e1) {
                            logger.log(e1);
                        }
                    }
                }

            } else {
                // restore old settings
                UrlDisplayType[] order = CFG_GENERAL.CFG.getUrlDisplayOrder();
                CFG_GENERAL.CFG.setUrlDisplayOrder(null);
                if (order != null) {
                    newOrder = new UrlDisplayEntry[UrlDisplayType.values().length];
                    int i = 0;
                    for (UrlDisplayType t : order) {
                        if (dupe.add(t.name())) {
                            lst.add(t);
                            newOrder[i++] = new UrlDisplayEntry(t.name(), true);
                        }
                    }
                    for (UrlDisplayType t : UrlDisplayType.values()) {
                        if (dupe.add(t.name())) {
                            lst.add(t);
                            newOrder[i++] = new UrlDisplayEntry(t.name(), false);
                        }
                    }
                    CFG_GENERAL.CFG.setUrlOrder(newOrder);

                }

            }
            for (UrlDisplayType t : UrlDisplayType.values()) {
                if (dupe.add(t.name())) {
                    lst.add(t);
                }
            }
            DISPLAY_URL_TYPE = lst.toArray(new UrlDisplayType[] {});
        }

        @Override
        public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
            update();
        }

        @Override
        public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
        }
    };

    public static UrlDisplayType[] DISPLAY_URL_TYPE = null;
    private static ChangeListener  CHANGELISTENER;
    static {

        CHANGELISTENER = new ChangeListener();
        CFG_GENERAL.URL_ORDER.getEventSender().addListener(CHANGELISTENER);
        CHANGELISTENER.update();

    }
    protected DownloadLink         link;

    public DefaultDownloadLinkViewImpl() {

    }

    public long getBytesLoaded() {
        return link.getDownloadCurrent();
    }

    public void setLink(DownloadLink downloadLink) {
        this.link = downloadLink;
    }

    /**
     * returns the estimated total filesize. This may not be accurate. sometimes this method even returns the same as
     * {@link #getBytesLoaded()} use {@link #getBytesTotal()} if you need a value that is either 0 or the
     */
    public long getBytesTotalEstimated() {
        return link.getDownloadSize();
    }

    public long getBytesTotal() {
        return link.getKnownDownloadSize();
    }

    public long getSpeedBps() {
        return link.getDownloadSpeed();
    }

    public long[] getChunksProgress() {
        return link.getChunksProgress();
    }

    @Override
    public long getBytesTotalVerified() {
        return link.getVerifiedFileSize();
    }

    @Override
    public long getDownloadTime() {
        return link.getDownloadTime();
    }

    @Override
    public String getDisplayName() {
        //
        return link.getName();
    }

    @Override
    public String getDisplayUrl() {
        if (1409057525234l - link.getCreated() > 0) {
            return link.getBrowserUrl();
        }
        // http://board.jdownloader.org/showpost.php?p=305216&postcount=12
        for (UrlDisplayType dt : DISPLAY_URL_TYPE) {
            if (dt == null) {
                continue;
            }
            String ret = getUrlByType(dt, link);
            if (ret != null) {
                return ret;
            }

        }

        return null;
        // if (CFG_GUI.SHOW_BROWSER_URL_IF_POSSIBLE) {
        //
        // switch (link.getUrlProtection()) {
        // case PROTECTED_CONTAINER:
        // case PROTECTED_DECRYPTER:
        // if (link.hasBrowserUrl()) {
        // return link.getBrowserUrl();
        // } else {
        // return null;
        // }
        //
        // default:
        // if (link.hasBrowserUrl()) {
        // return link.getBrowserUrl();
        // } else {
        // return link.getPluginPattern();
        //
        // }
        // }
        // }
        // Workaround for links added before this change

        // switch (link.getUrlProtection()) {
        // case PROTECTED_CONTAINER:
        // case PROTECTED_DECRYPTER:
        // if (link.hasBrowserUrl()) {
        // return link.getBrowserUrl();
        // } else {
        // return null;
        // }
        //
        // case PROTECTED_INTERNAL_URL:
        // if (link.hasBrowserUrl()) {
        // return link.getBrowserUrl();
        // }
        //
        // default:
        // return link.getPluginPattern();
        // }

    }

    public static String getUrlByType(UrlDisplayType dt, DownloadLink link) {

        switch (dt) {
        case REFERRER:
            return link.getReferrerUrl();

        case CONTAINER:
            return link.getContainerUrl();

        case ORIGIN:
            return link.getOriginUrl();

        case CONTENT:
            switch (link.getUrlProtection()) {
            case UNSET:
                if (link.getContentUrl() != null) {
                    return link.getContentUrl();
                }
                return link.getPluginPatternMatcher();
            }
        }
        return null;
    }

}
