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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class BookFiOrg extends antiDDoSForHost {
    // DEV NOTES
    // they share the same template
    // hosted on different IP ranges
    public BookFiOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://" + this.getHost() + "/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private static final String TYPE_MD5 = "https?://[^/]+/md5/([a-f0-9]{32})$";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "b-ok.cc", "b-ok.org", "art1lib.org", "art1lib.com", "bookfi.net", "bookfi.org", "bookshome.net", "bookshome.org", "booksc.org", "booksc.xyz", "booksc.eu", "booksc.me", "bookzz.org", "de1lib.org", "zlibrary.org", "libsolutions.net", "pt1lib.org", "1lib.eu", "1lib.org", "2lib.org", "b-ok.xyz", "b-ok.global", "3lib.net", "4lib.org", "eu1lib.org", "1lib.limited", "1lib.education", "1lib.to", "1lib.pl", "1lib.vip" });
        return ret;
    }

    private static final ArrayList<String> getDeadDomains() {
        /**
         * Collect dead domains so we know when we have to alter the domain of added URLs! </br> KEEP THIS LIST UP2DATE!!
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

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        if (link.getPluginPatternMatcher() != null) {
            final ArrayList<String> deadDomains = getDeadDomains();
            final String domain = Browser.getHost(link.getPluginPatternMatcher(), true);
            if (deadDomains.contains(domain)) {
                link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceFirst(org.appwork.utils.Regex.escape(domain), this.getHost()));
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        final String parameter = link.getPluginPatternMatcher();
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getURL().contains("redirectUrl")) {
            /* Redirect to other domain based on GEO-location/IP */
            logger.info("Redirect to another domain required");
            final String redirect = br.getRegex("location\\.href = '//' \\+ domain \\+ '(/book/[^\\']+)'").getMatch(0);
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
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"notFound") || br.containsHTML(">\\s*If you did not find the book or it was closed")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (parameter.matches(TYPE_MD5)) {
            link.setMD5Hash(new Regex(parameter, TYPE_MD5).getMatch(0));
        }
        /* We expect a redirect here */
        if (parameter.contains("/md5/") && br.getURL().contains("/md5/")) {
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
        final String filesizeStr = br.getRegex("(?i)class=\"glyphicon glyphicon-download-alt\"[^>]*></span>\\s*[^\\(]+\\s*\\([a-z0-9]+,\\s*(\\d+[^<>\"\\'\\)]+)\\)").getMatch(0);
        if (filesizeStr != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesizeStr));
        }
        final String title = PluginJSonUtils.getJson(br, "title");
        if (!StringUtils.isEmpty(title)) {
            String ext = br.getRegex("<i class=\"zlibicon-download\"[^>]*></i>\\s*[^<]*\\(([a-z0-9]+),\\s*\\d+").getMatch(0);
            if (ext == null) {
                ext = "djvu";
            }
            link.setName(title + "." + ext);
        }
        // if (parameter.contains("/md5/")) {
        // // now everything is ok, we should correct to a single url/file uid
        // param.setUrlDownload(br.getURL());
        // }
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
            /* 2021-10-07: Different domain(and/or IP) = different language --> Try to cover multiple languages here */
            if (br.containsHTML("(?i)There are more th(?:e|a)n \\d+ downloads from |ACHTUNG: Es gibt mehr als \\d+ Downloads von Ihrer IP")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily downloadlimit reached");
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
