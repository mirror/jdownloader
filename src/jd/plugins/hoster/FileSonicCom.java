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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.Property;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
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

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesonic.com" }, urls = { "http://[\\w\\.]*?(sharingmatrix|filesonic)\\..*?/.*?file/([a-zA-Z0-9]+(/.+)?|[a-z0-9]+/[0-9]+(/.+)?|[0-9]+(/.+)?)" }, flags = { 2 })
public class FileSonicCom extends PluginForHost {

    private static final Object LOCK                = new Object();
    private static String       geoDomain           = null;

    private static final String ua                  = "Mozilla/5.0 (JD; X11; U; Linux i686; en-US; rv:1.9.0.10) Gecko/2009042523 Ubuntu/9.04 (jaunty) Firefox/3.0.10";
    private static final String uaf                 = "Mozilla/5.0 (JDF; X11; U; Linux i686; en-US; rv:1.9.0.10) Gecko/2009042523 Ubuntu/9.04 (jaunty) Firefox/3.0.10";
    private static final String uap                 = "Mozilla/5.0 (JDP; X11; U; Linux i686; en-US; rv:1.9.0.10) Gecko/2009042523 Ubuntu/9.04 (jaunty) Firefox/3.0.10";

    private static final String RECAPTCHATEXT       = "Recaptcha\\.create";

    private static final String COLLABORATE         = "COLLABORATE";
    private static final String DLYOURFILESUSERTEXT = "You can only download files which YOU uploaded!";

    public FileSonicCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.filesonic.com/premium");
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (true) {
            for (DownloadLink aLink : urls) {
                aLink.getLinkStatus().setStatusText(DLYOURFILESUSERTEXT);
                aLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
            }
            return true;
        }
        if (urls == null || urls.length == 0) { return false; }
        try {
            final Browser br = new Browser();
            br.getHeaders().put("User-Agent", ua);
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
                try {
                    br.setCustomCharset("UTF-8");
                } catch (final Throwable e) {
                }
                br.postPage("http://api.filesonic.com/link?method=getInfo", sb.toString());
                for (final DownloadLink dllink : links) {
                    final String id = this.getPureID(dllink);
                    final String hit = br.getRegex("link><id>" + id + "(.*?)</link>").getMatch(0);
                    if (hit == null || hit.contains("status>NOT_AVAILABLE")) {
                        dllink.setAvailable(false);
                    } else {
                        String name = new Regex(hit, "filename>(.*?)</filename").getMatch(0);
                        if (name.startsWith("<![CDATA")) {
                            name = new Regex(name, "CDATA\\[(.*?)\\]\\]>").getMatch(0);
                        }
                        String size = new Regex(hit, "size>(\\d+)</size").getMatch(0);
                        dllink.setAvailable(true);
                        dllink.setFinalFileName(name);
                        if (size != null) dllink.setDownloadSize(Long.parseLong(size));
                        if ("1".equals(new Regex(hit, "<is_collaborate>(1)</is_collaborate>").getMatch(0))) {
                            dllink.setProperty(COLLABORATE, true);
                        } else {
                            dllink.setProperty(COLLABORATE, Property.NULL);
                        }
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

    private String downloadAPI(Browser useBr, Account account, DownloadLink link) throws IOException, PluginException {
        Browser br = useBr;
        if (br == null) br = new Browser();
        br.getHeaders().put("User-Agent", uap);
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
        if (br.containsHTML("Free user can not download files")) { throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filesonic.largefree", "Free user can not download files over 400MB")); }
        if (br.containsHTML("Download session in progress")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.filesonic.inprogress", "Download session in progress"), 10 * 60 * 1000l); }
        if (br.containsHTML("This file is password protected")) {
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
        }
        if (br.containsHTML("(<h1>404 Not Found</h1>|<title>404 Not Found</title>|An Error Occurred)")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.filesonic.servererror", "Server error"), 20 * 60 * 1000l); }
        if (br.containsHTML("This file is available for premium users only\\.")) { throw new PluginException(LinkStatus.ERROR_FATAL, "Premium only file. Buy Premium Account"); }
        if (br.containsHTML(">We have detected some suspicious behaviour coming from your IP address \\([\\d\\.]+\\) and have been temporarily blocked")) { throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filesonic.ipabuse", "IP blocked due to client abuse. Please address your problem and then try again")); }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        synchronized (FileSonicCom.LOCK) {
            AccountInfo ai = new AccountInfo();
            try {
                loginAPI(br, account, true);
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
                long exp = TimeFormatter.getMilliSeconds(expiredate.trim(), "yyyy-MM-dd HH:mm:ss", null);
                if (exp - System.currentTimeMillis() > 365 * 24 * 60 * 60 * 1000l) {
                    exp = -1;
                }
                ai.setValidUntil(exp);
                long valid = ai.getValidUntil();
                if (ai.isExpired()) {
                    /*
                     * workaround for recurring payments and delayed payment, we
                     * add one more day
                     */
                    ai.setExpired(false);
                    valid = System.currentTimeMillis() + 24 * 60 * 60 * 1000l;
                    ai.setValidUntil(valid);
                }
                return ai;
            }
            account.setValid(false);
            return ai;
        }
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
                br.getHeaders().put("User-Agent", ua);
                br.setCookie(defaultDomain, "lang", "en");
                br.setFollowRedirects(false);
                br.getPage(defaultDomain);
                geoDomain = br.getRedirectLocation();
                if (geoDomain == null) {
                    geoDomain = defaultDomain;
                } else {
                    String domain = new Regex(br.getRedirectLocation(), "https?://.*?(filesonic\\..*?)(/|$)").getMatch(0);
                    if (domain == null) {
                        logger.severe("getDomain: failed(2) " + br.getRedirectLocation() + " " + br.toString());
                        geoDomain = defaultDomain;
                    } else {
                        geoDomain = "http://www." + domain;
                    }
                }
            }
        } catch (final Throwable e) {
            logger.info("getDomain: failed(1)" + e.toString());
            geoDomain = defaultDomain;
        }
        return geoDomain;
    }

    private synchronized String getDomainAPI() {
        try {
            Browser br = new Browser();
            br.getHeaders().put("User-Agent", ua);
            br.setFollowRedirects(true);
            br.getPage("http://api.filesonic.com/utility?method=getFilesonicDomainForCurrentIp");
            String domain = br.getRegex("response>.*?filesonic(\\..*?)</resp").getMatch(0);
            if (domain != null) {
                String check = "http://www.filesonic" + domain;
                br.setFollowRedirects(false);
                br.getPage(check);
                if (br.getRedirectLocation() != null) {
                    String ret = new Regex(br.getRedirectLocation(), "(https?://.*?)(/|$)").getMatch(0);
                    if (ret != null) {
                        logger.info("Filesonic: DomainMisMatch " + domain + " ->" + ret);
                        // return ret;
                    }
                    // return br.getRedirectLocation();
                }
                return check;
            }
        } catch (final Throwable e) {
            logger.severe(e.getMessage());
        }
        return null;
    }

    private String getDownloadURL(Browser br) {
        if (br == null) return null;
        String ret = br.getRegex("<p><a href=\"(http://[^<]*?\\.filesonic\\..*?[^<]*?)\"><span>Start download now!</span></a></p>").getMatch(0);
        if (ret == null) ret = br.getRegex("<p><a id=\"start_download_link\" href=\"(http://[^<]*?\\.filesonic\\..*?[^<]*?)\"><span>Start download now!</span></a></p>").getMatch(0);
        if (ret == null) ret = br.getRegex(">Your download is ready<.*?href=\"(http://[^<]*?\\.filesonic\\..*?[^<]*?)\"").getMatch(0);
        return ret;

    }

    public String getID(final DownloadLink link) {
        String id = new Regex(link.getDownloadURL(), "/file/([a-zA-Z0-9]+(/.+)?)").getMatch(0);
        if (id == null || id.startsWith("r")) {
            String id2 = new Regex(link.getDownloadURL(), "/file/r[a-z0-9]+/([0-9]+(/.+)?)").getMatch(0);
            if (id2 != null) id = id2;
        }
        if (id == null) {
            id = new Regex(link.getDownloadURL(), "/file/([0-9]+(/.+)?)").getMatch(0);
        }
        return id;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public String getPureID(final DownloadLink link) {
        /* new format, Hashes */
        String id = new Regex(link.getDownloadURL(), "/file/([a-zA-Z0-9]+)").getMatch(0);
        if (id == null || id.startsWith("r")) {
            /* hash + old format, not very common */
            String id2 = new Regex(link.getDownloadURL(), "/file/r[a-z0-9]+/([0-9]+)").getMatch(0);
            if (id2 != null) id = id2;
        }
        if (id == null) {
            /* fileID */
            id = new Regex(link.getDownloadURL(), "/file/([0-9]+)").getMatch(0);
        }
        return id;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    public String handleFreeJson(final DownloadLink downloadLink) throws Exception {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (JDF-DEV; X11; U; Linux i686; en-US; rv:1.9.0.10) Gecko/2009042523 Ubuntu/9.04 (jaunty) Firefox/3.0.10");
        String fileID = getPureID(downloadLink);
        br.setFollowRedirects(true);
        br.getPage(this.getDomain() + "/file/" + fileID + "?start=1");
        HashMap<String, String> step = parseJSon(br);
        if ("ON_PAGE_TIMER".equalsIgnoreCase(step.get("result"))) {
            /* we have to wait some time */
            String timer = step.get("timer");
            long time = 30 * 1000l;
            if (timer != null) time = Long.parseLong(timer) * 1000l;
            sleep(time, downloadLink);
            step.remove("result");
            step.remove("timer");
            Form form = new Form();
            form.setAction(this.getDomain() + "/file/" + fileID + "?start=1");
            form.setMethod(MethodType.POST);
            for (String key : step.keySet()) {
                form.put(key, step.get(key));
            }
            br.submitForm(form);
        }
        step = parseJSon(br);
        if ("PASSWORD".equalsIgnoreCase(step.get("result"))) {
            /* password handling */
            String passCode = null;
            int tries = 0;
            while ("PASSWORD".equalsIgnoreCase(step.get("result"))) {
                /* password handling */
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput(null, downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                Form form = new Form();
                form.setAction(this.getDomain() + "/file/" + fileID + "?start=1");
                form.setMethod(MethodType.POST);
                form.put("passwd", Encoding.urlEncode(passCode));
                br.submitForm(form);
                if (tries++ >= 5) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Password Missing"); }
                step = parseJSon(br);
            }
            if (passCode != null) {
                downloadLink.setProperty("pass", passCode);
            }
        }
        if ("CAPTCHA".equalsIgnoreCase(step.get("result"))) {
            /* captcha handling */
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            // TODO: Remove after MAJOR (NIGHTLY) update
            final String id = step.get("captchapublickey");
            rc.setId(id);
            Form rcForm = new Form();
            rcForm.setMethod(MethodType.POST);
            rcForm.setAction(this.getDomain() + "/file/" + fileID + "?start=1");
            rc.setForm(rcForm);
            rc.load();
            for (int i = 0; i <= 5; i++) {
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                rc.setCode(c);
                step = parseJSon(br);
                if ("CAPTCHA".equalsIgnoreCase(step.get("result"))) {
                    rc.reload();
                    continue;
                }
                break;
            }
        }
        String url = step.get("link");
        if (url != null) {
            if (url.startsWith("http")) return url;
            return "http://" + url;
        }
        if ("Timer".equalsIgnoreCase(step.get("result"))) {
            String timer = step.get("countdowndelay");
            long time = 15 * 60 * 1000l;
            if (timer != null) time = Long.parseLong(timer) * 1000l;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, time);
        }
        if ("ERROR_OVER_SIZE".equalsIgnoreCase(step.get("result"))) { throw new PluginException(LinkStatus.ERROR_FATAL, "Premium only file. Buy Premium Account"); }
        if ("ERROR_PREMIUM_ONLY".equalsIgnoreCase(step.get("result"))) { throw new PluginException(LinkStatus.ERROR_FATAL, "Premium only file. Buy Premium Account"); }
        if ("ERROR_PARALLEL".equalsIgnoreCase(step.get("result"))) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP already loading!", 15 * 60 * 1000l); }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private HashMap<String, String> parseJSon(Browser br) {
        if (br == null) return null;
        HashMap<String, String> r = new HashMap<String, String>();
        String rets[][] = br.getRegex("\"(.*?)\":(\")?(.*?)(\"|,|}|$| )").getMatches();
        if (rets == null) return null;
        for (String[] ret : rets) {
            r.put(ret[0].toLowerCase(Locale.ENGLISH), ret[2].replaceAll("\\\\/", "/"));
        }
        return r;
    }

    // @Override, to keep Stable compatibility
    public boolean bypassMaxSimultanDownloadNum(DownloadLink link, Account acc) {
        return Boolean.TRUE.equals(link.getProperty(COLLABORATE, Property.NULL));
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        if (true) throw new PluginException(LinkStatus.ERROR_FATAL, DLYOURFILESUSERTEXT);
        String downloadUrl = null;
        String passCode = null;
        passCode = null;
        br.getHeaders().put("User-Agent", uaf);
        this.br.setCookiesExclusive(false);
        this.br.forceDebug(true);
        this.requestFileInformation(downloadLink);
        boolean collaborate = bypassMaxSimultanDownloadNum(downloadLink, null);
        // we have to enter captcha before we get ip_blocked_state
        // we do this timeing check to avoid this
        br.setCookie(getDomain(), "lang", "en");
        if (collaborate) {
            /* collaborate support, no PW support yet */
            br.setFollowRedirects(false);
            String fileID = getPureID(downloadLink);
            br.getPage(this.getDomain() + "/file/" + fileID);
            if (this.br.getRedirectLocation() != null) {
                this.br.getPage(this.br.getRedirectLocation());
            }
            downloadUrl = getDownloadURL(br);
            if (downloadUrl == null) {
                /* Collaborate seems broken,remove the property */
                downloadLink.setProperty(COLLABORATE, Property.NULL);
                logger.severe("Collaborate File and no URL");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                /* once PW support is added, we should retry here */
                // throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
        if (downloadUrl == null) downloadUrl = handleFreeJson(downloadLink);
        if (downloadUrl == null) {
            /* fallback to normal api */
            this.br.getPage(downloadLink.getDownloadURL());
            if (this.br.getRedirectLocation() != null) {
                this.br.getPage(this.br.getRedirectLocation());
            }
            passCode = null;

            final String freeDownloadLink = this.br.getRegex(".*href=\"(.*?start=1.*?)\"").getMatch(0);
            // this is an ajax call
            Browser ajax = this.br.cloneBrowser();
            ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            ajax.getPage(freeDownloadLink);

            this.errorHandling(downloadLink, ajax);
            this.br.setFollowRedirects(true);
            // download is ready already
            downloadUrl = getDownloadURL(ajax);
            if (downloadUrl == null) {

                // downloadUrl =
                // this.br.getRegex("downloadUrl = \"(http://.*?)\"").getMatch(0);
                // if (downloadUrl == null) { throw new
                // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                if (ajax.containsHTML("This file is available for premium users only.")) {

                throw new PluginException(LinkStatus.ERROR_FATAL, "Premium only file. Buy Premium Account"); }
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
                    ajax = this.br.cloneBrowser();
                    ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
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
                    ajax = this.br.cloneBrowser();
                    ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    ajax.submitForm(form);
                    if (tries++ >= 5) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Password Missing"); }

                }
                if (ajax.containsHTML(RECAPTCHATEXT)) {
                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(ajax);
                    // TODO: Remove after MAJOR (NIGHTLY) update
                    final String id = ajax.getRegex("Recaptcha\\.create\\(\"([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
                    rc.setId(id);
                    Form rcForm = new Form();
                    rcForm.setMethod(MethodType.POST);
                    rcForm.setAction(freeDownloadLink);
                    rc.setForm(rcForm);
                    rc.load();
                    for (int i = 0; i <= 5; i++) {
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c = getCaptchaCode(cf, downloadLink);
                        rc.setCode(c);
                        if (ajax.containsHTML(RECAPTCHATEXT)) {
                            rc.reload();
                            continue;
                        }
                        break;
                    }
                    if (ajax.containsHTML(RECAPTCHATEXT)) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
                }

                downloadUrl = getDownloadURL(ajax);
            }
            if (downloadUrl == null) {
                logger.severe(ajax.toString());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        /*
         * limited to 1 chunk at the moment cause don't know if its a server
         * error that more are possible and resume should also not work ;)
         */
        br.setFollowRedirects(true);
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, downloadUrl, true, collaborate ? 0 : 1);
        if (this.dl.getConnection().getResponseCode() == 404) {
            this.dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
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
        br.setFollowRedirects(true);
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, url, true, 0);
        if (this.dl.getConnection().getResponseCode() == 404) {
            this.dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (this.dl.getConnection() != null && this.dl.getConnection().getContentType() != null && (this.dl.getConnection().getContentType().contains("html") || this.dl.getConnection().getContentType().contains("unknown"))) {
            this.br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    private String loginAPI(Browser useBr, Account account, boolean showMessageDialog) throws IOException, PluginException {
        Browser br = useBr;
        if (br == null) br = new Browser();
        br.getHeaders().put("User-Agent", uap);
        br.setFollowRedirects(true);
        String page = br.getPage("http://api.filesonic.com/user?method=getInfo&u=" + Encoding.urlEncode(account.getUser()) + "&p=" + Encoding.urlEncode(account.getPass()) + "&format=xml");
        String premium = br.getRegex("is_premium>(.*?)</is_").getMatch(0);
        if (!"1".equals(premium)) {
            if (showMessageDialog) {
                if (br.containsHTML("Login failed. Please check username or password")) {
                    UserIO.getInstance().requestMessageDialog(0, "Filesonic Premium Error", "Login failed. Please check username or password!");
                } else if ("0".equalsIgnoreCase(premium)) {
                    UserIO.getInstance().requestMessageDialog(0, "Filesonic Premium Error", "This account has no premium status!");
                }
            }
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        return page;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (true) {
            downloadLink.getLinkStatus().setStatusText(DLYOURFILESUSERTEXT);
            return AvailableStatus.UNCHECKABLE;
        }
        this.correctDownloadLink(downloadLink);
        this.checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) return AvailableStatus.UNCHECKED;
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        this.correctDownloadLink(link);
    }

    @Override
    public void resetPluginGlobals() {
    }

}