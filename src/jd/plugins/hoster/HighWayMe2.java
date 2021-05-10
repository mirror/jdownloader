//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 1, names = { "high-way.me" }, urls = { "https?://high\\-way\\.me/onlinetv\\.php\\?id=\\d+[^/]+|https?://[a-z0-9\\-\\.]+\\.high\\-way\\.me/dlu/[a-z0-9]+/[^/]+" })
public class HighWayMe2 extends HighWayCore {
    public static interface HighWayMeConfigInterface extends UsenetAccountConfigInterface {
    };

    public HighWayMe2(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://high-way.me/pages/tariffs/");
    }

    @Override
    public String getAGBLink() {
        return "https://high-way.me/help/terms";
    }
    // /** According to High-Way staff, Usenet SSL is unavailable since 2017-08-01 */
    // @Override
    // public List<UsenetServer> getAvailableUsenetServer() {
    // final List<UsenetServer> ret = new ArrayList<UsenetServer>();
    // ret.addAll(UsenetServer.createServerList("reader.high-way.me", false, 119));
    // ret.addAll(UsenetServer.createServerList("reader.high-way.me", true, 563));
    // return ret;
    // }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private Thread showAPILoginInformation() {
        final Thread thread = new Thread() {
            final String apiCredsURL = "https://high-way.me/download.php#credentials";

            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "high-way.me - Login";
                        message += "Hallo liebe(r) high-way NutzerIn\r\n";
                        message += "Um deinen high-way Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "1. Öffne diesen Link im Browser falls das nicht automatisch passiert:\r\n\t'" + apiCredsURL + "'\t\r\n";
                        message += "2. Verwende die dort aufgeführten extra JDownloader Zugangsdaten und versuche den Login damit erneut!";
                    } else {
                        title = "high-way.me - Login";
                        message += "Hello dear high-way user\r\n";
                        message += "In order to use this service in JDownloader, you need to follow these steps:\r\n";
                        message += "1. Open this URL in your browser if it is not opened automatically:\r\n\t'" + apiCredsURL + "'\t\r\n";
                        message += "2. Look for the extra JDownloader login credentials on that page and retry the login process in JDownloader with those.";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(2 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(apiCredsURL);
                    }
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }
}