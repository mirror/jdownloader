//    jDownloader - Downloadmanager
//    Copyright (C) 2010  JD-Team support@jdownloader.org
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

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.myjd.MyJDownloaderView;

public class MyJDownloaderTabAction extends CustomizableAppAction {

    private static final long serialVersionUID = 2547991585530678706L;

    public MyJDownloaderTabAction() {
        setIconKey(IconKey.ICON_LOGO_MYJDOWNLOADER);
        setTooltipText(_GUI.T.action_myjdownloader_menu_tooltip());
        setName(_GUI.T.action_myjdownloader_menu());
        setAccelerator(KeyEvent.VK_M);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // AccountManagerSettings
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setMyJDownloaderViewVisible(true);
        JDGui.getInstance().setContent(MyJDownloaderView.getInstance(), true);
    }

}
