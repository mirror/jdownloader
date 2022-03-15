package org.jdownloader.gui.views.downloads.properties;

import java.awt.Insets;
import java.lang.ref.WeakReference;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPopupMenu;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;

import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.download.DownloadControllerListener;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.properties.AbstractPanelHeader;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

public class DownloadPropertiesHeader extends AbstractPanelHeader implements DownloadControllerListener {
    private final DownloadPropertiesBasePanel    card;
    private final Icon                           packageIcon   = new AbstractIcon(IconKey.ICON_PACKAGE_OPEN, 16);
    private volatile WeakReference<AbstractNode> nodeReference = null;

    public DownloadPropertiesHeader(DownloadPropertiesBasePanel loverView) {
        super("", NewTheme.I().getIcon(IconKey.ICON_DOWNLOAD, 16));
        this.card = loverView;
    }

    protected void onCloseAction() {
    }

    public void update(final AbstractNode objectbyRow) {
        if (objectbyRow != null) {
            nodeReference = new WeakReference<AbstractNode>(objectbyRow);
            DownloadController.getInstance().getEventSender().addListener(this, true);
        } else {
            nodeReference = null;
            DownloadController.getInstance().getEventSender().removeListener(this);
        }
        setTitle(objectbyRow);
    }

    protected void setTitle(final AbstractNode objectbyRow) {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                final String str;
                if (objectbyRow instanceof FilePackage) {
                    final FilePackage pkg = (FilePackage) objectbyRow;
                    setIcon(packageIcon);
                    str = (_GUI.T.LinkgrabberPropertiesHeader_update_package(pkg.getName()));
                } else if (objectbyRow instanceof DownloadLink) {
                    final DownloadLink link = (DownloadLink) objectbyRow;
                    setIcon(link.getLinkInfo().getIcon());
                    str = (_GUI.T.LinkgrabberPropertiesHeader_update_link(link.getView().getDisplayName()));
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
    public void onDownloadControllerAddedPackage(FilePackage pkg) {
    }

    @Override
    public void onDownloadControllerStructureRefresh(FilePackage pkg) {
    }

    @Override
    public void onDownloadControllerStructureRefresh() {
    }

    @Override
    public void onDownloadControllerStructureRefresh(AbstractNode node, Object param) {
    }

    @Override
    public void onDownloadControllerRemovedPackage(FilePackage pkg) {
    }

    @Override
    public void onDownloadControllerRemovedLinklist(List<DownloadLink> list) {
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, DownloadLinkProperty property) {
        final WeakReference<AbstractNode> nodeReference = this.nodeReference;
        AbstractNode node = null;
        if (property != null && DownloadLinkProperty.Property.NAME.equals(property.getProperty()) && property.getDownloadLink() == (node = nodeReference.get())) {
            setTitle(node);
        }
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg, FilePackageProperty property) {
        final WeakReference<AbstractNode> nodeReference = this.nodeReference;
        AbstractNode node = null;
        if (property != null && FilePackageProperty.Property.NAME.equals(property.getProperty()) && property.getFilePackage() == (node = nodeReference.get())) {
            setTitle(node);
        }
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink) {
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg) {
    }
}
