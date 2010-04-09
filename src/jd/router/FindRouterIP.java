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

package jd.router;

import java.awt.Color;
import java.net.InetAddress;

import jd.controlling.ProgressController;
import jd.gui.swing.jdgui.views.settings.GUIConfigEntry;
import jd.nrouter.RouterUtils;
import jd.utils.locale.JDL;

public class FindRouterIP {
    public static String findIP(GUIConfigEntry ip) {
        final ProgressController progress = new ProgressController(JDL.L("gui.config.routeripfinder.featchIP", "Search for routers hostname..."), 100, null);

        ip.setData(JDL.L("gui.config.routeripfinder.featchIP", "Search for routers hostname..."));

        progress.setStatus(80);
        InetAddress ia = RouterUtils.getAddress(false);
        if (ia != null) ip.setData(ia.getHostAddress());
        progress.setStatus(100);
        if (ia != null) {
            progress.setStatusText(JDL.LF("gui.config.routeripfinder.ready", "Hostname found: %s", ia.getHostAddress()));
            progress.doFinalize(3000);
            return ia.getHostAddress();

        } else {
            progress.setStatusText(JDL.L("gui.config.routeripfinder.notfound", "Can't find your routers hostname"));
            progress.doFinalize(3000);
            progress.setColor(Color.RED);

            return null;
        }
    }

    public FindRouterIP(final GUIConfigEntry ip) {

        new Thread() {
            // @Override
            public void run() {
                findIP(ip);
            }
        }.start();
    }

}