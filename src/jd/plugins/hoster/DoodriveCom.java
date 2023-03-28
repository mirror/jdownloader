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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DoodriveCom extends PluginForHost {
    public DoodriveCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("https://doodrive.com/upgrade");
    }

    @Override
    public String getAGBLink() {
        return "https://doodrive.com/terms";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "doodrive.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/f/([a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean            FREE_RESUME       = true;
    private static final int                FREE_MAXCHUNKS    = 0;
    private static final int                FREE_MAXDOWNLOADS = -1;
    private static AtomicReference<Cookies> cookies           = new AtomicReference<Cookies>(null);

    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
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

    private Browser prepBR(final Browser br) {
        br.setCookie(this.getHost(), "cookieconsent", "1");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Set fallback filename */
            link.setName(this.getFID(link));
        }
        prepBR(br);
        if (isDownload) {
            /* Download: Do not waste any time/steps as we should already know the onlinestatus by now. */
            synchronized (cookies) {
                if (cookies.get() != null) {
                    br.setCookies(cookies.get());
                }
            }
            br.getPage(link.getPluginPatternMatcher());
            parseFileInfo(link);
        } else {
            /*
             * Do first check without cookies - this way we can only find the online status of wrong fileIDs but therefore we will be able
             * to find the file info (filename & size) of offline items.
             */
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            parseFileInfo(link);
            boolean attemptSecondLinkcheck = false;
            synchronized (cookies) {
                if (cookies.get() != null) {
                    br.setCookies(cookies.get());
                    attemptSecondLinkcheck = true;
                }
            }
            if (attemptSecondLinkcheck) {
                br.getPage(link.getPluginPatternMatcher());
                parseFileInfo(link);
            }
        }
        return AvailableStatus.TRUE;
    }

    private void parseFileInfo(final DownloadLink link) throws PluginException {
        /*
         * Offline message may only appear once appropriate cookies (after captcha) are set. Some files may appear to be online at first
         * (filename and filesize given) but this errormessage will be displayed after the captcha.
         */
        if (br.containsHTML("<title>\\s*File Not Found|>\\s*The file you are trying to download is no longer available|>\\s*The file has expired>\\s*The file was deleted by")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h1 class=\"uk-h4 uk-margin-small uk-text-truncate\"[^>]*>([^<>\"]+)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex(">Name:</strong><span[^>]*>([^<>\"]+)<").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("readonly>[url=https?://[^/]+/f/[a-z0-9]+\\](.*?)\\[/url\\]").getMatch(0);
            }
        }
        String filesize = br.getRegex("Download\\s*\\((\\d+(?:\\.\\d+) [A-Za-z]+)\\)").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("File size:</strong><span[^>]*>([^<]+)<").getMatch(0);
        }
        if (filename != null) {
            link.setName(filename);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, true);
        handleDownload(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (!attemptStoredDownloadurlDownload(link, directlinkproperty, resumable, maxchunks)) {
            /* Step 1: "Download with Direct URL" + captcha */
            final Form step1 = br.getFormbyKey("f");
            if (step1 == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /*
             * This captcha should only be required if we're using the cookies of another "verified" session meaning that the user should
             * never have to solve two captchas to download one file!
             */
            if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(step1) || step1.containsHTML("class=\"recaptcha-submit\"")) {
                final String key = step1.getRegex("data-sitekey=\"([^\"]+)\"").getMatch(0);
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, key).getToken();
                step1.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            }
            br.submitForm(step1);
            /* Step 2: "Verification Required" */
            final Form step2 = br.getFormbyKey("data");
            if (step2 == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.submitForm(step2);
            /* Step 3: Quick redirect via form */
            final Form step3 = br.getFormbyKey("CSRFToken");
            if (step3 != null) {
                final InputField verify = step3.getInputField("verify");
                if (verify != null) {
                    verify.setDisabled(false);
                }
                br.submitForm(step3);
            }
            /* Step 4: "Successfully Verified" */
            final Form step4 = br.getFormbyKey("CSRFToken");
            if (step4 != null) {
                final InputField verify = step4.getInputField("verify");
                if (verify != null) {
                    verify.setDisabled(false);
                }
                br.submitForm(step4);
            }
            /* Step 5 */
            final Form step5 = br.getFormbyActionRegex(".*direct-downloader");
            if (step5 != null) {
                br.submitForm(step5);
            }
            String dllink = br.getRegex("window\\.location\\.href = \"(https?://[^\"]+)\";").getMatch(0);
            dllink = null;
            if (dllink == null) {
                dllink = br.getRegex("(https://[^/]+/d/[^\"]+)\"").getMatch(0);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl.setFilenameFix(true);
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resumable, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resumable, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
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