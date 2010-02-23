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
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gigasize.com" }, urls = { "http://[\\w\\.]*?gigasize\\.com/get\\.php.*" }, flags = { 2 })
public class GigaSizeCom extends PluginForHost {

    private static final String AGB_LINK = "http://www.gigasize.com/page.php?p=terms";
    private static int simultanpremium = 1;
    private static final Object PREMLOCK = new Object();

    public String agent = "Mozilla/5.0 (Windows; U; Windows NT 6.0; chrome://global/locale/intl.properties; rv:1.8.1.12) Gecko/2008102920  Firefox/3.0.0";

    public GigaSizeCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.gigasize.com/register.php");
        setStartIntervall(5000l);
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setDebug(true);
        br.getHeaders().put("User-Agent", agent);
        br.getPage("http://www.gigasize.com/index.php?lang=de");
        Form ff = new Form();
        ff.setAction("http://www.gigasize.com/login.php");
        ff.setMethod(MethodType.POST);
        ff.put("uname", Encoding.urlEncode(account.getUser()));
        ff.put("passwd", Encoding.urlEncode(account.getPass()));
        ff.put("d", "Login");
        ff.put("login", "1");
        br.submitForm(ff);
        String cookie = br.getCookie("http://www.gigasize.com", "Cookieuser[pass]");
        if (cookie == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        cookie = br.getCookie("http://www.gigasize.com", "Cookieuser[user]");
        if (cookie == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    public boolean isPremium() throws IOException {
        br.getPage("http://www.gigasize.com/myfiles.php");
        return br.getRegex("<div class=\"logged pu\"><em class=\"png\">").matches();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (!isPremium()) {
            ai.setStatus("Free Membership");
            account.setValid(true);
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
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        boolean free = false;
        synchronized (PREMLOCK) {
            requestFileInformation(parameter);
            login(account);
            if (!this.isPremium()) {
                if (simultanpremium + 1 > 2) {
                    simultanpremium = 2;
                } else {
                    simultanpremium++;
                }
                free = true;
            } else {
                if (simultanpremium + 1 > 20) {
                    simultanpremium = 20;
                } else {
                    simultanpremium++;
                }
            }
        }
        if (free) {
            handleFree0(parameter);
            return;
        }
        br.getPage(parameter.getDownloadURL());
        br.setFollowRedirects(true);
        br.getPage("http://www.gigasize.com/form.php");
        Form download = br.getForm(0);
        if (download == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, download, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dl.startDownload()) {
            /* workaround for buggy server */
            if (parameter.getDownloadSize() < 1000) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        }
    }

    @Override
    public void handleFree(DownloadLink parameter) throws Exception {
        requestFileInformation(parameter);
        if (parameter.getAvailableStatus() == AvailableStatus.UNCHECKABLE) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        handleFree0(parameter);
    }

    public void handleFree0(DownloadLink downloadLink) throws Exception {
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(true);
        if (br.containsHTML("versuchen gerade mehr")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
        Form forms[] = br.getForms();
        Form captchaForm = null;
        for (Form form : forms) {
            if (form.getAction() != null && form.getAction().contains("formdownload.php")) {
                captchaForm = form;
                break;
            }
        }
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String captchaCode = getCaptchaCode("http://www.gigasize.com/randomImage.php", downloadLink);
        captchaForm.put("txtNumber", captchaCode);
        br.submitForm(captchaForm);
        if (br.containsHTML("YOU HAVE REACHED")) {
            String temp = br.getRegex("Please retry after\\s(\\d+)\\sMinu").getMatch(0);
            int waitTime = 60;
            if (temp != null) {
                waitTime = Integer.parseInt(temp) + 1;
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitTime * 60 * 1000l);
        }
        Form download = br.getFormbyProperty("id", "formDownload");
        if (download == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, download, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dl.startDownload()) {
            /* workaround for buggy server */
            if (downloadLink.getDownloadSize() < 1000) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        }
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.getHeaders().put("User-Agent", agent);
        br.getPage("http://www.gigasize.com/index.php?lang=de");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("has been removed because we")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Die[ ]*?Datei[ ]*?wurde[ ]*?gel")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Download-Slots sind besetzt")) {
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.gigasizecom.errors.alreadyloading", "Cannot check, because already loading file"));
            return AvailableStatus.UNCHECKABLE;
        }
        String[] dat = br.getRegex("strong>Name</strong>: <b>(.*?)</b></p>.*?<p>Gr.*? <span>(.*?)</span>").getRow(0);
        if (dat.length != 2) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(dat[0]);
        downloadLink.setDownloadSize(Regex.getSize(dat[1]));
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        synchronized (PREMLOCK) {
            return simultanpremium;
        }
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

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
