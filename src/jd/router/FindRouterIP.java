//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.util.logging.LogRecord;

import javax.swing.ScrollPaneConstants;

import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.components.MiniLogDialog;
import jd.gui.skins.simple.config.GUIConfigEntry;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class FindRouterIP implements ControlListener {

    private MiniLogDialog mld;

    public FindRouterIP(final GUIConfigEntry ip) {
        JDUtilities.getController().addControlListener(this);
        mld = new MiniLogDialog(JDLocale.L("gui.config.routeripfinder", "Router IPsearch"));
        mld.getBtnOK().setEnabled(false);
        mld.getProgress().setMaximum(100);
        mld.getProgress().setValue(2);

        new Thread() {
            @Override
            public void run() {
                ip.setData(JDLocale.L("gui.config.routeripfinder.featchIP", "Suche nach RouterIP..."));
                mld.getProgress().setValue(60);
                GetRouterInfo rinfo = new GetRouterInfo(null);
                mld.getProgress().setValue(80);
                String ipAdress = rinfo.getAdress();
                ip.setData(ipAdress);
                mld.getProgress().setValue(100);

                JDUtilities.getController().removeControlListener(FindRouterIP.this);
                mld.setTitle(JDLocale.LF("gui.config.routeripfinder.ready", "IP found: %s", ipAdress));
                mld.getBtnOK().setEnabled(true);
                mld.getBtnOK().setText(JDLocale.L("gui.config.routeripfinder.close", "Fenster schließen"));
            }
        }.start();
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_LOG_OCCURED && mld != null && mld.isEnabled()) {
            LogRecord l = (LogRecord) event.getParameter();

            if (l.getSourceClassName().startsWith("jd.router.GetRouterInfo")) {
                mld.setText(JDUtilities.formatSeconds((int) l.getMillis() / 1000) + " : " + l.getMessage() + "\r\n" + mld.getText());
                mld.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
                mld.getProgress().setValue(mld.getProgress().getValue() + 1);
            }
        }
    }
}
