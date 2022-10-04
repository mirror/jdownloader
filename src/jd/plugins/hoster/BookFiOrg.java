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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class BookFiOrg extends antiDDoSForHost {
    public BookFiOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void init() {
        final ArrayList<String> deadDomains = this.getDeadDomains();
        for (final String[] domainlist : getPluginDomains()) {
            for (final String domain : domainlist) {
                if (!deadDomains.contains(domain)) {
                    Browser.setRequestIntervalLimitGlobal(domain, true, 1000);
                }
            }
        }
    }

    @Override
    public String getAGBLink() {
        return "http://" + this.getHost() + "/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private final String PATTERN_URL_MD5    = "https?://[^/]+/md5/([A-Fa-f0-9]{32})$";
    private final String PATTERN_URL_NORMAL = "https?://[^/]+/(book|dl)/(\\d+(/[a-z0-9]+)?)$";
    private final String PROPERTY_ISBN13    = "ISBN13";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "b-ok.cc", "b-ok.org", "art1lib.org", "art1lib.com", "bookfi.net", "bookfi.org", "bookshome.net", "bookshome.org", "booksc.org", "booksc.xyz", "booksc.eu", "booksc.me", "bookzz.org", "de1lib.org", "zlibrary.org", "libsolutions.net", "pt1lib.org", "1lib.eu", "1lib.org", "2lib.org", "b-ok.xyz", "b-ok.global", "3lib.net", "4lib.org", "eu1lib.org", "1lib.limited", "1lib.education", "1lib.to", "1lib.pl", "1lib.vip", "1lib.domains" });
        return ret;
    }

    private static final ArrayList<String> getDeadDomains() {
        /**
         * Collect dead domains so we know when we have to alter the domain of added URLs! </br>
         * KEEP THIS LIST UP2DATE!!
         */
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("bookfi.org");
        deadDomains.add("bookfi.net");
        deadDomains.add("bookzz.org");
        deadDomains.add("1lib.eu");
        deadDomains.add("1lib.org");
        deadDomains.add("2lib.org");
        deadDomains.add("4lib.org");
        return deadDomains;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:[a-z]+\\.)?" + buildHostsPatternPart(domains) + "/((book|dl)/\\d+(/[a-z0-9]+)?|md5/[A-F0-9]{32})");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String rewriteHost(String host) {
        /* 2021-10-05: bookifi.net --> b-ok.cc */
        if (host == null || host.equalsIgnoreCase("bookfi.net")) {
            return this.getHost();
        } else {
            return super.rewriteHost(host);
        }
    }

    private String getContentURL(final DownloadLink link) {
        final ArrayList<String> deadDomains = getDeadDomains();
        final String domain = Browser.getHost(link.getPluginPatternMatcher(), true);
        if (deadDomains.contains(domain)) {
            return link.getPluginPatternMatcher().replaceFirst(Pattern.quote(domain), this.getHost());
        }
        /* Return original link */
        return link.getPluginPatternMatcher();
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        final String isbn13 = link.getStringProperty(PROPERTY_ISBN13);
        if (isbn13 != null) {
            return isbn13;
        }
        final Regex md5url = new Regex(link.getPluginPatternMatcher(), PATTERN_URL_MD5);
        if (md5url.matches()) {
            return md5url.getMatch(0);
        } else {
            return new Regex(link.getPluginPatternMatcher(), PATTERN_URL_NORMAL).getMatch(1);
        }
    }

    @Override
    public String getMirrorID(final DownloadLink link) {
        String fid = null;
        if (link != null && StringUtils.equals(getHost(), link.getHost()) && (fid = getFID(link)) != null) {
            return getHost() + "://" + fid;
        } else {
            return super.getMirrorID(link);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String url = getContentURL(link);
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        getPage(url);
        if (br.getURL().contains("redirectUrl")) {
            /* Redirect to other domain based on GEO-location/IP */
            logger.info("Redirect to another domain required");
            String redirect = br.getRegex("redirectWithCounting\\('redirector', '//' \\+ domain \\+ '(/book/\\d+/[a-z0-9]+)'\\)").getMatch(0);
            if (redirect == null) {
                redirect = new Regex(br.getURL(), "(/book/.+)").getMatch(0);
            }
            final String allDomainsJs = br.getRegex("const domains = (\\[[^\\]]+\\]);").getMatch(0);
            if (redirect == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (allDomainsJs == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final List<String> ressourcelist = (List<String>) JavaScriptEngineFactory.jsonToJavaObject(allDomainsJs);
            final String redirectURL = "https://" + ressourcelist.get(0) + redirect;
            logger.info("Redirect to: " + redirectURL);
            getPage(redirectURL);
        }
        /* TODO: Add support for more languages */
        if (br.containsHTML("(?i)Zu viele Anfragen. Bitte versuchen Sie es später noch einmal.")) {
            /* 2022-10-04: They're returning this errormessage in plaintext */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many requests", 30 * 60 * 1000l);
        } else if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"notFound") || br.containsHTML(">\\s*If you did not find the book or it was closed")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)Diese Buch wurde entfernt")) {
            /* TODO: Add support for other languages */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex md5url = new Regex(url, PATTERN_URL_MD5);
        if (md5url.matches()) {
            link.setMD5Hash(md5url.getMatch(0));
        }
        final String isbn13 = br.getRegex("data-isbn=\"(\\d+)\"").getMatch(0);
        if (isbn13 != null) {
            link.setProperty(PROPERTY_ISBN13, isbn13);
        }
        /* We expect a redirect here */
        if (url.contains("/md5/") && br.getURL().contains("/md5/")) {
            // bookfi
            String bookid = br.getRegex("<a href=\"/?(book/\\d+)\".*?</a>\\s*</?h3").getMatch(0);
            if (bookid == null) {
                // bookos && bookzz
                bookid = br.getRegex("<a href=\"/?(book/\\d+/[a-z0-9]+)\".*?</a>\\s*</?h3").getMatch(0);
                if (bookid == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            getPage("/" + bookid);
        }
        /* Try to make this work language independant because website language is determined by domain and/or IP! */
        String filesizeStr = br.getRegex("(?i)class=\"glyphicon glyphicon-download-alt\"[^>]*></span>\\s*[^\\(]+\\s*\\([a-z0-9]+,\\s*(\\d+[^<>\"\\'\\)]+)\\)").getMatch(0);
        if (filesizeStr == null) {
            filesizeStr = br.getRegex("(?:>\\s*|\\(\\s*|\"\\s*|\\[\\s*|\\s+)([0-9\\.]+(?:\\s+|\\&nbsp;)?(TB|GB|MB|KB)(?!ps|/s|\\w|\\s*Storage|\\s*Disk|\\s*Space))").getMatch(0);
        }
        if (filesizeStr != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesizeStr));
        }
        final String title = PluginJSonUtils.getJson(br, "title");
        if (!StringUtils.isEmpty(title)) {
            final String ext = br.getRegex("class=\"book-property__extension\">([^<]+)<").getMatch(0);
            if (ext != null) {
                link.setName(title + "." + ext.trim());
            } else {
                logger.warning("Failed to find file-extension");
                link.setName(title);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        String dllink = br.getRegex("(/dl/[^<>\"\\']+)").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (br.containsHTML("class=\"download-limits-error")) {
                /* Typically max 5 files per day for free-users */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily downloadlimit reached", 30 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }
}
