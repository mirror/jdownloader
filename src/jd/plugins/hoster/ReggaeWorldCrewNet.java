//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 30971 $", interfaceVersion = 3, names = { "reggaeworldcrew.net" }, urls = { "" }, flags = { 0 })
public class ReggaeWorldCrewNet extends PluginForHost {

    public ReggaeWorldCrewNet(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.reggaeworldcrew.net/foro/register.php?");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {

        final AccountInfo ai = new AccountInfo();
        try {
            if (!login(br, account)) {
                account.setValid(false);
                return ai;
            }

        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        //
        ai.setStatus("Account is OK");
        ai.setValidUntil(-1);
        account.setValid(true);
        return ai;

    }

    public static boolean login(Browser br, Account account) throws Exception {
        br.setCookiesExclusive(true);
        br.clearCookies("http://www.reggaeworldcrew.net/foro/login.php?do=login");

        br.postPage("http://www.reggaeworldcrew.net/foro/login.php?do=login", "cookieuser=1&s=&securitytoken=guest&do=login&vb_login_password_hint=Contrase%C3%B1a&vb_login_md5password=&vb_login_md5password_utf=&vb_login_username=" + Encoding.urlEncode(account.getUser()) + "&vb_login_password=" + Encoding.urlEncode(account.getPass()));

        return br.getCookie(br.getHost(), "bb_sessionhash") != null;

    }

    @Override
    public String getAGBLink() {
        return "http://reggaeworldcrew.net/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        return null;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {

    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public static boolean login(Browser br) {
        ArrayList<Account> accounts = AccountController.getInstance().list("reggaeworldcrew.net");
        if (accounts != null && accounts.size() != 0) {
            final Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                final Account n = it.next();
                if (n.isEnabled() && n.isValid()) {

                    try {

                        if (login(br, n)) {
                            return true;
                        }

                    } catch (final Exception e) {

                        n.setValid(false);

                    }

                }
            }
        }
        return false;
    }
}
