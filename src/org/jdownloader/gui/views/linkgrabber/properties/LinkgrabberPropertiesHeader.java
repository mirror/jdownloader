package org.jdownloader.gui.views.linkgrabber.properties;

import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JPopupMenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

public class LinkgrabberPropertiesHeader extends AbstractPanelHeader {

    private final LinkgrabberProperties card;
    private final Icon                  packageIcon = new AbstractIcon(IconKey.ICON_PACKAGE_OPEN, 16);

    public LinkgrabberPropertiesHeader(LinkgrabberProperties loverView) {
        super("", NewTheme.I().getIcon(IconKey.ICON_DOWNLOAD, 16));
        this.card = loverView;
    }

    protected void onCloseAction() {
    }

    public void update(final AbstractNode objectbyRow) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                String str = "";
                if (objectbyRow instanceof CrawledPackage) {
                    final CrawledPackage pkg = (CrawledPackage) objectbyRow;
                    setIcon(packageIcon);
                    str = (_GUI.T.LinkgrabberPropertiesHeader_update_package(pkg.getName()));
                } else if (objectbyRow instanceof CrawledLink) {
                    final CrawledLink link = (CrawledLink) objectbyRow;
                    setIcon(link.getLinkInfo().getIcon());
                    str = (_GUI.T.LinkgrabberPropertiesHeader_update_link(link.getName()));
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

}
