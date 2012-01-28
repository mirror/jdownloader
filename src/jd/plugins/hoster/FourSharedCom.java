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
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.RandomUserAgent;
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
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4shared.com" }, urls = { "https?://(www\\.)?4shared(\\-china)?\\.com/(account/)?(download|get|file|document|photo|video|audio|mp3|office|rar|zip|archive)/.+?/.*" }, flags = { 2 })
public class FourSharedCom extends PluginForHost {
    private static String       agent        = RandomUserAgent.generate();

    private static final String PASSWORDTEXT = "enter a password to access";

    public FourSharedCom(final PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.4shared.com/ref/14368016/1");
        setConfigElements();
    }

    private static final String DOWNLOADSTREAMS = "DOWNLOADSTREAMS";

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.viajd", ".com").replaceFirst("https:", "http:"));
        if (link.getDownloadURL().contains(".com/download")) {
            boolean fixLink = true;
            try {
                final Browser br = new Browser();
                br.getHeaders().put("User-Agent", agent);
                br.setFollowRedirects(false);
                br.getPage(link.getDownloadURL());
                final String newLink = br.getRedirectLocation();
                if (newLink != null) {
                    final String tmp = new Regex(newLink, "(.*?)(\\?|$)").getMatch(0);
                    if (tmp != null) {
                        link.setUrlDownload(tmp);
                    } else {
                        link.setUrlDownload(newLink);
                    }
                    fixLink = false;
                }
            } catch (final Throwable e) {
            }
            if (fixLink) {
                String id = new Regex(link.getDownloadURL(), ".com/download/(.*?)/").getMatch(0);
                if (id != null) {
                    link.setUrlDownload("http://www.4shared.com/file/" + id);
                }
            }
        } else {
            link.setUrlDownload(link.getDownloadURL().replaceAll("red.com/[^/]+/", "red.com/file/").replace("account/", ""));
        }

    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        br.forceDebug(true);
        login(account);
        final String redirect = br.getRegex("loginRedirect\":\"(http.*?)\"").getMatch(0);
        br.setFollowRedirects(true);
        br.getPage(redirect);
        final String[] dat = br.getRegex("Bandwidth\\:.*?<div class=\"quotacount\">(.+?)\\% of (.*?)</div>").getRow(0);
        if (dat != null && dat.length == 2) {
            ai.setTrafficMax(SizeFormatter.getSize(dat[1]));
            ai.setTrafficLeft((long) (ai.getTrafficMax() * (100.0 - Float.parseFloat(dat[0])) / 100.0));
        }
        final String accountDetails = br.getRegex("(/account/myAccount\\.jsp\\?sId=[^\"]+)").getMatch(0);
        br.getPage(accountDetails);
        final String expire = br.getRegex("<td>Expiration Date:</td>.*?<td>(.*?)<span").getMatch(0);
        ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.trim(), "yyyy-MM-dd", Locale.UK));
        String accType = br.getRegex("Account Type:</td>.*?>(.*?)<").getMatch(0);
        if (accType != null) accType = accType.trim();
        if ("FREE".equalsIgnoreCase(accType)) {
            ai.setStatus("Free accounts are not supported");
            account.setValid(false);
        } else {
            ai.setStatus(accType);
            account.setValid(true);
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.4shared.com/terms.jsp";
    }

    private String getDllink() {
        String url = br.getRegex("<meta property=\"og:audio\" content=\"(http://.*?)\"").getMatch(0);
        if (url == null) {
            url = br.getRegex("var (flvLink|mp4Link|oggLink|mp3Link|streamerLink) = \\'(http://[^<>\"\\']{5,500})\\'").getMatch(1);
        }
        if (url == null) {
            /* maybe picture download */
            url = br.getRegex("\"(http://dc\\d+\\.4shared(\\-china)?\\.com/download/[A-Za-z0-9]+/(?!desktop4shared|4shared_Desktop_).*?)\"").getMatch(0);
        }
        if (url == null) {
            url = br.getRegex("linkShow.*?\\'(http://dc\\d+\\.4shared(\\-china)?\\.com/.*?)\\'").getMatch(0);
        }
        if (url == null) {
            url = br.getRegex("\\'(http://dc\\d+\\.4shared(\\-china)?\\.com/.*?)\\'").getMatch(0);
        }
        if (url == null) {
            url = br.getRegex("<div class=\"dl\">[\t\n\r ]+<a href=(\\'|\")(http://.*?)(\\'|\")").getMatch(1);
        }
        if (url == null) {
            url = br.getRegex("(\\'|\")(http://dc\\d+\\.4shared(\\-china)?\\.com/download/[a-zA-Z0-9_\\-]+/(?!desktop4shared|4shared_Desktop_).*?\\?tsid=\\d+\\-\\d+[a-z0-9\\-]+)(\\'|\")").getMatch(1);
        }
        if (url == null) {
            /* maybe directdownload */
            url = br.getRegex("startDownload.*?window\\.location.*?(http://.*?)\"").getMatch(0);
        }
        return url;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        /* better fix the plugin out of date, limit of 10 seems still to work */
        return 10;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String pass = handlePassword(downloadLink);
        boolean downloadStream = getPluginConfig().getBooleanProperty(DOWNLOADSTREAMS);
        String url = null;
        if (downloadLink.getStringProperty("streamDownloadDisabled") == null && downloadStream) {
            url = getDllink();
            /** Shouldn't happen */
            if (url != null && url.contains("4shared_Desktop_")) {
                downloadLink.setProperty("streamDownloadDisabled", "true");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
        // If file isn't available for freeusers we can still try to get the
        // streamlink
        if (br.containsHTML("In order to download files bigger that 500MB you need to login at 4shared") && url == null) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.foursharedcom.only4premium", "Files over 500MB are only downloadable for premiumusers!"));
        if (url == null) {
            url = br.getRegex("<a href=\"(http://(www\\.)?4shared(\\-china)?\\.com/get[^\\;\"]*).*?\" class=\".*?dbtn.*?\" tabindex=\"1\"").getMatch(0);
            if (url == null) url = br.getRegex("\"(http://(www\\.)?4shared(\\-china)?\\.com/get/[A-Za-z0-9\\-_]+/.*?)\"").getMatch(0);
            if (url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                br.getPage(url);
                url = getDllink();
                if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (url.contains("linkerror.jsp")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                // Ticket Time
                final String ttt = br.getRegex(" var c = (\\d+);").getMatch(0);
                int tt = 40;
                if (ttt != null) {
                    logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
                    tt = Integer.parseInt(ttt);
                }
                sleep(tt * 1000l, downloadLink);
            }
        }
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, false, 1);
        /**
         * Maybe download failed because we got a wrong directlink, disable
         * getting directlinks first, if it then fails again the correct
         * errormessage is shown
         */
        if (br.getURL().contains("401waitm") && downloadLink.getStringProperty("streamDownloadDisabled") == null) {
            downloadLink.setProperty("streamDownloadDisabled", "true");
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } else if (br.getURL().contains("401waitm") && downloadLink.getStringProperty("streamDownloadDisabled") != null) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 5 * 60 * 1000l); }
        final String error = new Regex(dl.getConnection().getURL(), "\\?error(.*)").getMatch(0);
        if (error != null) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        }
        if (!dl.getConnection().isContentDisposition() && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (downloadLink.getStringProperty("streamDownloadDisabled") == null) {
                downloadLink.setProperty("streamDownloadDisabled", "true");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            handleFreeErrors(downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (pass != null) downloadLink.setProperty("pass", pass);
        dl.startDownload();
    }

    private void handleFreeErrors(DownloadLink downloadLink) throws PluginException {
        if (br.containsHTML("(Servers Upgrade|4shared servers are currently undergoing a short-time maintenance)")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l); }
        String ttt = br.getRegex(" var c = (\\d+);").getMatch(0);
        if (ttt != null) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 5 * 60 * 1000l); }
    }

    private String handlePassword(DownloadLink link) throws Exception {
        String pass = link.getStringProperty("pass");
        if (br.containsHTML(PASSWORDTEXT)) {
            Form pwform = br.getFormbyProperty("name", "theForm");
            if (pwform == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            if (pass == null) {
                pass = getUserInput(null, link);
            }
            pwform.put("userPass2", pass);
            br.submitForm(pwform);
            if (br.containsHTML(PASSWORDTEXT)) {
                link.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, "Password wrong");
            }
        }
        return pass;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        String pass = handlePassword(downloadLink);
        // direct download or not?
        String link = br.getRedirectLocation();
        if (link == null) link = getDllink();
        if (link == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 0);
        final String error = new Regex(dl.getConnection().getURL(), "\\?error(.*)").getMatch(0);
        if (error != null) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        }
        if (!dl.getConnection().isContentDisposition() && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(Servers Upgrade|4shared servers are currently undergoing a short-time maintenance)")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (pass != null) downloadLink.setProperty("pass", pass);
        dl.startDownload();
    }

    public void login(final Account account) throws IOException, PluginException {
        setBrowserExclusive();
        br.getHeaders().put("User-Agent", agent);
        br.getPage("http://www.4shared.com/");
        br.setCookie("http://www.4shared.com", "4langcookie", "en");
        br.postPage("http://www.4shared.com/login", "callback=jsonp" + System.currentTimeMillis() + "&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=false&doNotRedirect=true");
        final String premlogin = br.getCookie("http://www.4shared.com", "premiumLogin");
        if (premlogin == null || !premlogin.contains("true")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        if (br.getCookie("http://www.4shared.com", "Password") == null || br.getCookie("http://www.4shared.com", "Login") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        try {
            setBrowserExclusive();
            br.getHeaders().put("User-Agent", agent);
            br.setCookie("http://www.4shared.com", "4langcookie", "en");
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());
            // need password?
            if (br.containsHTML(PASSWORDTEXT)) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.foursharedcom.passwordprotected", "This link is password protected"));
            if (br.containsHTML("The file link that you requested is not valid")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            String filename = br.getRegex("title\" content=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex(Pattern.compile("id=\"fileNameTextSpan\">(.*?)</span>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<title>(.*?) \\- 4shared\\.com \\- online file sharing and storage \\- download</title>").getMatch(0);
                    if (filename == null) filename = br.getRegex("<h1 id=\"fileNameText\">(.*?)</h1>").getMatch(0);
                }
            }
            String size = br.getRegex("<td class=\"finforight lgraybox\" style=\"border-top:1px #dddddd solid\">([0-9,]+ [a-zA-Z]+)</td>").getMatch(0);
            if (size == null) {
                size = br.getRegex("<span title=\"Size: (.*?)\">").getMatch(0);
            }
            if (filename == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            /** Server sometimes sends bad filenames */
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            if (size != null) {
                downloadLink.setDownloadSize(SizeFormatter.getSize(size.replace(",", "")));
            }
            return AvailableStatus.TRUE;
        } catch (final Exception e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FourSharedCom.DOWNLOADSTREAMS, JDL.L("plugins.hoster.foursharedcom.downloadstreams", "Download video/audio streams (faster download but this can decrease audio/video quality)")).setDefaultValue(false));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}