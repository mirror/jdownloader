package jd.plugins.components.gopro;

import java.awt.event.ActionEvent;

import org.appwork.uio.UIOManager;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcrawler.LinkCrawler;

public class SyncGoProLibraryToolbarAction extends AbstractToolBarAction {
    public SyncGoProLibraryToolbarAction() {
        setName("Sync GoPro Plus Library");
        setIconKey("fav/gopro.com");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        UIOManager.I().showConfirmDialog(UIOManager.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, "GoPro Plus Media Library Sync", "JDownloader will now search all available media files in your GoPro media library and add them to the Linkgrabber tab.\r\nPlease be patient - this will take a while. If you keep all links in the linkgrabber or downlist, the next sync process will be much faster.");
        LinkCrawler job = LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(LinkOrigin.TOOLBAR.getLinkOriginDetails(), "https://plus.gopro.com/media-library/"));
    }

    @Override
    protected String createTooltip() {
        return "Sync GoPro Plus Library";
    }
}
