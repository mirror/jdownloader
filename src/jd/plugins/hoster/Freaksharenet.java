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

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
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
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freakshare.net" }, urls = { "http://[\\w\\.]*?freakshare\\.net/file(s/|/)[\\w]+/(.*)" }, flags = { 2 })
public class Freaksharenet extends PluginForHost {

    public Freaksharenet(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(100l);
        this.enablePremium("http://freakshare.net/shop.html");
        setConfigElements();
    }

    private static final String WAIT1 = "WAIT1";

    private void setConfigElements() {
        ConfigEntry cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), WAIT1, JDL.L("plugins.hoster.Freaksharenet.waitInsteadOfReconnect", "Wait 10 minutes instead of reconnecting")).setDefaultValue(false);
        config.addEntry(cond);
    }

    @Override
    public String getAGBLink() {
        return "http://freakshare.net/?x=faq";
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");/* workaround for buggy server */
        br.setFollowRedirects(false);
        /*
         * set english language in phpsession
         */
        br.getPage("http://freakshare.net/?language=US");
        br.getPage("http://freakshare.net/login.html");
        br.postPage("http://freakshare.net/login.html", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&submit=Login");
        if (br.getCookie("http://freakshare.net", "login") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://freakshare.net/");
        if (!br.containsHTML("<td><b>Member \\(premium\\)</b></td>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String left = br.getRegex(">Traffic left:</td>.*?<td>(.*?)</td>").getMatch(0);
        ai.setTrafficLeft(left);
        String validUntil = br.getRegex(">valid until:</td>.*?<td><b>(.*?)</b></td>").getMatch(0);
        if (validUntil == null) {
            account.setValid(false);
        } else {
            ai.setValidUntil(Regex.getMilliSeconds(validUntil, "dd.MM.yyyy - HH:mm", null));
            account.setValid(true);
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        String url = null;
        if (br.getRedirectLocation() == null) {
            if (br.containsHTML("No Downloadserver. Please try again")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No Downloadserver. Please try again later", 15 * 60 * 1000l);
            Form form = br.getForm(0);
            if (form == null) {
                if (br.containsHTML("Sorry, your Traffic is used up for today")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.submitForm(form);
            url = br.getRedirectLocation();
        } else {
            url = br.getRedirectLocation();
        }
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        /*
         * set english language in phpsession
         */
        br.getPage("http://freakshare.net/?language=US");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("We are back soon")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        if (br.containsHTML("(Sorry but this File is not avaible|Sorry, this Download doesnt exist anymore)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No Downloadserver. Please try again")) return AvailableStatus.UNCHECKABLE;
        String filename = br.getRegex("\"box_heading\" style=\"text-align:center;\">(.*?)- .*?</h1>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        String filesize = br.getRegex("\"box_heading\" style=\"text-align:center;\">.*?- (.*?)</h1>").getMatch(0);
        if (filesize != null) downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        boolean waitReconnecttime = getPluginConfig().getBooleanProperty(WAIT1, false);
        requestFileInformation(downloadLink);
        if (br.containsHTML("your Traffic is used up for today")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001);
        if (br.containsHTML("You can Download only 1 File in")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1001);
        if (br.containsHTML("No Downloadserver. Please try again")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No Downloadserver. Please try again later", 15 * 60 * 1000l);
        Form form = br.getForm(1);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // waittime
        String ttt = br.getRegex("var time = (\\d+).[0-9];").getMatch(0);
        int tt = 0;
        if (ttt != null) tt = Integer.parseInt(ttt);
        if (tt > 180) {
            if (waitReconnecttime && tt < 701)
                sleep((tt + 2) * 1001l, downloadLink);
            else
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, tt * 1001l);
        }
        if (!waitReconnecttime) sleep((tt + 2) * 1001l, downloadLink);
        br.submitForm(form);
        form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (br.containsHTML("api.recaptcha.net")) {
            for (int i = 0; i <= 5; i++) {
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.parse();
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                rc.setCode(c);
                if (br.getRedirectLocation() == null) continue;
                break;
            }
            if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), false, 1);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, false, 1);
        }
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("bad try")) {
                logger.warning("Hoster said \"bad try\" which means that jd didn't wait enough time before trying to start the download!");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("your Traffic is used up for today")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001);
            if (br.containsHTML("No Downloadserver. Please try again")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No Downloadserver. Please try again later", 15 * 60 * 1000l);
            if (br.containsHTML("you cant  download more then 1 at time")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1001);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
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
