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

package jd.controlling;

import java.net.URL;

import jd.DecryptPluginWrapper;
import jd.OptionalPluginWrapper;
import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.gui.swing.components.Balloon;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.parser.Regex;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class CNL2 {

    /**
     * runs through all decrypter and checks if cnl2 is enabled and if text
     * equals a handleable link. if yes, the defaultbrowser opens.
     * 
     * @param text
     * @return
     */
    public static boolean checkText(String text) {
        if (text == null) return false;
        text = text.trim();
        if (!isExternInterfaceActive()) return false;
        if (!SubConfiguration.getConfig(LinkGrabberController.CONFIG).getBooleanProperty(LinkGrabberController.PARAM_USE_CNL2, true)) return false;
        try {
            for (DecryptPluginWrapper plg : DecryptPluginWrapper.getDecryptWrapper()) {
                if ((plg.getFlags() & PluginWrapper.CNL_2) > 0) {
                    if (plg.canHandle(text)) {
                        String match = new Regex(text, plg.getPattern()).getMatch(-1);
                        if (match.equalsIgnoreCase(text)) {
                            if (text.contains("?")) {
                                LocalBrowser.openDefaultURL(new URL(text + "&jd=1"));
                            } else {
                                LocalBrowser.openDefaultURL(new URL(text + "?jd=1"));
                            }
                            Balloon.show(JDL.L("jd.controlling.CNL2.checkText.title", "Click'n'Load"), null, JDL.L("jd.controlling.CNL2.checkText.message", "Click'n'Load URL opened"));
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    private static boolean isExternInterfaceActive() {
        OptionalPluginWrapper plg = JDUtilities.getOptionalPlugin("externinterface");
        return (plg != null && plg.isLoaded() && plg.isEnabled());
    }
}
