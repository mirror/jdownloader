//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.HostPlugin;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zevera.com" }, urls = { "https?://(?:[a-z0-9\\.\\-]+)?zevera\\.com/file\\?id=([A-Za-z0-9\\-_]+)" })
public class ZeveraCom extends ZeveraCore {
    protected static MultiHosterManagement mhm = new MultiHosterManagement("zevera.com");

    public ZeveraCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www." + this.getHost() + "/premium");
    }

    @Override
    public String getClientID() {
        return getClientIDExt();
    }

    public static String getClientIDExt() {
        return "306575304";
    }

    @Override
    public int getDownloadModeMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 0;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 0;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean supportsUsenet(final Account account) {
        return false;
    }

    @Override
    public boolean usePairingLogin(final Account account) {
        /** 2019-08-05: Pairing login is not supported by this service! */
        return false;
    }

    @Override
    protected MultiHosterManagement getMultiHosterManagement() {
        return mhm;
    }
}