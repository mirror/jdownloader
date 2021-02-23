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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.mozilla.javascript.ConsString;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgsrc.ru" }, urls = { "https?://decryptedimgsrc\\.ru/[^/]+/\\d+\\.html(\\?pwd=[a-z0-9]{32})?" })
public class ImgSrcRu extends PluginForHost {
    // DEV NOTES
    // drop requests on too much traffic, I suspect at the firewall on connection.
    private String                         dllink    = null;
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

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        /* 2020-11-16: No captchas at all */
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br = prepBrowser(br, false);
        final String r = link.getStringProperty("Referer", null);
        if (r != null) {
            br.getHeaders().put("Referer", r);
        }
        getPage(link.getPluginPatternMatcher(), link);
        getDllink();
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    String filename = getFileNameFromHeader(con);
                    String oldname = new Regex(link.getDownloadURL(), "(\\d+)\\.html").getMatch(0);
                    link.setFinalFileName(oldname + filename.substring(filename.lastIndexOf(".")));
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return AvailableStatus.TRUE;
                }
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
        /* 2020-12-28 > full size image */
        dllink = br.getRegex("<a\\s*href\\s*=\\s*'([^<>\"\\']+)[^>]*>\\s*(<\\s*b\\s*>)?\\s*view full").getMatch(0);
        if (dllink == null) {
            /* 2020-11-16 > rev. 42336 */
            dllink = br.getRegex("img[^>]*class\\s*=\\s*'big'[^>]*src\\s*=\\s*'([^<>\"\\']+)").getMatch(0);
        }
        if (dllink == null) {
            /* Old: < rev. 42336 */
            Object result = null;
            try {
                String js = br.getRegex(".+<script(?: type=(\"|')text/javascript\\1)?>.*?\\s*((?:var|let) [a-z]=[^<]+.*?)</script>.+").getMatch(1);
                js = js.replaceFirst("var n=new Image.*?;", "").replaceFirst("n\\.src.*?;", "").replaceAll("Mousetrap.*?;\\}\\s*\\)\\s*;", "");
                String elementName = new Regex(js, "(?:var|let) [a-z]=(\"|')(.+?)\\1").getMatch(1);
                String imageTag = br.getRegex("<[^>]+'" + Pattern.quote(elementName) + "'[^>]*>").getMatch(-1);
                String varSrc = new Regex(imageTag, "src=(\"|')(.+?)\\1").getMatch(1);
                StringBuilder sb = new StringBuilder();
                sb.append("var element = {src: elementSrc, href: ''}, document={getElementById:function(e){return element}};");
                sb.append("String.fromCodePoint = function (cp) {return String.fromCharCode(cp);};");
                sb.append(js.replaceAll("=\\s*location\\.protocol", "=\"" + br._getURL().getProtocol() + ":\""));
                sb.append("var result=element.href === '' ? element.src : element.href;");
                try {
                    final ScriptEngineManager mgr = JavaScriptEngineFactory.getScriptEngineManager(this);
                    final ScriptEngine engine = mgr.getEngineByName("javascript");
                    engine.put("elementSrc", br.getURL(varSrc).toString());
                    engine.eval(sb.toString());
                    result = engine.get("result");
                } catch (final Throwable e) {
                    logger.log(e);
                }
                if (result != null && result instanceof ConsString) {
                    dllink = result.toString();
                }
            } catch (final Throwable e) {
                /* 2020-11-18 */
                logger.log(e);
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public static boolean isPasswordProtected(Browser br) {
        return br.containsHTML("this album requires password\\s*<") || br.containsHTML(">\\s*Album owner\\s*(</a>)?\\s*has protected it from unauthorized access") || br.containsHTML(">\\s*Album owner\\s*(</a>)?\\s*has protected his work from unauthorized access") || br.containsHTML("enter password to continue:");
    }

    // TODO: reduce duplicated code with decrypter
    private void getPage(final String url, final DownloadLink link) throws Exception {
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setAllowedResponseCodes(new int[] { 410 });
        jd.plugins.decrypter.ImgSrcRu.getPage(br, url);
        if (br.getRequest().getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String enterOver18 = br.getRegex("(/main/warn[^<>\"\\']*over18[^<>\"\\']*)").getMatch(-1);
        if (enterOver18 != null) {
            logger.info("Entering over18 content: " + enterOver18);
            jd.plugins.decrypter.ImgSrcRu.getPage(br, enterOver18);
        } else if (br.containsHTML(">This album has not been checked by the moderators yet\\.|<u>Proceed at your own risk</u>")) {
            // /main/passcheck.php?ad=\d+ links can not br.getURL + "?warned=yeah"
            // lets look for the link
            final String yeah = br.getRegex("/[^/]+/a\\d+\\.html\\?warned=yeah").getMatch(-1);
            if (yeah != null) {
                jd.plugins.decrypter.ImgSrcRu.getPage(br, yeah);
            } else {
                // fail over
                jd.plugins.decrypter.ImgSrcRu.getPage(br, br.getURL() + "?warned=yeah");
            }
        }
        // needs to be before password
        if (br.containsHTML("Continue to album(?: >>)?")) {
            Form continueForm = br.getFormByRegex("value\\s*=\\s*'Continue");
            if (continueForm != null) {
                String password = link.getStringProperty("pass");
                if (isPasswordProtected(br)) {
                    if (password == null) {
                        password = getUserInput("Enter password for link:", link);
                        if (password == null || password.equals("")) {
                            logger.info("User abored/entered blank password");
                            throw new PluginException(LinkStatus.ERROR_FATAL);
                        }
                    }
                    continueForm.put("pwd", Encoding.urlEncode(password));
                }
                jd.plugins.decrypter.ImgSrcRu.submitForm(br, continueForm);
                if (isPasswordProtected(br)) {
                    link.setProperty("pass", Property.NULL);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                link.setProperty("pass", password);
            }
        }
        if (br.containsHTML(">Album foreword:.+Continue to album >></a>")) {
            final String newLink = br.getRegex(">shortcut\\.add\\(\"Right\",function\\(\\) \\{window\\.location=\\'(https?://imgsrc\\.ru/[^<>\"\\'/]+/[a-z0-9]+\\.html(\\?pwd=([a-z0-9]{32})?)?)\\'").getMatch(0);
            if (newLink == null) {
                logger.warning("Couldn't process Album forward");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            jd.plugins.decrypter.ImgSrcRu.getPage(br, newLink);
        }
        if (isPasswordProtected(br)) {
            Form pwForm = br.getFormbyProperty("name", "passchk");
            if (pwForm == null) {
                logger.warning("Password form finder failed!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String password = link.getStringProperty("pass");
            if (password == null) {
                password = getUserInput("Enter password for link:", link);
                if (password == null || password.equals("")) {
                    logger.info("User abored/entered blank password");
                    throw new PluginException(LinkStatus.ERROR_FATAL);
                }
            }
            pwForm.put("pwd", Encoding.urlEncode(password));
            jd.plugins.decrypter.ImgSrcRu.submitForm(br, pwForm);
            pwForm = br.getFormbyProperty("name", "passchk");
            if (pwForm != null) {
                link.setProperty("pass", Property.NULL);
                password = null;
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            link.setProperty("pass", password);
        } else if (new Regex(br.getURL(), "https?://imgsrc\\.ru/$").matches()) {
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