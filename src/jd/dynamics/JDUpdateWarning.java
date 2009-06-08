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

package jd.dynamics;

import jd.config.SubConfiguration;
import jd.gui.UserIO;

public class JDUpdateWarning extends DynamicPluginInterface {

    @Override
    public void execute() {
        new Thread() {
            public void run() {

                Boolean doit = SubConfiguration.getConfig("dynamics").getBooleanProperty("SHOWN", false);
                if (!doit) {
                    UserIO.getInstance().requestConfirmDialog(UserIO.NO_CANCEL_OPTION | UserIO.STYLE_HTML, "JD Update", "<h3>New Update available!</h3> JDownloader now updates hoster and decrypt plugins.<br/><b>This is not a crash!</b><br/> Just wait.", null, null, null);

                    try {
                        Thread.sleep(1 * 60 * 1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    SubConfiguration.getConfig("dynamics").setProperty("SHOWN", true);
                    SubConfiguration.getConfig("dynamics").save();
                }
            }
        }.start();

    }

}
