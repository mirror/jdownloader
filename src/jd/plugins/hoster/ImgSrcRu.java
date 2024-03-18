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

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.mozilla.javascript.ConsString;

import jd.PluginWrapper;
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
import jd.plugins.decrypter.ImgSrcRuCrawler;

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
        prepBr.setCookie(this.getHost(), "over18", "yeah"); // 2022-09-24
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

    private String getReferer(final DownloadLink link) {
        final String referOld = link.getStringProperty("Referer"); // backward compatibility
        if (referOld != null) {
            return referOld;
        } else {
            return link.getReferrerUrl();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br = prepBrowser(br, false);
        final String r = getReferer(link);
        if (r != null) {
            br.getHeaders().put("Referer", r);
        }
        getPage(link.getPluginPatternMatcher(), link);
        getDllink();
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                /* 2023-02-03: HEAD request is not supported anymore (will return error 404)! */
                con = brc.openGetConnection(dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    brc.followConnection();
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
        /* 2022-12-12 > full size image */
        dllink = br.getRegex("<a\\s*href\\s*=\\s*'([^<>\"\\']+)[^>]*\\s*'view full-sized").getMatch(0);
        if (dllink == null) {
            /* 2020-12-28 > full size image */
            dllink = br.getRegex("<a\\s*href\\s*=\\s*'([^<>\"\\']+)[^>]*>\\s*(<\\s*b\\s*>)?\\s*view full").getMatch(0);
            if (dllink == null) {
                /* 2020-11-16 > rev. 42336 */
                dllink = br.getRegex("img[^>]*class\\s*=\\s*'big'[^>]*src\\s*=\\s*'([^<>\"\\']+)").getMatch(0);
                if (dllink == null) {
                    /* 2021-02-25 */
                    dllink = br.getRegex("img[^>]*id\\s*=\\s*'bpi'[^>]*src\\s*=\\s*'([^<>\"\\']+)").getMatch(0);
                    if (dllink == null) {
                        /* 2021-06-07, click image area */
                        /* 2023-05-15, changed click image text and class='big' */
                        final String clickImage = br.getRegex(">\\s*Click the\\s*(?:<[^/>]*>)\\s*image(.*?)</a>").getMatch(0);
                        if (clickImage != null) {
                            dllink = new Regex(clickImage, "img[^>]*src\\s*=\\s*'(//[^<>\"\\']+)[^>]*class\\s*=\\s*'big'").getMatch(0);
                            if (dllink == null) {
                                dllink = new Regex(clickImage, "img[^>]*src\\s*=\\s*'(//[^<>\"\\']+)").getMatch(0);
                            }
                        }
                        if (dllink == null) {
                            dllink = br.getRegex("img[^>]*style\\s*=[^>]*src\\s*=\\s*'(//[^<>\"\\']+)").getMatch(0);
                            if (dllink == null) {
                                dllink = br.getRegex("img[^>]*src\\s*=\\s*'(//[^<>\"\\']+)[^>]*style\\s*=[^>]*").getMatch(0);
                            }
                        }
                    }
                }
            }
        }
        if (dllink == null) {
            /* Old: < rev. 42336 */
            Object result = null;
            try {
                String js = br.getRegex(".+<script(?: type=(\"|')text/javascript\\1)?>.*?\\s*((?:var|let) [a-z]=[^<]+.*?)</script>.+").getMatch(1);
                if (js != null) {
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
                }
            } catch (final Throwable e) {
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
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public static boolean isPasswordProtected(final Browser br) {
        return br.containsHTML("this album requires password\\s*<") || br.containsHTML(">\\s*Album owner\\s*(</a>)?\\s*has protected it from unauthorized access") || br.containsHTML(">\\s*Album owner\\s*(</a>)?\\s*has protected his work from unauthorized access") || br.containsHTML("enter password to continue:");
    }

    // TODO: reduce duplicated code with decrypter
    private void getPage(final String url, final DownloadLink link) throws Exception {
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setAllowedResponseCodes(new int[] { 410 });
        ImgSrcRuCrawler.getPage(br, url);
        if (br.getRequest().getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String enterOver18 = br.getRegex("(/main/warn[^<>\"\\']*over18[^<>\"\\']*)").getMatch(-1);
        if (enterOver18 != null) {
            logger.info("Entering over18 content: " + enterOver18);
            ImgSrcRuCrawler.getPage(br, enterOver18);
        } else if (br.containsHTML(">This album has not been checked by the moderators yet\\.|<u>Proceed at your own risk</u>")) {
            // /main/passcheck.php?ad=\d+ links can not br.getURL + "?warned=yeah"
            // lets look for the link
            final String yeah = br.getRegex("/[^/]+/a\\d+\\.html\\?warned=yeah").getMatch(-1);
            if (yeah != null) {
                ImgSrcRuCrawler.getPage(br, yeah);
            } else {
                // fail over
                ImgSrcRuCrawler.getPage(br, br.getURL() + "?warned=yeah");
            }
        }
        // needs to be before password
        if (br.containsHTML("Continue to album(?: >>)?")) {
            Form continueForm = br.getFormByRegex("value\\s*=\\s*'Continue");
            if (continueForm != null) {
                String password = link.getDownloadPassword();
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
                ImgSrcRuCrawler.submitForm(br, continueForm);
                if (isPasswordProtected(br)) {
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                link.setDownloadPassword(password);
            }
        }
        if (br.containsHTML(">Album foreword:.+Continue to album >></a>")) {
            final String newLink = br.getRegex(">shortcut\\.add\\(\"Right\",function\\(\\) \\{window\\.location=\\'(https?://imgsrc\\.ru/[^<>\"\\'/]+/[a-z0-9]+\\.html(\\?pwd=([a-z0-9]{32})?)?)\\'").getMatch(0);
            if (newLink == null) {
                logger.warning("Couldn't process Album forward");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ImgSrcRuCrawler.getPage(br, newLink);
        }
        if (isPasswordProtected(br)) {
            Form pwForm = br.getFormbyProperty("name", "passchk");
            if (pwForm == null) {
                logger.warning("Password form finder failed!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String password = link.getDownloadPassword();
            if (password == null) {
                password = getUserInput("Enter password for link:", link);
                if (password == null || password.equals("")) {
                    logger.info("User abored/entered blank password");
                    throw new PluginException(LinkStatus.ERROR_FATAL);
                }
            }
            pwForm.put("pwd", Encoding.urlEncode(password));
            ImgSrcRuCrawler.submitForm(br, pwForm);
            pwForm = br.getFormbyProperty("name", "passchk");
            if (pwForm != null) {
                link.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            link.setDownloadPassword(password);
        } else if (new Regex(br.getURL(), "(?i)https?://imgsrc\\.ru/$").patternFind()) {
            // link has been removed!
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}