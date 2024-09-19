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

import org.appwork.utils.StringUtils;

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
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceFirst("decryptedimgsrc", "imgsrc"));
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/main/dudes.php";
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        /* 2020-11-16: No captchas at all */
        return false;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "(?i)(\\d+)\\.html").getMatch(0);
    }

    public Browser prepBrowser(final Browser prepBr) {
        prepBr.setFollowRedirects(true);
        if (uaInt.incrementAndGet() > 25 || userAgent.get() == null) {
            userAgent.set(UserAgents.stringUserAgent());
            uaInt.set(0);
        }
        prepBr.getHeaders().put("User-Agent", userAgent.get());
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        prepBr.setCookie(getHost(), "iamlegal", "yeah");
        prepBr.setCookie(getHost(), "over18", "yeah"); // 2022-09-24
        prepBr.setCookie(getHost(), "lang", "en");
        prepBr.setCookie(getHost(), "per_page", "48");
        return prepBr;
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
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        prepBrowser(br);
        final String r = getReferer(link);
        if (r != null) {
            br.getHeaders().put("Referer", "https://" + getHost() + "/");
        }
        getPage(link.getPluginPatternMatcher(), link);
        // final String originalFilename = br.getRegex("fetchpriority='high' alt='([^']+)'>").getMatch(0);
        // if (originalFilename != null) {
        // link.setFinalFileName(originalFilename);
        // }
        getDllink();
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (!isDownload) {
            basicLinkCheck(br.cloneBrowser(), br.createGetRequest(dllink), link, link.getName(), ".jpeg");
        }
        return AvailableStatus.TRUE;
    }

    private void getDllink() {
        final String[] formats = new String[] { "jpeg", "webp" };
        for (final String format : formats) {
            dllink = br.getRegex("id='big-pic-" + format + "' srcset='([^<>\"']+)'").getMatch(0);
            if (dllink != null) {
                break;
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
        handleConnectionErrors(br, dl.getConnection());
        this.filenameHandling(link, dl.getConnection());
        dl.startDownload();
    }

    private void filenameHandling(final DownloadLink link, final URLConnectionAdapter con) {
        String filenameFromConnection = getFileNameFromConnection(con);
        if (filenameFromConnection == null) {
            return;
        }
        final String fileid = this.getFID(link);
        if (filenameFromConnection.contains(".")) {
            /* Remove domain from header-filename. */
            filenameFromConnection = filenameFromConnection.substring(filenameFromConnection.lastIndexOf("."));
        }
        link.setFinalFileName(fileid + filenameFromConnection);
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
        if (br.getHttpConnection().getResponseCode() == 400) {
            Browser.setRequestIntervalLimitGlobal(getHost(), 750);
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Error 400 rate limit reached", 10 * 60 * 1000l);
        } else if (br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String enterOver18 = br.getRegex("(/main/warn[^<>\"\\']*over18[^<>\"\\']*)").getMatch(-1);
        if (enterOver18 != null) {
            logger.info("Entering over18 content: " + enterOver18);
            ImgSrcRuCrawler.getPage(br, enterOver18);
        } else if (br.containsHTML(">\\s*This album has not been checked by the moderators yet\\.|<u>Proceed at your own risk\\s*</u>")) {
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
        if (br.containsHTML(">\\s*Album foreword:.+Continue to album\\s*>></a>")) {
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
                if (StringUtils.isEmpty(password)) {
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
        /*
         * 2024-03-19: Our set request-interval limit make it impossible anyways to effectively download a large amount of items at the same
         * time.
         */
        return 3;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}