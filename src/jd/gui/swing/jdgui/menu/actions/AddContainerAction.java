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
import jd.controlling.linkcollector.LinkOrigin;
import jd.gui.UserIO;

import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.container.ContainerPluginController;

public class AddContainerAction extends CustomizableAppAction {
    private static final long serialVersionUID = 4713690050852393405L;

    public AddContainerAction() {
        setName(_GUI.T.action_addcontainer());
        setTooltipText(_GUI.T.action_addcontainer_tooltip());
        setIconKey(IconKey.ICON_LOAD);
        setAccelerator(KeyEvent.VK_O);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final File[] files = UserIO.getInstance().requestFileChooser("_LOADSAVEDLC", _GUI.T.gui_filechooser_loaddlc(), UserIO.FILES_ONLY, true, ContainerPluginController.getInstance().getContainerFileFilter(null));
        if (files == null || files.length == 0) {
            return;
        }
        final StringBuilder sb = new StringBuilder();
        for (final File file : files) {
            if (sb.length() > 0) {
                sb.append("\r\n");
            }
            sb.append(file.toURI().toString());
        }
        LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(LinkOrigin.ADD_CONTAINER_ACTION.getLinkOriginDetails(), sb.toString()));
    }
}