//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.UserAgents;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.mozilla.javascript.ConsString;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgsrc.ru" }, urls = { "https?://decryptedimgsrc\\.ru/[^/]+/\\d+\\.html(\\?pwd=[a-z0-9]{32})?" })
public class ImgSrcRu extends PluginForHost {
    // DEV NOTES
    // drop requests on too much traffic, I suspect at the firewall on connection.
    private String                         ddlink    = null;
    private static AtomicReference<String> userAgent = new AtomicReference<String>(null);
    private static AtomicInteger           uaInt     = new AtomicInteger(0);

    public ImgSrcRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 250);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("decryptedimgsrc", "imgsrc"));
    }

    @Override
    public String getAGBLink() {
        return "https://imgsrc.ru/main/dudes.php";
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return false;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return false;
        }
        return false;
    }

    public Browser prepBrowser(Browser prepBr, Boolean neu) {
        if (neu) {
            String refer = prepBr.getHeaders().get("Referer");
            prepBr = new Browser();
            prepBr.getHeaders().put("Referer", refer);
        }
        prepBr.setFollowRedirects(true);
        if (uaInt.incrementAndGet() > 25 || userAgent.get() == null || neu) {
            userAgent.set(UserAgents.stringUserAgent());
            uaInt.set(0);
        }
        prepBr.getHeaders().put("User-Agent", userAgent.get());
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        prepBr.setCookie(this.getHost(), "iamlegal", "yeah");
        prepBr.setCookie(this.getHost(), "lang", "en");
        prepBr.setCookie(this.getHost(), "per_page", "48");
        return prepBr;
    }

    /**
     * because stable is lame!
     */
    public void setBrowser(final Browser ibr) {
        this.br = ibr;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        prepBrowser(br, false);
        final String r = downloadLink.getStringProperty("Referer", null);
        if (r != null) {
            br.getHeaders().put("Referer", r);
        }
        getPage(downloadLink.getDownloadURL(), downloadLink);
        getDllink();
        if (ddlink != null) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(ddlink);
                if (con.getContentType().contains("html")) {
                    downloadLink.setAvailable(false);
                    return AvailableStatus.FALSE;
                }
                String filename = getFileNameFromHeader(con);
                String oldname = new Regex(downloadLink.getDownloadURL(), "(\\d+)\\.html").getMatch(0);
                downloadLink.setFinalFileName(oldname + filename.substring(filename.lastIndexOf(".")));
                downloadLink.setDownloadSize(con.getContentLength());
                return AvailableStatus.TRUE;
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private void getDllink() {
        try {
            boolean done = false;
            String js = br.getRegex(".+<script(?: type=(\"|')text/javascript\\1)?>.*?\\s*((?:var|let) [a-z]=[^<]+.*?)</script>.+").getMatch(1);
            Object result = null;
            String t = null;
            if (js != null) {
                String[] var = new Regex(js, "((?:(?:var|let)\\s*)?(\\w+)\\s*=\\s*document\\.getElementById\\('?([\\w_]+)'?\\)\\.(\\w+)?,)").getRow(0);
                String replaced = null;
                if (var == null) {
                    final String var_e[] = new Regex(js, "(?:var|let)\\s*(\\w+)\\s*=\\s*'(.*?)'\\s*,").getRow(0);
                    if (var_e != null) {
                        replaced = var_e[0];
                        js = js.replaceFirst("getElementById\\(" + Pattern.quote(var_e[0]) + "\\)", "getElementById(" + var_e[1] + ").src");
                        js = js.replaceFirst("d=", "var q=");
                        js = js.replaceAll("String\\.fromCodePoint", "String.fromCharCode");// different method names Javascript<->Java
                        js = js.replaceFirst("q=d.src,", "");
                    }
                    var = new Regex(js, "((?:(?:var|let)\\s*)?(\\w+)\\s*=\\s*document\\.getElementById\\('?([\\w_]+)'?\\)\\.(\\w+)?,)").getRow(0);
                }
                String varres = br.getRegex("<[^>]+'" + Pattern.quote(var[2]) + "'[^>]*>").getMatch(-1);// find image tag with correct id
                if (varres == null) {
                    // within js as var
                    t = new Regex(js, var[2] + "='?(.*?)'?[,;]").getMatch(0);
                    if (t != null) {
                        varres = br.getRegex("<[^>]+'" + Pattern.quote(t) + "'[^>]*>").getMatch(-1);
                    }
                }
                final String varsrc = new Regex(varres, var[3] + "=('|\")(.*?)\\1").getMatch(1);// parse src from image tag
                js = js.replace(var[0], "");// set by engine.put(key,value)
                // they are now referencing o+elementid(e) for original, and big is just elementid(e)
                final String[] qual;
                if (replaced != null) {
                    qual = new String[] { /* original */"o'\\s*\\+\\s*" + var[2], "o'\\s*\\+\\s*" + replaced, "pic_o", "ori", /* big */"pic_b", "bip", replaced, var[2] };
                } else {
                    qual = new String[] { /* original */"o'\\s*\\+\\s*" + var[2], "pic_o", "ori", /* big */"pic_b", "bip", var[2] };
                }
                for (final String q : qual) {
                    if (new Regex(js, "document.getElementById\\('?" + q + ".*?\\)").matches()) {
                        if (!done) {
                            js = js.replaceFirst("document.getElementById\\('?" + q + ".*?\\)\\.\\w+=", "var result=");
                            done = true;
                            // replace non needed
                            js = js.replaceAll("document.getElementById\\(.*?\\)[^\r\n]+", "");
                            js = js.replaceAll("\\w+\\.src\\s*=\\s*[^\r\n]+", "");
                            break;
                        }
                    }
                }
                if (!done) {
                    final String newJS = js.replaceFirst("\\w+\\.src\\s*=", "var result=");
                    if (!StringUtils.equals(js, newJS)) {
                        js = newJS;
                        done = true;
                    }
                }
                if (!done) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                while (true) {
                    try {
                        final ScriptEngineManager mgr = JavaScriptEngineFactory.getScriptEngineManager(this);
                        final ScriptEngine engine = mgr.getEngineByName("javascript");
                        engine.put(var[1], varsrc);
                        engine.eval(js);
                        result = engine.get("result");
                    } catch (final Throwable e) {
                        // my youtube approach -raztoki
                        if (e.getMessage() != null) {
                            // do not use language components of the error message. Only static identifies, otherwise other languages will
                            // fail!
                            final String ee = new Regex(e.getMessage(), "ReferenceError: \"([\\$\\w]+)\".+<Unknown source>").getMatch(0);
                            if (ee != null) {
                                // lets look for missing reference
                                final String ref = br.getRegex("var\\s+" + Pattern.quote(ee) + "\\s*=\\s*.*?;").getMatch(-1);
                                if (ref != null) {
                                    js = ref + "\r\n" + js;
                                    continue;
                                } else {
                                    logger.warning("Could not find missing var/function");
                                }
                            } else {
                                logger.warning("Could not find reference Error");
                            }
                        }
                        logger.log(e);
                    }
                    break;
                }
                if (result != null && result instanceof ConsString) {
                    ddlink = result.toString();
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        } catch (Exception e) {
            logger.log(e);
            return;
        }
    }

    private void getDllink_old() throws Exception {
        String js = br.getRegex("<script(?: type=(\"|')text/javascript\\1)?>.*?((?:\\s*var [a-z]='.*?'[;,]){1,}[^<]+)</script>").getMatch(1);
        if (js == null) {
            logger.warning("Could not find JS!");
        } else {
            String best = new Regex(js, "'(ori_?pic)'").getMatch(0);
            if (best == null) {
                best = new Regex(js, "'(big_?pic)'").getMatch(0);
                if (best == null) {
                    best = new Regex(js, "main_?pic").getMatch(-1);
                    if (best == null) {
                        logger.warning("determining best!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            String x = new Regex(js, "document\\.getElementById\\('" + best + "'\\).(\\w+)").getMatch(0);
            if (x == null) {
                if (best.contains("ori")) {
                    x = "href";
                } else {
                    x = "src";
                }
            }
            final ScriptEngineManager mgr = JavaScriptEngineFactory.getScriptEngineManager(this);
            final ScriptEngine engine = mgr.getEngineByName("javascript");
            Object result = null;
            try {
                engine.eval("var document = { getElementById: function (a) { if (!this[a]) { this[a] = new Object(); function src() { return a.src; } this[a].src = src(); } return this[a]; }};");
                engine.eval(js + "\r\nvar result=document.getElementById('" + best + "')." + x + ";");
                result = engine.get("result");
            } catch (Throwable e) {
            }
            if (result != null && result instanceof ConsString) {
                ddlink = result.toString();
            }
        }
        if (ddlink == null) {
            ddlink = br.getRegex("name=bb onclick='select\\(\\);' type=text style='\\{width:\\d+;\\}' value='\\[URL=[^<>\"]+\\]\\[IMG\\](https?://[^<>\"]*?)\\[/IMG\\]").getMatch(0);
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (ddlink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, ddlink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void getPage(String url, DownloadLink downloadLink) throws Exception {
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setAllowedResponseCodes(new int[] { 410 });
        br.getPage(url);
        if (br.getRequest().getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(">This album has not been checked by the moderators yet\\.|<u>Proceed at your own risk</u>")) {
            // /main/passcheck.php?ad=\d+ links can not br.getURL + "?warned=yeah"
            // lets look for the link
            final String yeah = br.getRegex("/[^/]+/a\\d+\\.html\\?warned=yeah").getMatch(-1);
            if (yeah != null) {
                br.getPage(yeah);
            } else {
                // fail over
                br.getPage(br.getURL() + "?warned=yeah");
            }
        }
        // needs to be before password
        if (br.containsHTML(">Album foreword:.+Continue to album >></a>")) {
            final String newLink = br.getRegex(">shortcut\\.add\\(\"Right\",function\\(\\) \\{window\\.location=\\'(https?://imgsrc\\.ru/[^<>\"\\'/]+/[a-z0-9]+\\.html(\\?pwd=([a-z0-9]{32})?)?)\\'").getMatch(0);
            if (newLink == null) {
                logger.warning("Couldn't process Album forward");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(newLink);
        }
        if (br.containsHTML(">Album owner has protected his work from unauthorized access") || br.containsHTML("enter password to continue:")) {
            Form pwForm = br.getFormbyProperty("name", "passchk");
            if (pwForm == null) {
                logger.warning("Password form finder failed!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String password = downloadLink.getStringProperty("pass");
            if (password == null) {
                password = getUserInput("Enter password for link:", downloadLink);
                if (password == null || password.equals("")) {
                    logger.info("User abored/entered blank password");
                    throw new PluginException(LinkStatus.ERROR_FATAL);
                }
            }
            pwForm.put("pwd", Encoding.urlEncode(password));
            br.submitForm(pwForm);
            pwForm = br.getFormbyProperty("name", "passchk");
            if (pwForm != null) {
                downloadLink.setProperty("pass", Property.NULL);
                password = null;
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            downloadLink.setProperty("pass", password);
        } else if (br.getURL().equals("http://imgsrc.ru/")) {
            // link has been removed!
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}