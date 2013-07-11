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
import java.awt.event.KeyEvent;
import java.io.File;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.gui.UserIO;
import jd.nutils.io.JDFileFilter;

import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.container.ContainerPluginController;

public class AddContainerAction extends AppAction implements CachableInterface {

    private static final long serialVersionUID = 4713690050852393405L;

    public AddContainerAction() {
        setName(_GUI._.action_addcontainer());
        setTooltipText(_GUI._.action_addcontainer_tooltip());
        setIconKey("load");
        setAccelerator(KeyEvent.VK_O);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File[] ret = UserIO.getInstance().requestFileChooser("_LOADSAVEDLC", _GUI._.gui_filechooser_loaddlc(), UserIO.FILES_ONLY, new JDFileFilter(null, ContainerPluginController.getInstance().getContainerExtensions(null), true), true);
        if (ret == null) return;
        StringBuilder sb = new StringBuilder();
        for (File r : ret) {
            if (sb.length() > 0) sb.append("\r\n");
            sb.append("file://");
            sb.append(r.getAbsolutePath());
        }
        LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(sb.toString()));
    }

    @Override
    public void setData(String data) {
    }

}