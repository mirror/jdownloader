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
import java.io.File;

import jd.controlling.JDController;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.nutils.io.JDFileFilter;
import jd.utils.JDUtilities;

import org.jdownloader.gui.translate._GUI;

public class AddContainerAction extends ToolBarAction {

    private static final long serialVersionUID = 4713690050852393405L;

    public AddContainerAction() {
        super(_GUI._.action_addcontainer(), "action.load", "load");
    }

    @Override
    public void onAction(ActionEvent e) {
        File[] ret = UserIO.getInstance().requestFileChooser("_LOADSAVEDLC", _GUI._.gui_filechooser_loaddlc(), UserIO.FILES_ONLY, new JDFileFilter(null, JDUtilities.getContainerExtensions(null), true), true);
        if (ret == null) return;
        for (File r : ret) {
            JDController.loadContainerFile(r);
        }
    }

    @Override
    public void initDefaults() {
    }

    @Override
    protected String createMnemonic() {
        return _GUI._.action_addcontainer_mnemonic();
    }

    @Override
    protected String createAccelerator() {
        return _GUI._.action_addcontainer_accelerator();
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_addcontainer_tooltip();
    }

}