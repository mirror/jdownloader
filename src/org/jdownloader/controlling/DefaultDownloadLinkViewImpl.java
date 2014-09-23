package org.jdownloader.controlling;

import jd.plugins.DownloadLink;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.settings.UrlDisplayType;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class DefaultDownloadLinkViewImpl implements DownloadLinkView {
    private static class ChangeListener implements GenericConfigEventListener<Enum<?>[]> {
        private boolean enabled = true;

        public void update() {
            if (!enabled) {
                return;
            }
            UrlDisplayType[] newOrder = CFG_GENERAL.CFG.getUrlDisplayOrder();
            if (newOrder == null || newOrder.length == 0) {
                newOrder = new UrlDisplayType[] { UrlDisplayType.REFERRER, UrlDisplayType.ORIGIN, UrlDisplayType.CONTAINER, UrlDisplayType.CONTENT };
                enabled = false;
                try {
                    CFG_GENERAL.CFG.setUrlDisplayOrder(newOrder);
                } finally {
                    enabled = true;
                }

            }
            DISPLAY_URL_TYPE = newOrder;
        }

        @Override
        public void onConfigValueModified(KeyHandler<Enum<?>[]> keyHandler, Enum<?>[] newValue) {
            update();
        }

        @Override
        public void onConfigValidatorError(KeyHandler<Enum<?>[]> keyHandler, Enum<?>[] invalidValue, ValidationException validateException) {
        }
    };

    public static UrlDisplayType[] DISPLAY_URL_TYPE = null;
    private static ChangeListener  CHANGELISTENER;
    static {

        CHANGELISTENER = new ChangeListener();
        CFG_GENERAL.URL_DISPLAY_ORDER.getEventSender().addListener(CHANGELISTENER);
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
