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

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.translate._GUI;

public class KnowledgeAction extends ActionAdapter {

    private static final long serialVersionUID = 2227665710503234763L;

    public KnowledgeAction() {
        super(_GUI._.action_help(), "action.help", "help");
    }

    @Override
    public void onAction(ActionEvent e) {
        CrossSystem.openURLOrShowMessage("http://jdownloader.org/knowledge/index");
    }

    @Override
    public void initDefaults() {
    }

    @Override
    public String createAccelerator() {
        return ShortcutController._.getOpenKnowledgebaseAction();
    }

    @Override
    public String createTooltip() {
        return _GUI._.action_help_tooltip();
    }

}
