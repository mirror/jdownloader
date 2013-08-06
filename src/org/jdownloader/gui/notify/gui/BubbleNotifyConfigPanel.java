//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package org.jdownloader.gui.notify.gui;

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;

import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class BubbleNotifyConfigPanel extends AbstractConfigPanel implements StateUpdateListener {

    private static final long serialVersionUID = 1L;

    public String getTitle() {
        return _GUI._.NotifierConfigPanel_getTitle();
    }

    public BubbleNotifyConfigPanel() {
        super();

        this.addHeader(getTitle(), NewTheme.I().getIcon("bubble", 32));
        this.addDescription(_GUI._.plugins_optional_JDLightTray_ballon_desc());

        addPair(_GUI._.plugins_optional_JDLightTray_ballon_newPackages(), null, new Checkbox(CFG_BUBBLE.BUBBLE_NOTIFY_ON_NEW_LINKGRABBER_PACKAGE_ENABLED));
        addPair(_GUI._.plugins_optional_JDLightTray_ballon_newlinks(), null, new Checkbox(CFG_BUBBLE.BUBBLE_NOTIFY_ON_NEW_LINKGRABBER_LINKS_ENABLED));
        addPair(_GUI._.plugins_optional_JDLightTray_ballon_updates(), null, new Checkbox(CFG_BUBBLE.BUBBLE_NOTIFY_ON_UPDATE_AVAILABLE_ENABLED));
        addPair(_GUI._.plugins_optional_JDLightTray_ballon_reconnectstart(), null, new Checkbox(CFG_BUBBLE.BUBBLE_NOTIFY_ON_RECONNECT_START_ENABLED));
        addPair(_GUI._.plugins_optional_JDLightTray_ballon_reconnectend(), null, new Checkbox(CFG_BUBBLE.BUBBLE_NOTIFY_ON_RECONNECT_END_ENABLED));

    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("bubble", 32);
    }

    @Override
    public void save() {

    }

    @Override
    public void updateContents() {

    }

    @Override
    public void onStateUpdated() {

    }
}