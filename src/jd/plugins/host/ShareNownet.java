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

package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class ShareNownet extends PluginForHost {

    private String captchaCode;
    private File captchaFile;
    private String downloadurl;

    public ShareNownet(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("http://share-now.net/?site=premium"); noch net
        // fertig
    }

    @Override
    public String getAGBLink() {
        return "http://share-now.net/agb.php";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        downloadurl = downloadLink.getDownloadURL();
        br.getPage(downloadurl);
        if (br.containsHTML("Datei existiert nicht oder")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String[] linkinfo = null;
        linkinfo = br.getRegex(Pattern.compile("<h3 align=\"center\"><strong>(.*?)</strong> \\(\\s*([0-9\\.]*)\\s([GKMB]*)\\s*\\) </h3>", Pattern.CASE_INSENSITIVE)).getRow(0);

        if (linkinfo == null || linkinfo.length < 2) {
            linkinfo = br.getRegex("<p><span class=\"style\\d+\">\\s*(.*?)</span>.*?<span class=\"style\\d+\">(.*?)</span>").getRow(0);
        }
        if (linkinfo == null || linkinfo.length < 2) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setDownloadSize(Regex.getSize(linkinfo[1]));
        downloadLink.setName(linkinfo[0].trim());
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        Form form = br.getForm(1);
        br.setDebug(true);
        /* gibts nen captcha? */
        if (br.containsHTML("Sicherheitscode eingeben")) {
            /* Captcha File holen */
            captchaFile = getLocalCaptchaFile(this);
            br.getDownload(captchaFile, "http://share-now.net/captcha.php?id=" + form.getInputFieldByName("download").getValue());
            /* CaptchaCode holen */
            captchaCode = Plugin.getCaptchaCode(captchaFile, this, downloadLink);
            form.put("Submit", "Download+Now");
            form.put("captcha", captchaCode);
        }
        /* DownloadLink holen/Captcha check */
        dl = br.openDownload(downloadLink, form);
        if (!dl.getConnection().isContentDisposition() || dl.getRequest().getLocation() != null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (dl.getConnection().isContentDisposition() && dl.getConnection().getLongContentLength() == 0) throw new PluginException(LinkStatus.ERROR_FATAL, "Server Error");
        /* Datei herunterladen */
        dl.startDownload();
    }

    private void login(Account account) throws IOException, PluginException {
        setBrowserExclusive();
        br.getPage("http://share-now.net/?lang=de");
        br.postPage("http://share-now.net/?lang=de", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&loginuser=1");
        if (br.getCookie("http://share-now.net", "user") == null) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
        if (br.getCookie("http://share-now.net", "pass") == null) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    private void isExpired(Account account) throws IOException, PluginException {
        br.getPage("http://netload.in/index.php?id=2");
        String validUntil = br.getRegex("Verbleibender Zeitraum</div>.*?<div style=.*?><span style=.*?>(.*?)</span></div>").getMatch(0).trim();
        if (validUntil != null && new Regex(validUntil.trim(), "kein").matches()) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
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
        res += new Date().getTime();
        ai.setValidUntil(res);
        return ai;
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}