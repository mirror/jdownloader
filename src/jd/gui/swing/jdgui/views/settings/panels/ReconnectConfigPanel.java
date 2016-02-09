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

package jd.gui.swing.jdgui.views.settings.panels;

import javax.swing.Icon;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_RECONNECT;
import org.jdownloader.translate._JDT;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.panels.reconnect.ReconnectManager;

public class ReconnectConfigPanel extends AbstractConfigPanel {

    private static final long serialVersionUID = -7963763730328793139L;

    // private ReconnectTester tester;

    public String getTitle() {
        return _JDT.T.gui_settings_reconnect_title();
    }

    public ReconnectConfigPanel() {
        super();
        this.addHeader(_GUI.T.ReconnectSettings_ReconnectSettings_settings_(), new AbstractIcon(IconKey.ICON_SETTINGS, 32));
        this.addDescription(_GUI.T.ReconnectSettings_ReconnectSettings_settings_desc2());

        addPair(_GUI.T.ReconnectSettings_ReconnectSettings_enabled_(), null, new Checkbox(CFG_RECONNECT.AUTO_RECONNECT_ENABLED));

        addPair(_GUI.T.ReconnectSettings_ReconnectSettings_prefer_reconnect_desc(), null, new Checkbox(CFG_RECONNECT.DOWNLOAD_CONTROLLER_PREFERS_RECONNECT_ENABLED));
        addPair(_GUI.T.ReconnectSettings_ReconnectSettings_interrupt_resumable_allowed(), null, new Checkbox(CFG_RECONNECT.RECONNECT_ALLOWED_TO_INTERRUPT_RESUMABLE_DOWNLOADS));

        this.addHeader(_JDT.T.gui_settings_reconnect_title_method(), new AbstractIcon(IconKey.ICON_RECONNECT, 32));

        add(new ReconnectManager());

    }

    @Override
    public Icon getIcon() {
        return new AbstractIcon(IconKey.ICON_RECONNECT, 32);
    }

    @Override
    public void save() {

    }

    @Override
    public void updateContents() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {

            }
        };
        // new Thread() {
        // @Override
        // public void run() {
        // final IP ip = IPController.getInstance().getIP();
        // tester.updateCurrentIP(ip);
        //
        // }
        // }.start();
    }
}