package org.jdownloader.controlling;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.plugins.DownloadLink;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.UrlDisplayEntry;
import org.jdownloader.settings.UrlDisplayType;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class DefaultDownloadLinkViewImpl implements DownloadLinkView {
    private static class ChangeListener implements GenericConfigEventListener<Object> {

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
                            LogController.getInstance().getLogger(DefaultDownloadLinkViewImpl.class.getName()).log(e1);
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

    public static UrlDisplayType[]      DISPLAY_URL_TYPE = null;
    private static final ChangeListener CHANGELISTENER   = new ChangeListener();
    static {
        CFG_GENERAL.URL_ORDER.getEventSender().addListener(CHANGELISTENER);
        CHANGELISTENER.update();
    }
    protected DownloadLink              link;

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
        // http://board.jdownloader.org/showpost.php?p=305216&postcount=12
        for (final UrlDisplayType dt : DISPLAY_URL_TYPE) {
            if (dt != null) {
                final String ret = LinkTreeUtils.getUrlByType(dt, link);
                if (StringUtils.isNotEmpty(ret)) {
                    return ret;
                }
            }
        }
        return null;

    }

}
