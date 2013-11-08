package org.jdownloader.gui.views.downloads.bottombar;

import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JPopupMenu;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.action.MenuManagerAction;

public abstract class AbstractBottomBarMenuManager<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends ContextMenuManager<PackageType, ChildrenType> implements GUIListener {

    private CopyOnWriteArrayList<CustomizeableActionBar> links;

    @Override
    public void setMenuData(MenuContainerRoot root) {
        super.setMenuData(root);

        // no delayer here.

    }

    @Override
    public void onKeyModifier(int parameter) {
    }

    protected AbstractBottomBarMenuManager() {
        super();
        links = new CopyOnWriteArrayList<CustomizeableActionBar>();
        GUIEventSender.getInstance().addListener(this, true);

    }

    public JPopupMenu build(SelectionInfo<PackageType, ChildrenType> si) {
        throw new WTFException("Not Supported");

    }

    public boolean isAcceleratorsEnabled() {
        return true;
    }

    public void show() {

        new MenuManagerAction().actionPerformed(null);
    }

    @Override
    public void onGuiMainTabSwitch(View oldView, View newView) {

        // MainToolBar.getInstance().updateToolbar();
    }

    @Override
    protected void updateGui() {
        for (final CustomizeableActionBar link : links) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    link.updateGui();
                }
            };
        }
    }

    public void addLink(CustomizeableActionBar bottomBar) {
        links.add(bottomBar);
    }

}
