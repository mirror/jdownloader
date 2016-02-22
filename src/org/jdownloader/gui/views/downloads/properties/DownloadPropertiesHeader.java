package org.jdownloader.gui.views.downloads.properties;

import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JPopupMenu;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.properties.AbstractPanelHeader;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

public class DownloadPropertiesHeader extends AbstractPanelHeader {

    private final DownloadPropertiesBasePanel card;
    private final Icon                        packageIcon = new AbstractIcon(IconKey.ICON_PACKAGE_OPEN, 16);

    public DownloadPropertiesHeader(DownloadPropertiesBasePanel loverView) {
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
                if (objectbyRow instanceof FilePackage) {
                    final FilePackage pkg = (FilePackage) objectbyRow;
                    setIcon(packageIcon);
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

    @Override
    protected void onSettings(ExtButton options) {
        final JPopupMenu pu = new JPopupMenu();
        card.fillPopup(pu);
        final Insets insets = LAFOptions.getInstance().getExtension().customizePopupBorderInsets();
        pu.show(options, -insets.left, -pu.getPreferredSize().height + insets.bottom);
    }

}
