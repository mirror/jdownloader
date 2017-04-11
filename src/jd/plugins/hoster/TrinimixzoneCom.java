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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.logging2.LogInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "trinimixzone.com" }, urls = { "" }) 
public class TrinimixzoneCom extends PluginForHost {

    public TrinimixzoneCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://trinimixzone.com/forum/member.php?action=register");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        if (!login(br, account)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        ai.setStatus("Account is OK");
        ai.setValidUntil(-1);
        return ai;
    }

    public static boolean login(Browser br, Account account) throws Exception {
        br.setCookiesExclusive(true);
        br.clearCookies("http://trinimixzone.com/forum/member.php");
        final Form form = new Form();
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
        final boolean ret = br.getCookie(br.getHost(), "mybbuser") != null;
        if (!ret) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        return ret;
    }

    @Override
    public String getAGBLink() {
        return "http://trinimixzone.com/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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

    public static boolean login(final Browser br) {
        final ArrayList<Account> accounts = AccountController.getInstance().getValidAccounts("trinimixzone.com");
        if (accounts != null && accounts.size() != 0) {
            final LogInterface logger = br.getLogger();
            for (final Account account : accounts) {
                try {
                    if (login(br, account)) {
                        return true;
                    }
                } catch (final PluginException e) {
                    logger.log(e);
                    account.setValid(false);
                } catch (final Exception e) {
                    logger.log(e);
                }
            }
        }
        return false;
    }
}
