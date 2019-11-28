package org.jdownloader.gui.toolbar;

import java.awt.event.ActionEvent;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.JDGui.Panels;
import jd.gui.swing.jdgui.interfaces.View;

import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.LocationInList;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MergeToPackageAction;
import org.jdownloader.translate._JDT;

public class MoveToNewFolderToolbarAction extends AbstractToolBarAction {
    public MoveToNewFolderToolbarAction() {
        setName(_GUI.T.MergeToPackageAction_MergeToPackageAction_());
        setIconKey(IconKey.ICON_PACKAGE_NEW);
    }

    @Override
    public void onGuiMainTabSwitch(View oldView, View newView) {
        super.onGuiMainTabSwitch(oldView, newView);
    }

    private boolean expandNewPackage = false;

    public static String getTranslationForExpandNewPackage() {
        return _JDT.T.MergeToPackageAction_getTranslationForExpandNewPackage();
    }

    @Customizer(link = "#getTranslationForExpandNewPackage")
    public boolean isExpandNewPackage() {
        return expandNewPackage;
    }

    public void setExpandNewPackage(boolean expandNewPackage) {
        this.expandNewPackage = expandNewPackage;
    }

    private boolean lastPathDefault = false;

    public static String getTranslationForLastPathDefault() {
        return _JDT.T.MergeToPackageAction_getTranslationForLastPathDefault();
    }

    @Customizer(link = "#getTranslationForLastPathDefault")
    public boolean isLastPathDefault() {
        return lastPathDefault;
    }

    public void setLastPathDefault(boolean lastPathDefault) {
        this.lastPathDefault = lastPathDefault;
    }

    private LocationInList location = LocationInList.END_OF_LIST;

    public static String getTranslationForLocation() {
        return _JDT.T.MergeToPackageAction_getTranslationForLocation();
    }

    @Customizer(link = "#getTranslationForLocation")
    public LocationInList getLocation() {
        return location;
    }

    public void setLocation(LocationInList location) {
        this.location = location;
    }

    @Override
    public void addContextSetup(ActionContext contextSetup) {
        super.addContextSetup(contextSetup);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (JDGui.getInstance().isCurrentPanel(Panels.LINKGRABBER)) {
            final MergeToPackageAction action = new MergeToPackageAction();
            action.setExpandNewPackage(isExpandNewPackage());
            action.setLastPathDefault(isLastPathDefault());
            action.setLocation(getLocation());
            action.actionPerformed(event);
        } else if (JDGui.getInstance().isCurrentPanel(Panels.DOWNLOADLIST)) {
            final org.jdownloader.gui.views.downloads.action.MergeToPackageAction action = new org.jdownloader.gui.views.downloads.action.MergeToPackageAction();
            action.setExpandNewPackage(isExpandNewPackage());
            action.setLastPathDefault(isLastPathDefault());
            action.setLocation(getLocation());
            action.actionPerformed(event);
        }
    }

    @Override
    protected String createTooltip() {
        return null;
    }
}
