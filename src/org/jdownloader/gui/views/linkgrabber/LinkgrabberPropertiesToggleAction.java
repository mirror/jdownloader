package org.jdownloader.gui.views.linkgrabber;

import java.awt.event.ActionEvent;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.bottombar.ToggleAppAction;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.JDGui;

public class LinkgrabberPropertiesToggleAction extends ToggleAppAction {

    public LinkgrabberPropertiesToggleAction() {
        super(CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE, _GUI.T.LinkgrabberPropertiesToggleAction_LinkgrabberPropertiesToggleAction(), _GUI.T.LinkgrabberPropertiesToggleAction_LinkgrabberPropertiesToggleAction());
        setIconKey(IconKey.ICON_BOTTOMBAR);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);

        AbstractNode selectedObject = LinkGrabberTable.getInstance().getModel().getObjectbyRow(LinkGrabberTable.getInstance().getSelectionModel().getLeadSelectionIndex());

        if (CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE.isEnabled() && selectedObject == null) {
            JDGui.help(_GUI.T.help_enable_table_properties_title(), _GUI.T.help_enable_table_properties_msg(), new AbstractIcon(IconKey.ICON_INFO, 32));

        }
    }
}
