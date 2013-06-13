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

/* =========== IMPORTANT HINT! ===========
 * 
 * We need this dummy-class to migrate all 
 * existing premium4.me accounts to 
 * premium.to accounts
 * 
 * PLEASE REMOVE THIS FILE ON 01/08/2013
 * 
 * =======================================
 */

package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premium4.me" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd5654" }, flags = { 2 })
public class Premium4Me extends PluginForHost {

    /*
     * =========== IMPORTANT HINT! ===========
     * 
     * We need this dummy-class to migrate all existing premium4.me accounts to premium.to accounts
     * 
     * PLEASE REMOVE THIS FILE ON 01/08/2013
     * 
     * =======================================
     */

    public Premium4Me(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://premium4.me/");
        // migrate premium4.me accounts to premium.to
        migrateAccounts();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        // migrate premium4.me accounts to premium.to
        migrateAccounts();
        return (new AccountInfo());
    }

    @Override
    public String getAGBLink() {
        return "http://premium4.me/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        // migrate premium4.me accounts to premium.to
        migrateAccounts();
        return false;
    }

    @Override
    public void reset() {
    }

    private void resetAvailablePremium(Account ac) {
        ac.setProperty("multiHostSupport", Property.NULL);
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private void migrateAccounts() {
        // migrate premium4.me accounts to premium.to
        if (AccountController.getInstance().hasAccounts("premium4.me")) {
            for (Account account : AccountController.getInstance().getValidAccounts("premium4.me")) {
                Account newAcc = new Account(account.getUser(), account.getPass());
                newAcc.setHoster("premium.to");
                account.getAccountController().addAccount(newAcc);
                account.getAccountController().removeAccount(account);
                newAcc.getAccountController().updateAccountInfo(newAcc, true);
            }
        }
    }

}