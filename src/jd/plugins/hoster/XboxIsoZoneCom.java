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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xboxisozone.com" }, urls = { "(http://(www\\.)?((xboxisozone|dcisozone|gcisozone|psisozone|theisozone)\\.com|romgamer\\.com/download)/dl\\-start/\\d+/(\\d+/)?|xboxisopremiumonly://.+)" }, flags = { 2 })
public class XboxIsoZoneCom extends PluginForHost {

    public XboxIsoZoneCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.theisozone.com/subscribe/");
    }

    private static final Object LOCK     = new Object();
    private final String        MAINPAGE = "http://theisozone.com/";

    public void correctDownloadLink(DownloadLink link) {
        if (!link.getDownloadURL().contains("www.")) link.setUrlDownload(link.getDownloadURL().replace("http://", "http://www."));
        link.setUrlDownload(link.getDownloadURL().replace("xboxisopremiumonly://", "http://"));
    }

    @Override
    public String getAGBLink() {
        return "http://xboxisozone.com/faq.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        if (link.getBooleanProperty("premiumonly")) {
            br.getPage(link.getDownloadURL());
            final String filesize = br.getRegex(">Premium Download</span>[\t\n\r ]+<center style=\"margin\\-top:3px;\">[\t\n\r ]+1 File download, Total size ([^<>\"]*?) <br").getMatch(0);
            final String filename = br.getRegex("<title>([^<>\"]*?) \\&bull; Xbox Isos \\&bull; Downloads @ The Iso Zone</title>").getMatch(0);
            if (filename != null) link.setName(Encoding.htmlDecode(filename.trim()));
            if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
            link.getLinkStatus().setStatusText("Only downloadable for premium users");
        } else {
            link.getLinkStatus().setStatusText("Only downloadable for freeusers");
        }
        // No available check possible because this tells the server that i
        // started a download
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (downloadLink.getBooleanProperty("premiumonly")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file is only available for Premium Members");
        }
        String mainlink = downloadLink.getStringProperty("mainlink");
        if (mainlink == null) throw new PluginException(LinkStatus.ERROR_FATAL, "mainlink missing, please delete and re-add this link to your downloadlist!");
        br.getPage(mainlink);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        final String filesize = br.getRegex("<title>Download [^<>\"/]*? ([0-9\\.,]+ MB) \\&bull;").getMatch(0);
        if (filesize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
        String rcID = br.getRegex("\\?k=([^<>\"/]+)\"").getMatch(0);
        if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        Form rcForm = new Form();
        rcForm.setMethod(MethodType.POST);
        rcForm.put("verify_code", "");
        rcForm.put("captcha", "");
        rcForm.setAction(downloadLink.getDownloadURL().replace("/dl-start/", "/dl-free/"));
        rc.setForm(rcForm);
        rc.setId(rcID);
        rc.load();
        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        String c = getCaptchaCode(cf, downloadLink);
        rc.setCode(c);
        String finallink = br.getRedirectLocation();
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (finallink.contains("/dl-start/")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (finallink.contains("/download-limit-exceeded/")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            String waittime = br.getRegex("You may download again in approximately:<b> (\\d+) Minutes").getMatch(0);
            if (waittime == null) waittime = br.getRegex("You may resume downloading in (\\d+) Minutes").getMatch(0);
            if (waittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 60 * 1001l);
            if (br.containsHTML("(<TITLE>404 Not Found</TITLE>|<H1>Not Found</H1>|<title>404 \\- Not Found</title>|<h1>404 \\- Not Found</h1>|<h1>403 \\- Forbidden</h1>|<title>403 \\- Forbidden</title>)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            if (br.containsHTML("The file you requested could not be found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.getPage("http://www.theisozone.com/");
                br.postPage("http://www.theisozone.com/sub-login/", "sub_login=&login=Login&sub_user=" + Encoding.urlEncode(account.getUser()) + "&sub_pass=" + Encoding.urlEncode(account.getPass()));
                if (!br.containsHTML("Account Status: <span style=\"color:green\">Active</span>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://www.theisozone.com/subscribers/control-panel/");
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("Next payment due on:</strong> (\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd", null));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        // We can only download "premium only" links!
        if (!link.getBooleanProperty("premiumonly")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file is only available for Free Users");
        }
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        final Form dlform = br.getFormbyProperty("class", "dl_form");
        if (dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlform, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}