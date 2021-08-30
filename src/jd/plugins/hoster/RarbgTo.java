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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class RarbgTo extends antiDDoSForHost {
    public RarbgTo(PluginWrapper wrapper) {
        super(wrapper);
        /* 2019-01-04: Try to avoid triggering their anti-spam measures! */
        this.setStartIntervall(5 * 1000l);
    }

    @Override
    public String getAGBLink() {
        return "https://rarbg.to/";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "rarbg.to", "rarbgproxied.org" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/torrent/([a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean              FREE_RESUME        = false;
    private static final int                  FREE_MAXCHUNKS     = 1;
    /* 2021-08-30: Only increase this after updating max. simultaneous downloads handling to start one download after another! */
    private static final int                  FREE_MAXDOWNLOADS  = 1;
    protected static HashMap<String, Cookies> antiCaptchaCookies = new HashMap<String, Cookies>();

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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        loadAntiCaptchaCookies(this.br, this.getHost());
        br.setFollowRedirects(true);
        if (!link.isNameSet()) {
            /* Avoid triggering their spam protection! Also all added URLs are usually online! */
            link.setName(this.getFID(link) + ".torrent");
            return AvailableStatus.TRUE;
        }
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">\\s*No such torrent")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (isThreadDefenceActive(this.br)) {
            /* Do not handle their anti bot protection during linkcheck as it may require the user to solve a captcha. */
            // this.handleThreadDefence(link, this.br);
            return AvailableStatus.UNCHECKABLE;
        }
        String filename = br.getRegex("\\&f=([^<>\"]+)").getMatch(0);
        if (filename != null) {
            if (!filename.endsWith(".torrent")) {
                filename += ".torrent";
            }
            filename = Encoding.htmlDecode(filename).trim();
            link.setFinalFileName(filename);
        }
        return AvailableStatus.TRUE;
    }

    protected void loadAntiCaptchaCookies(final Browser prepBr, final String host) {
        synchronized (antiCaptchaCookies) {
            if (!antiCaptchaCookies.isEmpty()) {
                for (final Map.Entry<String, Cookies> cookieEntry : antiCaptchaCookies.entrySet()) {
                    final String key = cookieEntry.getKey();
                    if (key != null && key.equals(host)) {
                        try {
                            prepBr.setCookies(key, cookieEntry.getValue(), false);
                        } catch (final Throwable e) {
                        }
                    }
                }
            }
        }
    }

    private boolean isThreadDefenceActive(final Browser br) {
        return br.getURL().contains("threat_defence.php");
    }

    /**
     * Handles their anti bot protection.
     *
     * @throws Exception
     */
    private void handleThreadDefence(final DownloadLink link, final Browser br) throws Exception {
        synchronized (antiCaptchaCookies) {
            if (isThreadDefenceActive(br)) {
                if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    /* 2021-08-30: Make this fail in stable as it is not yet working! */
                    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Anti-spam was triggered", 30 * 60 * 1000l);
                }
                boolean hasSolvedAtLeastOneChallenge = false;
                if (br.containsHTML("/threat_defence_ajax\\.php\\?sk=")) {
                    final String sk = br.getRegex("var value_sk = '([a-z0-9]+)';").getMatch(0);
                    final String cid = br.getRegex("var value_c = '(\\d+)';").getMatch(0);
                    final String i = br.getRegex("var value_i = '(\\d+)';").getMatch(0);
                    final String r = br.getRegex("/threat_defence_ajax\\.php\\?sk=[^\"]+\\&r=(\\d+)'").getMatch(0);
                    final String r2 = br.getRegex("/threat_defence\\.php\\?defence=2[^>]+\\&r=(\\d+)").getMatch(0);
                    if (sk == null || cid == null || i == null || r == null || r2 == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final UrlQuery query = new UrlQuery();
                    query.add("sk", sk);
                    query.add("cid", cid);
                    query.add("i", i);
                    query.add("r", r);
                    query.add("_", Long.toString(System.currentTimeMillis()));
                    /* This will return an empty page */
                    br.getPage("/threat_defence_ajax.php?" + query.toString());
                    /* This request will either complete the challenge or ask for a captcha. */
                    this.sleep(5500l, link); // mimic browser wait
                    br.getPage("/threat_defence.php?defence=2&sk=" + sk + "&cid=" + cid + "&i=" + i + "&ref_cookie=rarbg.to&r=" + r2);
                    hasSolvedAtLeastOneChallenge = true;
                }
                final Form captchaForm = br.getFormbyActionRegex(".*/threat_defence\\.php");
                if (captchaForm != null) {
                    final String captchaURL = br.getRegex("(/threat_captcha\\.php\\?[^<>\"]+)").getMatch(0);
                    if (captchaURL == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String code = this.getCaptchaCode(captchaURL, link);
                    if (code == null || !code.matches("(?i)[a-z0-9]{5}")) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA, "Invalid captcha format");
                    }
                    captchaForm.put("solve_string", code.toUpperCase(Locale.ENGLISH));
                    br.setFollowRedirects(false);
                    br.submitForm(captchaForm);
                    final String redirect = br.getRedirectLocation();
                    if (br.containsHTML("(?i)>\\s*Wrong captcha entered")) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    } else if (redirect == null) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Anti bot protection captcha handling redirect missing");
                    } else if (!redirect.matches("https?://[^/]+/torrents\\.php\\?r=\\d+")) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Anti bot protection captcha handling invalid redirect: " + redirect);
                    } else {
                        logger.info("Successfully passed captcha challenge");
                        /* TODO: Something is missing here! It is redirecting us back to thread_defence.php! Maybe a missing cookie? */
                        br.setFollowRedirects(true);
                        br.getPage(redirect);
                        hasSolvedAtLeastOneChallenge = true;
                    }
                }
                if (!hasSolvedAtLeastOneChallenge) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Anti bot protection handling failure");
                } else if (isThreadDefenceActive(br)) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Failed to pass bot protection", 30 * 60 * 1000l);
                } else {
                    logger.info("Successfully passed bot protection --> URL: " + br.toString());
                    /* Save new cookies to prevent future anti bot challenges */
                    antiCaptchaCookies.put(this.getHost(), this.br.getCookies(this.getHost()));
                }
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, true);
        final String directlinkproperty = "free_directlink";
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            this.handleThreadDefence(link, this.br);
            dllink = br.getRegex("(/download\\.php[^<>\"]+)").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    link.removeProperty(property);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return dllink;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}