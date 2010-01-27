//    jDownloader // Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.JDHash;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharesystems.de" }, urls = { "http://[//w//.]*?sharesystems\\.de/.*?hash=(.*)" }, flags = { 2 })
/*
 * * Plugin for sharesystems. Written on adminrequest. Uses API given by
 * sharesystems.de
 */
public class ShareSystemsDe extends PluginForHost {

    public ShareSystemsDe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://sharesystems.de/index.php?vip");
    }

    @Override
    public String getAGBLink() {
        return "http://sharesystems.de/index.php?agb";
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        br.getPage("http://sharesystems.de/index.php?login");
        br.forceDebug(true);
        Form login = br.getForm(0);
        login.put("username", account.getUser());
        login.put("password", account.getPass());
        br.submitForm(login);
        if (br.getRedirectLocation() == null || br.getCookie("http://sharesystems.de", "login_hash") == null) {
            account.setValid(false);
        } else {
            account.setValid(true);
            br.getPage("http://sharesystems.de/index.php?statistik");
            String validUntil = br.getRegex("Account g.*?ltig bis</td>.*?<td class=\"rightT\">(.*?)Uhr</td>").getMatch(0);
            if (validUntil != null) ai.setValidUntil(Regex.getMilliSeconds("GMT+00:00, " + validUntil.trim(), "z, dd.MM.yyyy HH:mm", null));
            String trafficLeft = br.getRegex("Traffic-Kontostand</td>.*?<td class=\"rightT\">(.*?)</td>").getMatch(0);
            if (trafficLeft != null) ai.setTrafficLeft(Regex.getSize(trafficLeft));
        }
        return ai;
    }

    /**
     * Checks if the downloadlink is valid and online. Throws a PluginException
     * if this file is likely invalid, damaged or has any other errors.
     */
    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.forceDebug(true);
        br.getPage(downloadLink.getDownloadURL());
        String hash = new Regex(downloadLink.getDownloadURL().toLowerCase(), "hash=([0-9a-f]{32})").getMatch(0);

        // method returns either the filename or an errorid.

        String name = br.getPage("http://api.sharesystems.de/?action=jd_free&hash=" + hash + "&get_filename").trim();
        // throws an appripriate exception of name is an errorid
        parseError(name);
        String size = br.getPage("http://api.sharesystems.de/?action=jd_free&hash=" + hash + "&get_filesize").trim();
        downloadLink.setFinalFileName(name);
        downloadLink.setDownloadSize(Long.parseLong(size));
        String md5hash = br.getPage("http://api.sharesystems.de/?action=jd_free&hash=" + hash + "&get_md5").trim();
        downloadLink.setMD5Hash(md5hash);

        return AvailableStatus.TRUE;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {

        this.requestFileInformation(downloadLink);
        // e86466b05601d8c12952105d82a9ad6a is the jd key
        String login = account.getUser() + JDHash.getMD5(account.getPass()) + "e86466b05601d8c12952105d82a9ad6a";
        String hash = new Regex(downloadLink.getDownloadURL().toLowerCase(), "hash=([0-9a-f]{32})").getMatch(0);

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, "http://api.sharesystems.de/?action=jd_vip&hash=" + hash + "&key=" + JDHash.getMD5(login), true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            parseError(br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    /**
     * @param string
     * @throws PluginException
     */
    private void parseError(String string) throws PluginException {
        int errorID = -1;
        String[] params = Regex.getLines(string);
        string = params[0];
        try {
            errorID = Integer.parseInt(string);
        } catch (Exception e) {
            // no error;
            return;
        }
        switch (errorID) {
        case 11:
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // -Wrong download or containerLink
        case 13:
            // username or password wrong
            throw new PluginException(LinkStatus.ERROR_PREMIUM);
        case 15:// This file requires VIP account
            throw new PluginException(LinkStatus.ERROR_PREMIUM);
        case 17:// VIP Account expired
            throw new PluginException(LinkStatus.ERROR_PREMIUM);

        case 19:// INvalid Link (like 11 but not the same
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 21:// File has been deleted
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 23:// File has been abused
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 25:// Serverfehler:Link zur Datei nicht gefunden
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 2 * 60 * 1000l);
        case 27:// Servererror: FILE NOT FOUND
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 2 * 60 * 1000l);

        case 29:// No ACCESS to read this file
            // download limit reached
            try {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(params[1]) * 1000l);
            } catch (NumberFormatException e) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);

            } catch (ArrayIndexOutOfBoundsException e) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);

            }

        case 31:// Damaged file
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        this.requestFileInformation(downloadLink);
        String hash = new Regex(downloadLink.getDownloadURL().toLowerCase(), "hash=([0-9a-f]{32})").getMatch(0);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, "http://api.sharesystems.de/?action=jd_free&hash=" + hash, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            parseError(br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
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
}
