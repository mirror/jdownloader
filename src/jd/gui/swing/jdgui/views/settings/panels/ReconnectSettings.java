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

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.panels.reconnect.ReconnectManager;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_RECONNECT;
import org.jdownloader.translate._JDT;

public class ReconnectSettings extends AbstractConfigPanel {

    private static final long serialVersionUID = -7963763730328793139L;

    // private ReconnectTester tester;

    public String getTitle() {
        return _JDT._.gui_settings_reconnect_title();
    }

    public ReconnectSettings() {
        super();
        this.addHeader(_JDT._.gui_settings_reconnect_title_method(), NewTheme.I().getIcon("reconnect", 32));

        add(new ReconnectManager());
        this.addHeader(_GUI._.ReconnectSettings_ReconnectSettings_settings_(), NewTheme.I().getIcon("settings", 32));
        this.addDescription(_GUI._.ReconnectSettings_ReconnectSettings_settings_desc());

        addPair(_GUI._.ReconnectSettings_ReconnectSettings_enabled_(), null, new Checkbox(CFG_RECONNECT.AUTO_RECONNECT_ENABLED));

        addPair(_GUI._.ReconnectSettings_ReconnectSettings_prefer_reconnect_desc(), null, new Checkbox(CFG_RECONNECT.DOWNLOAD_CONTROLLER_PREFERS_RECONNECT_ENABLED));
        addPair(_GUI._.ReconnectSettings_ReconnectSettings_interrupt_resumable_allowed(), null, new Checkbox(CFG_RECONNECT.RECONNECT_ALLOWED_TO_INTERRUPT_RESUMABLE_DOWNLOADS));

    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("reconnect", 32);
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