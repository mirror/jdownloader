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
import jd.http.RandomUserAgent;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freakshare.net" }, urls = { "http://[\\w\\.]*?freakshare\\.(net|com)/file(s/|/)[\\w]+/(.*)" }, flags = { 2 })
public class Freaksharenet extends PluginForHost {

    public Freaksharenet(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(100l);
        this.enablePremium("http://freakshare.com/shop.html");
        setConfigElements();
    }

    private boolean             NOPREMIUM          = false;
    private static final String WAIT1              = "WAIT1";
    private int                 MAXPREMDLS         = -1;
    private static final String MAXDLSLIMITMESSAGE = "Sorry, you cant download more then";

    private void setConfigElements() {
        ConfigEntry cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), WAIT1, JDL.L("plugins.hoster.Freaksharenet.waitInsteadOfReconnect", "Wait 10 minutes instead of reconnecting")).setDefaultValue(true);
        config.addEntry(cond);
    }

    @Override
    public String getAGBLink() {
        return "http://freakshare.com/?x=faq";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("freakshare.net", "freakshare.com"));
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");/* workaround for buggy server */
        br.setFollowRedirects(false);
        /*
         * set english language in phpsession because we have no cookie for that
         */
        br.getPage("http://freakshare.com/?language=US");
        br.postPage("http://freakshare.com/login.html", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&submit=Login");
        if (br.getCookie("http://freakshare.com", "login") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://freakshare.com/");
        if (!br.containsHTML("<td><b>Member \\(free\\)</b></td>") && !br.containsHTML("<td><b>Member \\(premium\\)</b></td>")) {
            logger.info("JD couldn't find out the membership of this account!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (!br.containsHTML("<td><b>Member \\(premium\\)</b></td>")) {
            NOPREMIUM = true;
            MAXPREMDLS = 1;
        }
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
        String usedSpace = br.getRegex(">Used Space:</td>[\n\r\t ]+<td>(.*?) of ").getMatch(0);
        if (usedSpace != null) ai.setUsedSpace(usedSpace.toLowerCase().replace("yte", ""));
        String points = br.getRegex(">Points:</td>[\n\r\t ]+<td>(\\d+)</td>").getMatch(0);
        if (points != null) ai.setPremiumPoints(points);
        String hostedFiles = br.getRegex(">Hosted Files:</td>[\n\r\t ]+<td>(\\d+)</td>").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Integer.parseInt(hostedFiles));
        if (!NOPREMIUM) {
            String left = br.getRegex(">Traffic left:</td>.*?<td>(.*?)</td>").getMatch(0);
            if (left != null) {
                ai.setTrafficLeft(left);
            }
            String validUntil = br.getRegex(">valid until:</td>.*?<td><b>(.*?)</b></td>").getMatch(0);
            if (validUntil == null) {
                account.setValid(false);
            } else {
                ai.setValidUntil(Regex.getMilliSeconds(validUntil, "dd.MM.yyyy - HH:mm", null));
                account.setValid(true);
            }
            ai.setStatus("Premium User");
        } else {
            ai.setStatus("Registered (free) User");
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        if (NOPREMIUM) {
            doFree(downloadLink);
        } else {
            String url = null;
            if (br.getRedirectLocation() == null) {
                if (br.containsHTML("No Downloadserver. Please try again")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No Downloadserver. Please try again later", 15 * 60 * 1000l);
                if (br.containsHTML("Traffic is used up for today")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                Form form = br.getForm(0);
                if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                br.submitForm(form);
                url = br.getRedirectLocation();
            } else {
                url = br.getRedirectLocation();
            }
            if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
            dl.startDownload();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        /*
         * set english language in phpsession
         */
        br.getPage("http://freakshare.com/?language=US");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("We are back soon")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        if (br.containsHTML("(Sorry but this File is not avaible|Sorry, this Download doesnt exist anymore)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No Downloadserver\\. Please try again")) return AvailableStatus.UNCHECKABLE;
        String filename = br.getRegex("\"box_heading\" style=\"text-align:center;\">(.*?)- .*?</h1>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        String filesize = br.getRegex("\"box_heading\" style=\"text-align:center;\">.*?- (.*?)</h1>").getMatch(0);
        if (filesize != null) downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(DownloadLink downloadLink) throws Exception {
        final boolean resume = false;
        final int maxchunks = 1;
        boolean waitReconnecttime = getPluginConfig().getBooleanProperty(WAIT1, false);
        if (br.containsHTML("your Traffic is used up for today")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001);
        if (br.containsHTML("You can Download only 1 File in")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1001);
        if (br.containsHTML("No Downloadserver\\. Please try again")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No Downloadserver. Please try again later", 15 * 60 * 1000l);
        br.setFollowRedirects(false);
        Form form = br.getForm(1);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // waittime
        String ttt = br.getRegex("var time = (\\d+)\\.[0-9];").getMatch(0);
        int tt = 0;
        if (ttt != null) tt = Integer.parseInt(ttt);
        if (tt > 180) {
            if (waitReconnecttime && tt < 701) {
                sleep((tt + 2) * 1001l, downloadLink);
            } else {
                // 10 Minutes reconnect-waittime is not enough, let's wait one
                // hour
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
            }
        }
        if (!waitReconnecttime) sleep((tt + 2) * 1001l, downloadLink);
        br.submitForm(form);
        form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (br.containsHTML("api\\.recaptcha\\.net")) {
            for (int i = 0; i <= 5; i++) {
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.parse();
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                rc.setCode(c);
                if (br.containsHTML(MAXDLSLIMITMESSAGE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Reached max free DLs", 10 * 60 * 1000l);
                if (br.getRedirectLocation() == null) continue;
                break;
            }
            if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), resume, maxchunks);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, resume, maxchunks);
        }
        if (!dl.getConnection().isContentDisposition()) {
            logger.info("The finallink is no file, trying to handle errors...s");
            br.followConnection();
            if (br.containsHTML(MAXDLSLIMITMESSAGE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Reached max free DLs", 10 * 60 * 1000l);
            if (br.containsHTML("File can not be found")) {
                logger.info("File for the following is offline (server error): " + downloadLink.getDownloadURL());
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
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
    public int getMaxSimultanPremiumDownloadNum() {
        return MAXPREMDLS;
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
