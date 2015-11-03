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
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 30971 $", interfaceVersion = 3, names = { "trinimixzone.com" }, urls = { "http://.trinimixzone\\.com/DO_DOT_MATCH_PREMIUM_ACC_SUPPLY_ONLY" }, flags = { 0 })
public class TrinimixzoneCom extends PluginForHost {

    public TrinimixzoneCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://trinimixzone.com/forum/member.php?action=register");
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
        br.clearCookies("http://trinimixzone.com/forum/member.php");
        Form form = new Form();
        form.setAction("http://trinimixzone.com/forum/member.php");
        form.setEncoding("application/x-www-form-urlencoded");
        form.setMethod(MethodType.POST);
        form.addInputField(new InputField("action", "do_login"));
        form.addInputField(new InputField("url", Encoding.urlEncode("http://trinimixzone.com/forum/index.php")));
        form.addInputField(new InputField("quick_username", Encoding.urlEncode(account.getUser())));
        form.addInputField(new InputField("quick_password", Encoding.urlEncode(account.getPass())));
        form.addInputField(new InputField("quick_remember", "yes"));
        form.addInputField(new InputField("submit", "Login"));
        br.submitForm(form);

        return br.getCookie(br.getHost(), "mybbuser") != null;

    }

    @Override
    public String getAGBLink() {
        return "http://trinimixzone.com/";
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
        ArrayList<Account> accounts = AccountController.getInstance().list("trinimixzone.com");
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
