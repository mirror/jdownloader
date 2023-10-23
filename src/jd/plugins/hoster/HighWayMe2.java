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

import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetConfigPanel;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.plugins.config.AccountConfigInterface;
import org.jdownloader.plugins.config.Order;

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 4, names = { "high-way.me" }, urls = { "https?://high-way\\.me/onlinetv\\.php\\?id=\\d+[^/]+|https?://((?:torrent|usenet)(archiv)?)\\.(?:high-way\\.me|dwld\\.link)/dl(?:u|t)/[a-z0-9]+(?:/$|/.+)" })
public class HighWayMe2 extends HighWayCore {
    protected static MultiHosterManagement mhm                      = new MultiHosterManagement("high-way.me");
    private static final String            urlWebsiteAPICredentials = "high-way.me/pages/cred/";

    public HighWayMe2(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://high-way.me/pages/tariffs/");
    }

    @Override
    public String getAGBLink() {
        return "https://high-way.me/help/terms";
    }

    @Override
    protected String getAPIBase() {
        return "https://" + this.getHost() + "/apiV2.php";
    }

    @Override
    public String getWebsiteBase() {
        return "https://" + this.getHost() + "/";
    }

    @Override
    protected boolean useApikeyLogin() {
        return false;
    }

    @Override
    protected void exceptionAccountInvalid(final Account account) throws PluginException {
        showAPILoginInformation();
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            throw new AccountInvalidException("\r\nUngültiger Benutzername/Passwort!\r\n Du findest deine Zugangsdaten für JDownloader hier: " + urlWebsiteAPICredentials);
        } else {
            throw new AccountInvalidException("\r\nInvalid username/password!\r\nYou can find your JDownloader login credentials here: " + urlWebsiteAPICredentials);
        }
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("reader.high-way.me", false, 119));
        ret.addAll(UsenetServer.createServerList("reader.high-way.me", true, 563));
        return ret;
    }

    private Thread showAPILoginInformation() {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "high-way.me - Login";
                        message += "Hallo liebe(r) high-way NutzerIn\r\n";
                        message += "Um deinen high-way Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "1. Öffne diesen Link im Browser falls das nicht automatisch passiert:\r\n\t'" + urlWebsiteAPICredentials + "'\t\r\n";
                        message += "2. Verwende die dort aufgeführten extra JDownloader Zugangsdaten und versuche den Login damit erneut!";
                    } else {
                        title = "high-way.me - Login";
                        message += "Hello dear high-way user\r\n";
                        message += "In order to use this service in JDownloader, you need to follow these steps:\r\n";
                        message += "1. Open this URL in your browser if it is not opened automatically:\r\n\t'" + urlWebsiteAPICredentials + "'\t\r\n";
                        message += "2. Look for the extra JDownloader login credentials on that page and retry the login process in JDownloader with those.";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(2 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL("https://" + urlWebsiteAPICredentials);
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

    public static interface HighWayMeConfigInterface extends UsenetAccountConfigInterface {
        public class Translation {
            public String getUseDownloadslotBlockingCloudDownloadMode_label() {
                return "Block download slots for files which have to be downloaded to the multihoster first? If you disable this, you will need to add account usage rules to be able to smoothly use this feature!";
            }
        }

        public static final HighWayMeConfigInterface.Translation TRANSLATION = new Translation();

        @AboutConfig
        @DefaultBooleanValue(true)
        @Order(10)
        boolean isUseDownloadslotBlockingCloudDownloadMode();

        void setUseDownloadslotBlockingCloudDownloadMode(boolean b);
    };

    @Override
    protected PluginConfigPanelNG createConfigPanel() {
        return new UsenetConfigPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean showKeyHandler(KeyHandler<?> keyHandler) {
                return "usedownloadslotblockingclouddownloadmode".equals(keyHandler.getKey());
            }

            @Override
            protected boolean useCustomUI(KeyHandler<?> keyHandler) {
                return !"usedownloadslotblockingclouddownloadmode".equals(keyHandler.getKey());
            }

            @Override
            protected void initAccountConfig(PluginForHost plgh, Account acc, Class<? extends AccountConfigInterface> cf) {
                super.initAccountConfig(plgh, acc, cf);
                extend(this, getHost(), getAvailableUsenetServer(), getAccountJsonConfig(acc));
            }
        };
    }

    @Override
    public HighWayMeConfigInterface getAccountJsonConfig(final Account account) {
        return (HighWayMeConfigInterface) super.getAccountJsonConfig(account);
    }

    @Override
    protected boolean blockDownloadSlotsForCloudDownloads(final Account account) {
        if (getAccountJsonConfig(account).isUseDownloadslotBlockingCloudDownloadMode()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    protected MultiHosterManagement getMultiHosterManagement() {
        return mhm;
    }
}