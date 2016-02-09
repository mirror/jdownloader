package org.jdownloader.gui.toolbar;

import java.awt.event.ActionEvent;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MergeToPackageAction;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.JDGui.Panels;
import jd.gui.swing.jdgui.interfaces.View;

public class MoveToNewFolderToolbarAction extends AbstractToolBarAction {

    private MergeToPackageAction setupDelegate;

    public MoveToNewFolderToolbarAction() {
        setName(_GUI.T.MergeToPackageAction_MergeToPackageAction_());
        setIconKey(IconKey.ICON_PACKAGE_NEW);
        setupDelegate = new MergeToPackageAction();
        addContextSetup(setupDelegate);

    }

    @Override
    public void onGuiMainTabSwitch(View oldView, View newView) {
        super.onGuiMainTabSwitch(oldView, newView);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (JDGui.getInstance().isCurrentPanel(Panels.LINKGRABBER)) {
            MergeToPackageAction action = new MergeToPackageAction();
            action.setExpandNewPackage(setupDelegate.isExpandNewPackage());
            action.setLastPathDefault(setupDelegate.isLastPathDefault());
            action.setLocation(setupDelegate.getLocation());
            action.actionPerformed(event);

        } else if (JDGui.getInstance().isCurrentPanel(Panels.DOWNLOADLIST)) {
            org.jdownloader.gui.views.downloads.action.MergeToPackageAction action = new org.jdownloader.gui.views.downloads.action.MergeToPackageAction();
            action.setExpandNewPackage(setupDelegate.isExpandNewPackage());
            action.setLastPathDefault(setupDelegate.isLastPathDefault());
            action.setLocation(setupDelegate.getLocation());
            action.actionPerformed(event);

        }
    }

    @Override
    protected String createTooltip() {
        return null;
    }
}
