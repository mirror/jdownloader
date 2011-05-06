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

import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.gui.swing.jdgui.views.settings.panels.reconnect.ReconnectManager;
import jd.gui.swing.jdgui.views.settings.panels.reconnect.ReconnectTester;

import org.jdownloader.extensions.AbstractConfigPanel;
import org.jdownloader.images.Theme;
import org.jdownloader.translate.JDT;

public class ReconnectSettings extends AbstractConfigPanel {

    private static final long serialVersionUID = -7963763730328793139L;
    private ReconnectTester   tester;

    public String getTitle() {
        return JDT._.gui_settings_reconnect_title();
    }

    public ReconnectSettings() {
        super();
        this.addHeader(JDT._.gui_settings_reconnect_title_method(), Theme.getIcon("settings/reconnect", 32));

        add(new ReconnectManager());
        this.addHeader(JDT._.gui_settings_reconnect_title_test(), Theme.getIcon("test", 32));

        add(tester = new ReconnectTester());

    }

    @Override
    public ImageIcon getIcon() {
        return Theme.getIcon("settings/reconnect", 32);
    }

    @Override
    protected void onShow() {

        new Thread() {
            @Override
            public void run() {
                final IP ip = IPController.getInstance().getIP();
                tester.updateCurrentIP(ip);

            }
        }.start();
    }

    @Override
    protected void onHide() {
    }
}