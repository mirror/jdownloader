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

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.plugins.components.usenet.UsenetServer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 1, names = { "high-way.me" }, urls = { "https?://high\\-way\\.me/onlinetv\\.php\\?id=\\d+[^/]+|https?://[a-z0-9\\-\\.]+\\.high\\-way\\.me/dlu/[a-z0-9]+/[^/]+" })
public class HighWayMe2 extends HighWayCore {
    private static final String PROPERTY_ACCOUNT_API_MIGRATION_MESSAGE_DISPLAYED = "API_MIGRATION_MESSAGE_DISPLAYED";

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
    protected String getWebsiteBase() {
        return "https://" + this.getHost() + "/";
    }

    @Override
    protected boolean useApikeyLogin() {
        return false;
    }

    @Override
    protected void exceptionAccountInvalid(final Account account) throws PluginException {
        if (account.hasProperty(PROPERTY_ACCOUNT_MAXCHUNKS) && !account.hasProperty(PROPERTY_ACCOUNT_API_MIGRATION_MESSAGE_DISPLAYED)) {
            /**
             * Show this message once for every user after migration to APIv2. </br> This uses property "usenetU" to determine if this
             * account has ever been checked successfully before. </br> TODO: Remove this after 2021-09 (some time in 2021-10)
             */
            account.setProperty(PROPERTY_ACCOUNT_API_MIGRATION_MESSAGE_DISPLAYED, true);
            showOneTimeLougoutAPIMigrationMessage();
        } else {
            showAPILoginInformation();
        }
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\n Du findest deine Zugangsdaten für JD hier: high-way.me/download.php#credentials", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou can find your JD login credentials here: high-way.me/download.php#credentials", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("reader.high-way.me", false, 119));
        ret.addAll(UsenetServer.createServerList("reader.high-way.me", true, 563));
        return ret;
    }

    private Thread showOneTimeLougoutAPIMigrationMessage() {
        final Thread thread = new Thread() {
            public void run() {
                final String apiCredsURLWithoutProtocol = "high-way.me/download.php#credentials";
                final String twoFALoginSettingsURLWithoutProtocol = "high-way.me/account/two-step";
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "high-way.me - einmaliger Logout";
                        message += "Hallo liebe(r) high-way NutzerIn\r\n";
                        message += "Wegen technischer Änderungen wurdest du einmalig automatisch ausgeloggt.\r\n";
                        message += "Es gibt ab sofort separate high-way Zugangsdaten für JD, die sich von denen die du für den Browser benötigst unterscheiden.\r\n";
                        message += "Dies dient der sicherheit deines high-way Accounts!\r\n";
                        message += "Du findest diese hier: " + apiCredsURLWithoutProtocol + "\r\n";
                        message += "Außerdem kannst du JDownloader ab sofort auch mit aktivierter 2 Faktor Authentifizierung verwenden!\r\n";
                        message += "Es wird empfohlen, die 2 Faktor Authentifizierung hier zu aktivieren: " + twoFALoginSettingsURLWithoutProtocol;
                    } else {
                        title = "high-way.me - you've been logged out";
                        message += "Hello dear high-way user\r\n";
                        message += "Due to technical changes you have been logged out automatically once.\r\n";
                        message += "From now on you need separate high-way login credentials for JD which are different from the ones you need in your browser.\r\n";
                        message += "This step was taken to improve account security.\r\n";
                        message += "Your can find your new login credentials here: " + apiCredsURLWithoutProtocol + "\r\n";
                        message += "Also you can now enable two factor authentication for high-way and keep using JDownloader.\r\n";
                        message += "It is recommended to enable two factor authentication here: " + twoFALoginSettingsURLWithoutProtocol;
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(3 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL("https://" + apiCredsURLWithoutProtocol);
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

    private Thread showAPILoginInformation() {
        final Thread thread = new Thread() {
            public void run() {
                final String apiCredsURLWithoutProtocol = "high-way.me/download.php#credentials";
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "high-way.me - Login";
                        message += "Hallo liebe(r) high-way NutzerIn\r\n";
                        message += "Um deinen high-way Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "1. Öffne diesen Link im Browser falls das nicht automatisch passiert:\r\n\t'" + apiCredsURLWithoutProtocol + "'\t\r\n";
                        message += "2. Verwende die dort aufgeführten extra JDownloader Zugangsdaten und versuche den Login damit erneut!";
                    } else {
                        title = "high-way.me - Login";
                        message += "Hello dear high-way user\r\n";
                        message += "In order to use this service in JDownloader, you need to follow these steps:\r\n";
                        message += "1. Open this URL in your browser if it is not opened automatically:\r\n\t'" + apiCredsURLWithoutProtocol + "'\t\r\n";
                        message += "2. Look for the extra JDownloader login credentials on that page and retry the login process in JDownloader with those.";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(2 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL("https://" + apiCredsURLWithoutProtocol);
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

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}