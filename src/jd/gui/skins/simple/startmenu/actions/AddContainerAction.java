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

package jd.gui.skins.simple.startmenu.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import jd.gui.skins.simple.components.JDFileChooser;
import jd.nutils.io.JDFileFilter;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class AddContainerAction extends StartAction {

    private static final long serialVersionUID = 4713690050852393405L;

    public AddContainerAction() {
        super("action.load", "gui.images.load");
    }

    public void actionPerformed(ActionEvent e) {
        AddContainerAction.addContainerDialog();
    }

    public static void addContainerDialog() {
        JDFileChooser fc = new JDFileChooser("_LOADSAVEDLC");
        fc.setDialogTitle(JDL.L("gui.filechooser.loaddlc", "Load DLC file"));
        fc.setFileFilter(new JDFileFilter(null, ".dlc|.rsdf|.ccf|.metalink", true));
        if (fc.showOpenDialog(null) == JDFileChooser.APPROVE_OPTION) {
            File ret2 = fc.getSelectedFile();
            if (ret2 != null) {
                JDUtilities.getController().loadContainerFile(ret2);
            }
        }
    }

}
