package org.jdownloader.gui.views.linkgrabber.properties;

import java.awt.Insets;
import java.lang.ref.WeakReference;

import javax.swing.Icon;
import javax.swing.JPopupMenu;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkProperty;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackageProperty;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

public class LinkgrabberPropertiesHeader extends AbstractPanelHeader implements LinkCollectorListener {
    private final LinkgrabberProperties          card;
    private final Icon                           packageIcon   = new AbstractIcon(IconKey.ICON_PACKAGE_OPEN, 16);
    private volatile WeakReference<AbstractNode> nodeReference = null;

    public LinkgrabberPropertiesHeader(LinkgrabberProperties loverView) {
        super("", NewTheme.I().getIcon(IconKey.ICON_DOWNLOAD, 16));
        this.card = loverView;
    }

    protected void onCloseAction() {
    }

    public void update(final AbstractNode objectbyRow) {
        if (objectbyRow != null) {
            nodeReference = new WeakReference<AbstractNode>(objectbyRow);
            LinkCollector.getInstance().getEventsender().addListener(this, true);
        } else {
            nodeReference = null;
            LinkCollector.getInstance().getEventsender().removeListener(this);
        }
        setTitle(objectbyRow);
    }

    protected void setTitle(final AbstractNode objectbyRow) {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                final String str;
                if (objectbyRow instanceof CrawledPackage) {
                    final CrawledPackage pkg = (CrawledPackage) objectbyRow;
                    setIcon(packageIcon);
                    str = (_GUI.T.LinkgrabberPropertiesHeader_update_package(pkg.getName()));
                } else if (objectbyRow instanceof CrawledLink) {
                    final CrawledLink link = (CrawledLink) objectbyRow;
                    setIcon(link.getLinkInfo().getIcon());
                    str = (_GUI.T.LinkgrabberPropertiesHeader_update_link(link.getName()));
                } else {
                    str = "";
                }
                setText(str);
            }
        };
    }

    @Override
    protected void onSettings(ExtButton options) {
        final JPopupMenu pu = new JPopupMenu();
        card.fillPopup(pu);
        final Insets insets = LAFOptions.getInstance().getExtension().customizePopupBorderInsets();
        pu.show(options, -insets.left, -pu.getPreferredSize().height + insets.bottom);
    }

    @Override
    public void onLinkCollectorAbort(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
        final WeakReference<AbstractNode> nodeReference = this.nodeReference;
        AbstractNode node = null;
        if (LinkCollectorEvent.TYPE.REFRESH_DATA == event.getType() && event.getParameter(0) == (node = nodeReference.get())) {
            final Object param = event.getParameter(1);
            if (param instanceof CrawledLinkProperty) {
                final CrawledLinkProperty eventPropery = (CrawledLinkProperty) param;
                switch (eventPropery.getProperty()) {
                case NAME:
                    setTitle(node);
                    break;
                default:
                    break;
                }
            } else if (param instanceof CrawledPackageProperty) {
                final CrawledPackageProperty eventPropery = (CrawledPackageProperty) param;
                switch (eventPropery.getProperty()) {
                case NAME:
                    setTitle(node);
                    break;
                default:
                    break;
                }
            }
        }
    }

    @Override
    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink link) {
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink link) {
    }

    @Override
    public void onLinkCrawlerAdded(LinkCollectorCrawler crawler) {
    }

    @Override
    public void onLinkCrawlerStarted(LinkCollectorCrawler crawler) {
    }

    @Override
    public void onLinkCrawlerStopped(LinkCollectorCrawler crawler) {
    }

    @Override
    public void onLinkCrawlerFinished() {
    }

    @Override
    public void onLinkCrawlerNewJob(LinkCollectingJob job) {
    }
}
