//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class GigaSizeCom extends PluginForHost {

    private static final String AGB_LINK = "http://www.gigasize.com/page.php?p=terms";
    private static int simultanpremium = 1;

    public GigaSizeCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.gigasize.com/register.php");
        setStartIntervall(5000l);
    }

    public void login(Account account) throws IOException, PluginException {
        br.postPage("http://www.gigasize.com/login.php", "uname=" + Encoding.urlEncode(account.getUser()) + "&passwd=" + Encoding.urlEncode(account.getPass()) + "&=Login&login=1");
        String cookie = br.getCookie("http://www.gigasize.com", "Cookieuser[pass]");
        if (cookie == null) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
        cookie = br.getCookie("http://www.gigasize.com", "Cookieuser[user]");
        if (cookie == null) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public boolean isPremium() throws IOException {
        br.getPage("http://www.gigasize.com/myfiles.php");
        return br.getRegex("<div class=\"logged pu\"><em class=\"png\">").matches();
    }

    @Override
    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        if (!isPremium()) {
            ai.setStatus("Free Membership");
            ai.setValid(true);
            return ai;
        }
        br.getPage("http://www.gigasize.com/myfiles.php");
        String expirein = br.getRegex("Ihr Premium Account.*?ab in(.*?)Tag.*?</p>").getMatch(0);
        String points = br.getRegex("Erworbene Gigapoints: <span>(.*?)</span>").getMatch(0);
        if (expirein != null) {
            ai.setValidUntil(System.currentTimeMillis() + (Long.parseLong(expirein.trim()) * 24 * 50 * 50 * 1000));
        }
        if (points != null) {
            ai.setPremiumPoints(points);
        }
        ai.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        DownloadLink downloadLink = (DownloadLink) parameter;
        getFileInformation(parameter);
        login(account);
        if (!this.isPremium()) {
            if (simultanpremium + 1 > 2) {
                simultanpremium = 2;
            } else {
                simultanpremium++;
            }
            handleFree0(downloadLink);
            return;
        } else {
            if (simultanpremium + 1 > 20) {
                simultanpremium = 20;
            } else {
                simultanpremium++;
            }
        }
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(true);
        br.getPage("http://www.gigasize.com/form.php");
        Form download = br.getForm(0);
        dl = br.openDownload(downloadLink, download, true, 0);
        if (!dl.getConnection().isContentDisposition()) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT); }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink parameter) throws Exception {
        getFileInformation(parameter);
        handleFree0(parameter);
    }

    public void handleFree0(DownloadLink downloadLink) throws Exception {
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(true);
        if (br.containsHTML("versuchen gerade mehr")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l); }
        Form captchaForm = br.getForm(0);
        String captchaCode = getCaptchaCode("http://www.gigasize.com/randomImage.php", downloadLink);
        captchaForm.put("txtNumber", captchaCode);
        br.submitForm(captchaForm);
        Form download = br.getFormbyID("formDownload");
        dl = br.openDownload(downloadLink, download, true, 1);
        if (!dl.getConnection().isContentDisposition()) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT); }
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Download-Slots sind besetzt")) {
            downloadLink.getLinkStatus().setStatusText("Unchecked due to already loading");
            return true;
        }
        String[] dat = br.getRegex("strong>Name</strong>: <b>(.*?)</b></p>.*?<p>Gr.*? <span>(.*?)</span>").getRow(0);
        if (dat.length != 2) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(dat[0]);
        downloadLink.setDownloadSize(Regex.getSize(dat[1]));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 800;
    }

    @Override
    public void resetPluginGlobals() {
    }
}