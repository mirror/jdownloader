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

package jd.gui.swing.jdgui.menu.actions;

import java.awt.event.ActionEvent;

import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.GraphicalUserInterfaceSettings;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.views.log.LogView;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.gui.translate._GUI;

public class LogAction extends ToolBarAction {

    private static final long serialVersionUID = -353145605693194634L;

    public LogAction() {
        super(_GUI._.action_log(), "action.log", "log");
    }

    @Override
    public void initDefaults() {
    }

    @Override
    public void onAction(final ActionEvent e) {

        JsonConfig.create(GraphicalUserInterfaceSettings.class).setLogViewVisible(true);
        SwingGui.getInstance().setContent(LogView.getInstance(), true);
    }

    @Override
    protected String createMnemonic() {
        return _GUI._.action_log_mnemonic();
    }

    @Override
    protected String createAccelerator() {
        return _GUI._.action_log_accelerator();
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_log_tooltip();
    }

}
