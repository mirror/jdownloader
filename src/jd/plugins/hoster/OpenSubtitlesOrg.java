//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.OpenSubtitlesOrg.OpenSubtitlesConfig.ActionOnCaptchaRequired;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "opensubtitles.org" }, urls = { "https?://(?:www\\.)?opensubtitles\\.org/([a-z]{2})/subtitles/(\\d+)(/([\\w-]+))?" })
public class OpenSubtitlesOrg extends PluginForHost {
    public OpenSubtitlesOrg(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("https://www." + getHost() + "/support#vip");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://www." + getHost() + "/en/disclaimer";
    }

    private String getContentURL(final DownloadLink link) {
        return "https://www." + getHost() + "/en/subtitles/" + getFID(link);
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        /* Resume and chunks disabled, not needed for such small files */
        return false;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        /* Resume and chunks disabled, not needed for such small files */
        return 1;
    }

    protected static List<Cookie> antibotCookies = new ArrayList<Cookie>();

    protected void loadAntibotCookies(final Browser br) {
        synchronized (OpenSubtitlesOrg.antibotCookies) {
            for (final Cookie cookie : OpenSubtitlesOrg.antibotCookies) {
                br.setCookie(cookie);
            }
        }
    }

    private void saveAntibotCookies(final Browser br) {
        OpenSubtitlesOrg.antibotCookies.clear();
        final Cookies cookies = br.getCookies(br.getHost());
        final List<Cookie> cookieslist = cookies.getCookies();
        if (cookieslist.size() == 0) {
            /* This should never happen! */
            this.logger.warning("Failed to find any cookie");
            return;
        }
        for (final Cookie cookie : cookies.getCookies()) {
            OpenSubtitlesOrg.antibotCookies.add(cookie);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks());
        final String fid = urlinfo.getMatch(1);
        final String slug = urlinfo.getMatch(3);
        final String extDefault = ".zip";
        if (!link.isNameSet()) {
            if (slug != null) {
                link.setName(fid + "_" + slug.replace("-", " ").trim() + extDefault);
            } else {
                link.setName(fid + extDefault);
            }
        }
        this.setBrowserExclusive();
        br.setCookie(this.getHost(), "weblang", "en");
        br.getPage(this.getContentURL(link));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains(fid)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("/en/download/sub/\\d+\"><span itemprop=\"name\">([^<>\"]*?)</span>").getMatch(0);
        if (filename != null) {
            filename = fid + "_" + Encoding.htmlDecode(filename.trim()).replace("\"", "'");
        } else if (filename == null) {
            filename = fid;
        }
        filename += extDefault;
        /* 2020-06-18: Do not set final filename here! Use content-disposition final-filename! */
        link.setName(filename);
        final String filesizeBytesStr = br.getRegex("(\\d+)\\s*Bytes").getMatch(0);
        if (filesizeBytesStr != null) {
            link.setDownloadSize(Long.parseLong(filesizeBytesStr));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        synchronized (OpenSubtitlesOrg.antibotCookies) {
            boolean hasRequestedCaptchaOnce = false;
            captchaLoop: do {
                /* Load last antibot captchas in hope that we can avoid captcha. */
                this.loadAntibotCookies(br);
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, "https://dl.opensubtitles.org/en/download/sub/" + this.getFID(link), this.isResumeable(link, null), this.getMaxChunks(link, null));
                if (looksLikeDownloadableContent(dl.getConnection())) {
                    /* All okay -> Download subtitle */
                    break captchaLoop;
                }
                br.followConnection(true);
                this.dl = null;
                final ActionOnCaptchaRequired captchaaction = PluginJsonConfig.get(OpenSubtitlesConfig.class).getActionOnCaptchaRequired();
                if (!isCaptchaRequired(br)) {
                    /* No file but also no captcha required -> Unexpected state */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (hasRequestedCaptchaOnce) {
                    /* Do not ask for captcha again */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Captcha handling failed! Change IP or try again later");
                } else if (captchaaction == ActionOnCaptchaRequired.RETRY_LATER) {
                    /* Do not ask for captcha -> Wait and try again later */
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Captcha required! Change IP or try again later", 2 * 60 * 1000l);
                }
                final Form captchaForm = br.getFormbyProperty("name", "recaptcha2");
                if (captchaForm == null) {
                    /* This should never happen. */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Captcha handling failed! Change IP or try again later");
                }
                final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br) {
                    @Override
                    protected String getSiteUrl() {
                        // temp workaround else we could get redirected to a file in browser
                        return link.getPluginPatternMatcher();
                    }
                };
                final String recaptchaV2Response = rc2.getToken();
                captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                br.submitForm(captchaForm);
                hasRequestedCaptchaOnce = true;
                continue captchaLoop;
            } while (true);
            if (hasRequestedCaptchaOnce) {
                /* In hope that subsequent downloads will not run into a captcha for some time. */
                logger.info("Captcha success -> Re-use cookies for subsequent downloads");
                this.saveAntibotCookies(br);
            }
        }
        dl.startDownload();
    }

    private boolean isCaptchaRequired(final Browser br) {
        if (br.getURL().matches("(?i).+/captcha/redirect.+")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        final ActionOnCaptchaRequired captchaaction = PluginJsonConfig.get(OpenSubtitlesConfig.class).getActionOnCaptchaRequired();
        if (captchaaction == ActionOnCaptchaRequired.RETRY_LATER) {
            /*
             * User has disabled captchas -> Do not ask -> From the plugins' point of view it looks like there will never be a captcha
             * required.
             */
            return false;
        } else if (acc != null && AccountType.PREMIUM.equals(acc.getType())) {
            return false;
        } else {
            /* Captcha is not always present but can happen when user is downloading a lot of items in a short period of time. */
            return true;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        /* 2024-08-13: Prevent running into captcha unnecessarily quickly. */
        return 2;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
    // @Override
    // public String getDescription() {
    // return "Download subtitles from ";
    // }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return OpenSubtitlesConfig.class;
    }

    public static interface OpenSubtitlesConfig extends PluginConfigInterface {
        final String                                        text_ActionOnCaptchaRequired = "Action to perform when captcha is required for downloading";
        public static final OpenSubtitlesConfig.TRANSLATION TRANSLATION                  = new TRANSLATION();

        public static class TRANSLATION {
            public String getActionOnCaptchaRequired_label() {
                return "Action to perform when captcha is required for downloading";
            }
        }

        public static enum ActionOnCaptchaRequired implements LabelInterface {
            PROCESS_CAPTCHA {
                @Override
                public String getLabel() {
                    return "Ask for captcha";
                }
            },
            RETRY_LATER {
                @Override
                public String getLabel() {
                    return "Try again later";
                }
            };
        }

        @AboutConfig
        @DefaultEnumValue("PROCESS_CAPTCHA")
        @Order(10)
        @DescriptionForConfigEntry("")
        ActionOnCaptchaRequired getActionOnCaptchaRequired();

        void setActionOnCaptchaRequired(final ActionOnCaptchaRequired action);
    }
}
