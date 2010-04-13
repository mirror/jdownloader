//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.views.linkgrabber;

import javax.swing.Icon;

import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberControllerEvent;
import jd.controlling.LinkGrabberControllerListener;
import jd.gui.UserIF;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.interfaces.View;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class LinkgrabberView extends View {

    private static final long serialVersionUID = -8027069594232979742L;

    private static final String IDENT_PREFIX = "jd.gui.swing.jdgui.views.linkgrabberview.";

    private static LinkgrabberView INSTANCE = null;

    public static LinkgrabberView getInstance() {
        if (INSTANCE == null) INSTANCE = new LinkgrabberView();
        return INSTANCE;
    }

    private LinkgrabberView() {

        super();
        this.setContent(LinkGrabberPanel.getLinkGrabber());
        this.setDefaultInfoPanel(new LinkGrabberInfoPanel());

        LinkGrabberController.getInstance().addListener(new LinkGrabberControllerListener() {
            public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event) {
                switch (event.getID()) {
                case LinkGrabberControllerEvent.ADDED:
                    JDGui.getInstance().requestPanel(UserIF.Panels.DOWNLOADLIST, null);
                    break;
                }
            }
        });
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II("gui.images.taskpanes.linkgrabber", ICON_SIZE, ICON_SIZE);
    }

    @Override
    public String getTitle() {
        return JDL.L(IDENT_PREFIX + "tab.title", "Linkgrabber");
    }

    @Override
    public String getTooltip() {
        return JDL.L(IDENT_PREFIX + "tab.tooltip", "Collect, add and select links and URLs");
    }

    @Override
    protected void onHide() {
        ActionController.getToolBarAction("action.downloadview.movetotop").setEnabled(false);
        ActionController.getToolBarAction("action.downloadview.moveup").setEnabled(false);
        ActionController.getToolBarAction("action.downloadview.movedown").setEnabled(false);
        ActionController.getToolBarAction("action.downloadview.movetobottom").setEnabled(false);
    }

    @Override
    protected void onShow() {
        ActionController.getToolBarAction("action.downloadview.movetotop").setEnabled(true);
        ActionController.getToolBarAction("action.downloadview.moveup").setEnabled(true);
        ActionController.getToolBarAction("action.downloadview.movedown").setEnabled(true);
        ActionController.getToolBarAction("action.downloadview.movetobottom").setEnabled(true);
    }

    @Override
    public String getID() {
        return "linkgrabberview";
    }

}
