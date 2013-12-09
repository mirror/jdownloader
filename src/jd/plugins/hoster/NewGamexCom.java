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
import java.net.URL;

import jd.PluginWrapper;
import jd.nutils.SimpleFTP;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "newgamex.com" }, urls = { "ftp://(www\\.)?download\\.newgamex\\.com/.+" }, flags = { 2 })
public class NewGamexCom extends PluginForHost {

    public NewGamexCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.newgamex.com/");
    }

    @Override
    public String getAGBLink() {
        return "http://www.newgamex.com/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        link.getLinkStatus().setStatusText("Status can only be checked on downloadstart");
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) throw (PluginException) e;
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by registered/premium users");
    }

    private static Object LOCK = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            SimpleFTP ftp = new SimpleFTP();
            try {
                ftp.setLogger(logger);
                String url = "ftp://" + account.getUser() + ":" + account.getPass() + "@download.newgamex.com/";
                ftp.connect(new URL(url));
            } catch (IOException e) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } finally {
                try {
                    ftp.disconnect();
                } catch (final Throwable ignore) {
                }
            }
        }

    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        // Login happens in here
        String dlURL = link.getDownloadURL();
        if (dlURL.startsWith("ftp")) {
            String url = new Regex(dlURL, "ftp://(.*?@)?(.+)").getMatch(1);
            String dllink = "ftp://" + account.getUser() + ":" + account.getPass() + "@" + url;
            try {
                ((Ftp) JDUtilities.getNewPluginForHostInstance("ftp")).download(dllink, link, true);
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("530")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login incorrect", PluginException.VALUE_ID_PREMIUM_DISABLE); }
                throw e;
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}