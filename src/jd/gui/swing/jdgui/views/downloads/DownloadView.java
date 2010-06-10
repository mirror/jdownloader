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

package jd.gui.swing.jdgui.views.downloads;

import javax.swing.Icon;

import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.interfaces.View;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class DownloadView extends View {

    private static final long serialVersionUID = 2624923838160423884L;

    private static final String IDENT_PREFIX = "jd.gui.swing.jdgui.views.downloadview.";

    private static DownloadView INSTANCE = null;

    private DownloadView() {
        super();
        this.setContent(DownloadLinksPanel.getDownloadLinksPanel());
        this.setDefaultInfoPanel(new DownloadInfoPanel());
    }

    public synchronized static DownloadView getInstance() {
        if (INSTANCE == null) INSTANCE = new DownloadView();
        return INSTANCE;
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II("gui.images.taskpanes.download", ICON_SIZE, ICON_SIZE);
    }

    @Override
    public String getTitle() {
        return JDL.L(IDENT_PREFIX + "tab.title", "Download");
    }

    @Override
    public String getTooltip() {
        return JDL.L(IDENT_PREFIX + "tab.tooltip", "Downloadlist and Progress");
    }

    @Override
    protected void onHide() {
        setActionStatus(false);
    }

    @Override
    protected void onShow() {
        setActionStatus(true);
    }

    private void setActionStatus(boolean enabled) {
        ActionController.getToolBarAction("action.downloadview.movetotop").setEnabled(enabled);
        ActionController.getToolBarAction("action.downloadview.moveup").setEnabled(enabled);
        ActionController.getToolBarAction("action.downloadview.movedown").setEnabled(enabled);
        ActionController.getToolBarAction("action.downloadview.movetobottom").setEnabled(enabled);

        ActionController.getToolBarAction("action.remove.links").setEnabled(enabled);
        ActionController.getToolBarAction("action.remove.packages").setEnabled(enabled);
        ActionController.getToolBarAction("action.remove_dupes").setEnabled(enabled);
        ActionController.getToolBarAction("action.remove_disabled").setEnabled(enabled);
        ActionController.getToolBarAction("action.remove_offline").setEnabled(enabled);
        ActionController.getToolBarAction("action.remove_failed").setEnabled(enabled);
    }

    @Override
    public String getID() {
        return "downloadview";
    }

}
