//    jDownloader - Downloadmanager
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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share-now.net" }, urls = { "http://[\\w\\.]*?share-now\\.net/{1,}files/\\d+-(.*?)\\.html" }, flags = { 0 })
public class ShareNownet extends PluginForHost {

    public ShareNownet(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("http://share-now.net/?site=premium"); noch net
        // fertig
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        try {
            isExpired(account);
        } catch (PluginException e) {
            ai.setExpired(true);
            return ai;
        }
        String validUntil = br.getRegex("Verbleibender Zeitraum</div>.*?<div style=.*?><span style=.*?>(.*?)</span></div>").getMatch(0).trim();
        String days = new Regex(validUntil, "([\\d]+) ?Tage").getMatch(0);
        String hours = new Regex(validUntil, "([\\d]+) ?Stunde").getMatch(0);
        long res = 0;
        if (days != null) res += Long.parseLong(days.trim()) * 24 * 60 * 60 * 1000;
        if (hours != null) res += Long.parseLong(hours.trim()) * 60 * 60 * 1000;
        res += System.currentTimeMillis();
        ai.setValidUntil(res);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://share-now.net/agb.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        Form form = br.getForm(1);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setDebug(true);
        /* gibts nen captcha? */
        if (br.containsHTML("Sicherheitscode eingeben")) {
            /* CaptchaCode holen */
            String captchaCode = getCaptchaCode("http://share-now.net/captcha.php?id=" + form.getInputFieldByName("download").getValue(), downloadLink);
            form.put("Submit", "Download+Now");
            form.put("captcha", captchaCode);
        }
        form.remove("Submit2");
        form.remove("Usenet+Download");
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form);
        if (dl.getConnection().getLongContentLength() == 0 || dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getURL().contains("download.php")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, JDL.L("plugins.hoster.sharenownet.errors.servererror", "Server Error"));
        }

        /* Datei herunterladen */
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    private void isExpired(Account account) throws IOException, PluginException {
        br.getPage("http://netload.in/index.php?id=2");
        String validUntil = br.getRegex("Verbleibender Zeitraum</div>.*?<div style=.*?><span style=.*?>(.*?)</span></div>").getMatch(0).trim();
        if (validUntil != null && new Regex(validUntil.trim(), "kein").matches()) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    private void login(Account account) throws IOException, PluginException {
        setBrowserExclusive();
        br.getPage("http://share-now.net/?lang=de");
        br.postPage("http://share-now.net/?lang=de", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&loginuser=1");
        if (br.getCookie("http://share-now.net", "user") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (br.getCookie("http://share-now.net", "pass") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Datei existiert nicht oder")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)\\| share-now\\.net </title>").getMatch(0);
        if (filename == null || filename.equals("")) filename = br.getRegex("class=\"style.*?>.*?<br />(.*?)</span>").getMatch(0);
        String filesize = br.getRegex("content=\".*?Filesize:(.*?)-").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        downloadLink.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}