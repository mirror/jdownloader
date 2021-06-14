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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Set fallback filename */
            link.setName(this.getFID(link));
        }
        br.setFollowRedirects(true);
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
            Form preDlForm = br.getFormbyActionRegex(".*bot-verify");
            if (preDlForm == null) {
                /* 2021-05-06 */
                preDlForm = br.getFormbyKey("verify");
            }
            if (preDlForm != null) {
                /* Step1 */
                br.setFollowRedirects(true);
                br.getHeaders().put("Origin", "https://" + this.getHost());
                /* It may be set to null so it wouldn't be sent then */
                preDlForm.put("verify", "");
                br.submitForm(preDlForm);
                /* Step 1.1: (Optional) Redirect to fake "blog" page e.g. "gositestat.com" */
                final Form blogForm = br.getFormbyKey("doo-verify");
                if (blogForm != null) {
                    br.submitForm(blogForm);
                }
                /* Step2: Captcha & waittime */
                preDlForm = br.getFormbyKey("doo-verify");
                if (preDlForm == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                /* 2021-06-11: No waittime required anymore */
                // final long timestampBeforeCaptcha = System.currentTimeMillis();
                // int waitSeconds = 10;
                // try {
                // final Browser brc = br.cloneBrowser();
                // brc.getPage("https://" + this.br.getHost() + "/assets/js/global.js");
                // final String waitStr = brc.getRegex("time\\s*:\\s*(\\d+)").getMatch(0);
                // if (waitStr != null) {
                // waitSeconds = Integer.parseInt(waitStr);
                // }
                // } catch (final Throwable e) {
                // logger.log(e);
                // logger.warning("Failed to find pre-download-waittime in js");
                // }
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                preDlForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                /* Very important! */
                preDlForm.put("verify", "");
                // /* Substract the time the user took to solve the captcha so that time is not wasted. */
                // final long timeToWait = waitSeconds * 1001l - (System.currentTimeMillis() - timestampBeforeCaptcha);
                // this.sleep(timeToWait, link);
                br.submitForm(preDlForm);
                /* Look for final Form leading back from "external" site to doodrive --> Download */
                Form verifyOut = null;
                for (final Form form : this.br.getForms()) {
                    if (form.containsHTML("doo-verify-out")) {
                        verifyOut = form;
                        break;
                    }
                }
                if (verifyOut != null) {
                    br.submitForm(verifyOut);
                    /* 2021-06-11: Typically redirect to "/f/<fuid>?f=<someHash>" */
                } else {
                    /* Let's continue and hope this Form just wasn't required. */
                    logger.warning("Failed to find verifyOut Form");
                }
                /*
                 * Now we should have cookies that allow us to linkcheck other links without having to enter more captchas. A download
                 * captcha will still be required for each download!
                 */
                synchronized (cookies) {
                    cookies.set(br.getCookies(br.getURL()));
                }
                /* Only now can we know whether or not that file is online. */
                parseFileInfo(link);
            } else {
                logger.info("No 'verify' Form required");
            }
            /* Final step */
            final Form dlform = br.getFormbyKey("f");
            if (dlform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /*
             * This captcha should only be required if we're using the cookies of another "verified" session meaning that the user should
             * never have to solve two captchas to download one file!
             */
            if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(dlform) || dlform.containsHTML("class=\"recaptcha-submit\"")) {
                final String key = dlform.getRegex("data-sitekey=\"([^\"]+)\"").getMatch(0);
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, key).getToken();
                dlform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            }
            URLConnectionAdapter con = br.openFormConnection(dlform);
            if (!looksLikeDownloadableContent(con)) {
                br.followConnection();
                final Form form = br.getFormbyKey("data");
                if (form == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.submitForm(form);
                final String url = br.getRegex("window\\.location\\.href\\s*=\\s*\"(https?://.*?)\"").getMatch(0);
                if (url == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, resumable, maxchunks);
            } else {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, con.getRequest(), resumable, maxchunks);
            }
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
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