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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesonic.com" }, urls = { "http://[\\w\\.]*?(sharingmatrix|filesonic)\\.(com|net|jp|tw)/.*?file/([0-9]+(/.+)?|[a-z0-9]+/[0-9]+(/.+)?)" }, flags = { 2 })
public class FileSonicCom extends PluginForHost {

    private static final Object LOCK = new Object();
    private static long LAST_FREE_DOWNLOAD = 0l;
    private static String geoDomain = null;
    private static String geoDomainEnd = null;

    public FileSonicCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.filesonic.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://www.filesonic.com/terms-and-conditions";
    }

    private synchronized String getDomain() {
        if (geoDomain != null) return geoDomain;
        String defaultDomain = "http://www.filesonic.com";
        try {
            geoDomain = getDomainAPI();
            if (geoDomain == null) {
                Browser br = new Browser();
                br.setCookie(defaultDomain, "lang", "en");
                br.setFollowRedirects(false);
                br.getPage(defaultDomain);
                geoDomain = br.getRedirectLocation();
                if (geoDomain == null) {
                    geoDomain = defaultDomain;
                } else {
                    String domain = new Regex(br.getRedirectLocation(), "http://.*?(filesonic\\..*?)/").getMatch(0);
                    geoDomain = "http://www." + domain;
                }
            }
        } catch (final Throwable e) {
            geoDomain = defaultDomain;
        }
        geoDomainEnd = new Regex(geoDomain, ".*?filesonic(\\..+)($|/)").getMatch(0);
        return geoDomain;
    }

    private synchronized String getDomainEnd() {
        if (geoDomainEnd != null) return geoDomainEnd;
        getDomain();
        return geoDomainEnd;
    }

    private synchronized String getDomainAPI() {
        try {
            Browser br = new Browser();
            br.setFollowRedirects(true);
            br.getPage("http://api.filesonic.com/utility?method=getFilesonicDomainForCurrentIp");
            String domain = br.getRegex("response>.*?filesonic(\\..*?)</resp").getMatch(0);
            if (domain != null) { return "http://www.filesonic" + domain; }
        } catch (final Throwable e) {
            logger.severe(e.getMessage());
        }
        return null;
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            final Browser br = new Browser();
            br.setCookie(getDomain(), "lang", "en");
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 80 links at once */
                    if (index == urls.length || links.size() > 80) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("ids=");
                int c = 0;
                for (final DownloadLink dl : links) {
                    if (c > 0) {
                        sb.append(",");
                    }
                    sb.append(getPureID(dl));
                    c++;
                }
                br.postPage("http://api.filesonic.com/link?method=getInfo", sb.toString());
                for (final DownloadLink dllink : links) {
                    final String id = this.getPureID(dllink);
                    final String hit = br.getRegex("link><id>" + id + "(.*?)</link>").getMatch(0);
                    if (hit == null || hit.contains("status>NOT_AVAILABLE")) {
                        dllink.setAvailable(false);
                    } else {
                        String name = new Regex(hit, "filename>(.*?)</filename").getMatch(0);
                        String size = new Regex(hit, "size>(\\d+)</size").getMatch(0);
                        dllink.setAvailable(true);
                        dllink.setFinalFileName(name);
                        if (size != null) dllink.setDownloadSize(Long.parseLong(size));
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    /* converts id and filename */
    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* convert sharingmatrix to filesonic that set english language */
        link.setUrlDownload(getDomain() + "/file/" + getID(link));
    }

    public String getID(final DownloadLink link) {
        String id = new Regex(link.getDownloadURL(), "/file/([0-9]+(/.+)?)").getMatch(0);
        if (id == null) {
            id = new Regex(link.getDownloadURL(), "/file/[a-z0-9]+/([0-9]+(/.+)?)").getMatch(0);
        }
        return id;
    }

    public String getPureID(final DownloadLink link) {
        String id = new Regex(link.getDownloadURL(), "/file/([0-9]+)").getMatch(0);
        if (id == null) {
            id = new Regex(link.getDownloadURL(), "/file/[a-z0-9]+/([0-9]+)").getMatch(0);
        }
        return id;
    }

    private void errorHandling(final DownloadLink downloadLink, final Browser br) throws PluginException {
        if (br.containsHTML("The file that you're trying to download is larger than")) {
            final String size = br.getRegex("trying to download is larger than (.*?)\\. <a href=\"").getMatch(0);
            if (size != null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, JDL.LF("plugins.hoster.filesonic.onlypremiumsize", "Only premium users can download files larger than %s.", size.trim()));
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filesonic.onlypremium", "Only downloadable for premium users!"));
            }
        }

        if (br.containsHTML("Free users may only download 1 file at a time")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.filesonic.alreadyloading", "This IP is already downloading"), 5 * 60 * 1000l); }

        if (br.containsHTML("Free user can not download files")) {
            //
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filesonic.largefree", "Free user can not download files over 400MB"));
        }
        if (br.containsHTML("Download session in progress")) {
            //
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.filesonic.inprogress", "Download session in progress"), 10 * 60 * 1000l);
        }
        if (br.containsHTML("This file is password protected")) {
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
        }
        if (br.containsHTML("An Error Occurred")) {

            //
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.filesonic.servererror", "Server error"), 20 * 60 * 1000l);
        }
    }

    private String loginAPI(Browser useBr, Account account) throws IOException, PluginException {
        Browser br = useBr;
        if (br == null) br = new Browser();
        br.setFollowRedirects(true);
        String page = br.getPage("http://api.filesonic.com/user?method=getInfo&u=" + Encoding.urlEncode(account.getUser()) + "&p=" + Encoding.urlEncode(account.getPass()) + "&format=xml");
        String premium = br.getRegex("is_premium>(.*?)</is_").getMatch(0);
        if (!"1".equals(premium)) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        return page;
    }

    private String downloadAPI(Browser useBr, Account account, DownloadLink link) throws IOException, PluginException {
        Browser br = useBr;
        if (br == null) br = new Browser();
        br.setFollowRedirects(true);
        String pw = "";
        String pwUsw = link.getStringProperty("pass", null);
        if (pwUsw != null) {
            pw = "&passwords[" + this.getPureID(link) + "]=" + Encoding.urlEncode(pwUsw);
        }
        String page = br.getPage("http://api.filesonic.com/link?method=getDownloadLink&u=" + Encoding.urlEncode(account.getUser()) + "&p=" + Encoding.urlEncode(account.getPass()) + "&format=xml&ids=" + this.getPureID(link) + pw);
        if (page.contains("FSApi_Auth_Exception")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        String status = br.getRegex("status>(.*?)</status").getMatch(0);
        if ("NOT_AVAILABLE".equals(status)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return page;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        synchronized (FileSonicCom.LOCK) {
            AccountInfo ai = new AccountInfo();
            try {
                loginAPI(br, account);
            } catch (final IOException e) {
                account.setValid(true);
                account.setTempDisabled(true);
                ai.setStatus("ServerError, will retry later");
                return ai;
            } catch (final Throwable e) {
                ai.setStatus("Account invalid! Try Email as Login and don't use special chars in PW!");
                account.setValid(false);
                return ai;
            }
            final String expiredate = br.getRegex("expiration>(.*?)</premium").getMatch(0);
            if (expiredate != null) {
                ai.setStatus("Premium User");
                account.setValid(true);
                ai.setUnlimitedTraffic();
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expiredate.trim(), "yyyy-MM-dd HH:mm:ss", null));
                return ai;
            }
            account.setValid(false);
            return ai;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        String downloadUrl = null;
        String passCode = null;
        passCode = null;
        this.br.setCookiesExclusive(false);

        this.br.forceDebug(true);
        // we have to enter captcha before we get ip_blocked_state
        // we do this timeing check to avoid this
        final long waited = System.currentTimeMillis() - FileSonicCom.LAST_FREE_DOWNLOAD;
        if (FileSonicCom.LAST_FREE_DOWNLOAD > 0 && waited < 300000) {
            FileSonicCom.LAST_FREE_DOWNLOAD = 0;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 600000 - waited);
        }
        this.requestFileInformation(downloadLink);
        passCode = null;

        final String freeDownloadLink = this.br.getRegex(".*href=\"(.*?start=1.*?)\"").getMatch(0);
        // this is an ajax call
        final Browser ajax = this.br.cloneBrowser();
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ajax.postPage(freeDownloadLink, "");

        this.errorHandling(downloadLink, ajax);
        this.br.setFollowRedirects(true);
        // download is ready already
        final String re = "<p><a href=\"(http://[^<]*?\\.filesonic\\.[a-z]{2,3}[^<]*?)\"><span>Start download now!</span></a></p>";

        downloadUrl = ajax.getRegex(re).getMatch(0);
        if (downloadUrl == null) {

            // downloadUrl =
            // this.br.getRegex("downloadUrl = \"(http://.*?)\"").getMatch(0);
            // if (downloadUrl == null) { throw new
            // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            if (ajax.containsHTML("This file is available for premium users only.")) {

            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Premium only file. Buy Premium Account"); }
            final String countDownDelay = ajax.getRegex("countDownDelay = (\\d+)").getMatch(0);
            if (countDownDelay != null) {
                /*
                 * we have to wait a little longer than needed cause its not
                 * exactly the same time
                 */
                if (Long.parseLong(countDownDelay) > 300) {

                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(countDownDelay) * 1001l); }
                this.sleep(Long.parseLong(countDownDelay) * 1000, downloadLink);
                final String tm = ajax.getRegex("name='tm' value='(.*?)' />").getMatch(0);
                final String tm_hash = ajax.getRegex("name='tm_hash' value='(.*?)' />").getMatch(0);
                final Form form = new Form();
                form.setMethod(Form.MethodType.POST);
                form.setAction(downloadLink.getDownloadURL() + "?start=1");
                form.put("tm", tm);
                form.put("tm_hash", tm_hash);

                ajax.submitForm(form);
                this.errorHandling(downloadLink, ajax);
            }
            int tries = 0;
            while (ajax.containsHTML("Please Enter Password")) {
                /* password handling */
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput(null, downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                final Form form = ajax.getForm(0);
                form.put("passwd", Encoding.urlEncode(passCode));
                ajax.submitForm(form);
                if (tries++ >= 5) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Password Missing"); }

            }
            if (ajax.containsHTML("Recaptcha\\.create")) {
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(ajax);
                rc.handleAuto(this, downloadLink);
            }

            downloadUrl = ajax.getRegex(re).getMatch(0);
        }
        if (downloadUrl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        /*
         * limited to 1 chunk at the moment cause don't know if its a server
         * error that more are possible and resume should also not work ;)
         */
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, downloadUrl, false, 1);
        if (this.dl.getConnection() != null && this.dl.getConnection().getContentType() != null && (this.dl.getConnection().getContentType().contains("html") || this.dl.getConnection().getContentType().contains("unknown"))) {
            this.br.followConnection();

            this.errorHandling(downloadLink, this.br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        this.dl.setFilenameFix(true);
        this.dl.startDownload();
        FileSonicCom.LAST_FREE_DOWNLOAD = System.currentTimeMillis();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        this.setBrowserExclusive();
        this.requestFileInformation(downloadLink);
        String resp = downloadAPI(br, account, downloadLink);
        String url = new Regex(resp, "CDATA\\[(http://.*?)\\]\\]").getMatch(0);
        if (url == null) {
            /* check for needed pw */
            String status = new Regex(resp, "status>(.*?)</status").getMatch(0);
            if ("PASSWORD_REQUIRED".equals(status) || "WRONG_PASSWORD".equals(status)) {
                /* wrong pw */
                String passCode = Plugin.getUserInput(null, downloadLink);
                downloadLink.setProperty("pass", passCode);
            }
            resp = downloadAPI(br, account, downloadLink);
            url = new Regex(resp, "CDATA\\[(http://.*?)\\]\\]").getMatch(0);
        }
        if (url == null) {
            String status = new Regex(resp, "status>(.*?)</status").getMatch(0);
            if ("PASSWORD_REQUIRED".equals(status) || "WRONG_PASSWORD".equals(status)) {
                /* wrong pw */
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, "Password missing/wrong");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, url, true, 0);
        if (this.dl.getConnection() != null && this.dl.getConnection().getContentType() != null && (this.dl.getConnection().getContentType().contains("html") || this.dl.getConnection().getContentType().contains("unknown"))) {
            this.br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink parameter) throws Exception {
        this.correctDownloadLink(parameter);
        this.br.setCookie(getDomain(), "lang", "en");
        this.br.getPage(parameter.getDownloadURL());
        if (this.br.getRedirectLocation() != null) {
            this.br.getPage(this.br.getRedirectLocation());
        }
        if (this.br.containsHTML("File not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = this.br.getRegex("Filename: </span> <strong>(.*?)<").getMatch(0);
        String filesize = this.br.getRegex("<span class=\"size\">(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String otherName = this.br.getRegex("<title>Download (.*?) for").getMatch(0);
        if (otherName != null && otherName.length() > filename.length()) {
            filename = otherName;
        }
        filesize = filesize.replace("&nbsp;", "");
        parameter.setName(filename.trim());
        parameter.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
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