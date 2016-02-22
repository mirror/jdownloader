package org.jdownloader.gui.views.downloads.properties;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JPopupMenu;

import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.properties.AbstractPanelHeader;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class DownloadPropertiesHeader extends AbstractPanelHeader {

    protected AbstractNode              current;
    private DownloadPropertiesBasePanel card;

    public DownloadPropertiesHeader(DownloadPropertiesBasePanel loverView) {
        super("", NewTheme.I().getIcon(IconKey.ICON_DOWNLOAD, 16));
        this.card = loverView;
        // setBackground(Color.RED);
        // setOpaque(true);

        // LinkCollector.getInstance().getEventsender().addListener(this, true);

    }

    protected void onCloseAction() {
    }

    public void update(final AbstractNode objectbyRow) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                current = objectbyRow;
                String str = "";
                if (objectbyRow instanceof FilePackage) {
                    final FilePackage pkg = (FilePackage) objectbyRow;
                    setIcon(NewTheme.I().getIcon(IconKey.ICON_PACKAGE_OPEN, 16));
                    str = (_GUI.T.LinkgrabberPropertiesHeader_update_package(pkg.getName()));
                } else if (objectbyRow instanceof DownloadLink) {
                    final DownloadLink link = (DownloadLink) objectbyRow;
                    setIcon(link.getLinkInfo().getIcon());
                    str = (_GUI.T.LinkgrabberPropertiesHeader_update_link(link.getView().getDisplayName()));
                }
                setText(str);

            }
        };

    }
    //
    // @Override
    // public void onLinkCollectorAbort(LinkCollectorEvent event) {
    // }
    //
    // @Override
    // public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
    // }
    //
    // @Override
    // public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
    // }
    //
    // @Override
    // public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
    // if (event.getParameter() == current) {
    // update(current);
    // }
    //
    // }
    //
    // @Override
    // public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
    // }
    //
    // @Override
    // public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
    // }
    //
    // @Override
    // public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
    // }
    //
    // @Override
    // public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
    // }
    //
    // @Override
    // public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
    // }
    //
    // @Override
    // public void onLinkCrawlerAdded(LinkCollectorCrawler parameter) {
    // }
    //
    // @Override
    // public void onLinkCrawlerStarted(LinkCollectorCrawler parameter) {
    // }
    //
    // @Override
    // public void onLinkCrawlerStopped(LinkCollectorCrawler parameter) {
    // }

    @Override
    protected void onSettings(ExtButton options) {
        JPopupMenu pu = new JPopupMenu();
        card.fillPopup(pu);

        Insets insets = LAFOptions.getInstance().getExtension().customizePopupBorderInsets();

        Dimension pref = pu.getPreferredSize();
        // pref.width = positionComp.getWidth() + ((Component)
        // e.getSource()).getWidth() + insets[1] + insets[3];
        // pu.setPreferredSize(new Dimension(optionsgetWidth() + insets[1] + insets[3], (int) pref.getHeight()));

        pu.show(options, -insets.left, -pu.getPreferredSize().height + insets.bottom);
    }

    // @Override
    // public void onLinkCrawlerNewJob(LinkCollectingJob job) {
    // }
}
