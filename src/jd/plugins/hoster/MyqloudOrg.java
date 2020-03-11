//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MyqloudOrg extends XFileSharingProBasic {
    public MyqloudOrg(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-03-11: reCaptchaV2<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "myqloud.org" });
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
        return XFileSharingProBasic.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return true;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return -5;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return -5;
        } else {
            /* Free(anonymous) and unknown account type */
            return -5;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    @Override
    protected boolean isVideohoster_enforce_video_filename() {
        /* 2020-03-11: Special */
        return true;
    }

    /**
     * This does currently not work via main template. Keep it this way once it is vlear whether this can be integrated in template or not!
     */
    @Override
    protected String getDllinkViaOfficialVideoDownload(final Browser brc, final DownloadLink link, final Account account, final boolean returnFilesize) throws Exception {
        logger.info("Trying to find official video downloads");
        String dllink = null;
        /* Info in table. E.g. xvideosharing.com, watchvideo.us */
        String[] videoQualityHTMLs = new Regex(correctedBR, "<tr><td>[^\r\t\n]+download_video\\(.*?</td></tr>").getColumn(-1);
        if (videoQualityHTMLs.length == 0) {
            /* Match on line - safe attempt but this may not include filesize! */
            videoQualityHTMLs = new Regex(correctedBR, "download_video\\([^\r\t\n]+").getColumn(-1);
        }
        if (videoQualityHTMLs.length == 0) {
            /* 2020-03-11: Special */
            videoQualityHTMLs = new Regex(correctedBR, "<div class=\"badge-download high-quality.*?download icon-secondary").getColumn(-1);
        }
        /** TODO: Add quality selection: Low, Medium, Original Example: deltabit.co */
        /*
         * Internal quality identifiers highest to lowest (inside 'download_video' String): o = original, h = high, n = normal, l=low
         */
        final HashMap<String, Integer> qualityMap = new HashMap<String, Integer>();
        qualityMap.put("l", 20); // low
        qualityMap.put("n", 40); // normal
        qualityMap.put("h", 60); // high
        qualityMap.put("o", 80); // original
        long maxInternalQualityValue = 0;
        String filesizeStr = null;
        String videoQualityStr = null;
        String videoHash = null;
        String targetHTML = null;
        if (videoQualityHTMLs.length == 0) {
            logger.info("Failed to find any official video downloads");
        }
        // logger.info("Trying to find selected quality for official video download");
        logger.info("Trying to find highest quality for official video download");
        for (final String videoQualityHTML : videoQualityHTMLs) {
            final String filesizeStrTmp = new Regex(videoQualityHTML, "(([0-9\\.]+)\\s*(KB|MB|GB|TB))").getMatch(0);
            // final String vid = videoinfo.getMatch(0);
            final Regex videoinfo = new Regex(videoQualityHTML, "download_video\\('([a-z0-9]+)','([^<>\"\\']*)','([^<>\"\\']*)'");
            // final String vid = videoinfo.getMatch(0);
            /* Usually this will be 'o' standing for "original quality" */
            String videoQualityStrTmp = videoinfo.getMatch(1);
            if (videoQualityStrTmp == null) {
                /* 2020-03-11: Special */
                videoQualityStrTmp = new Regex(videoQualityHTML, "data-vmode=\"\\s*(.)\\s*\"").getMatch(0);
            }
            String videoHashTmp = videoinfo.getMatch(2);
            if (videoHashTmp == null) {
                /* 2020-03-11: Special */
                videoHashTmp = new Regex(videoQualityHTML, "data-hash=\"\\s*(.*?)\\s*\"").getMatch(0);
            }
            if (StringUtils.isEmpty(videoQualityStrTmp) || StringUtils.isEmpty(videoHashTmp)) {
                /*
                 * Possible plugin failure but let's skip bad items. Upper handling will fallback to stream download if everything fails!
                 */
                continue;
            } else if (!qualityMap.containsKey(videoQualityStrTmp)) {
                /*
                 * 2020-01-18: There shouldn't be any unknown values but we should consider allowing such in the future maybe as final
                 * fallback.
                 */
                logger.info("Skipping unknown quality: " + videoQualityStrTmp);
                continue;
            }
            final int internalQualityValueTmp = qualityMap.get(videoQualityStrTmp);
            if (internalQualityValueTmp < maxInternalQualityValue) {
                /* Only continue with qualities that are higher than the highest we found so far. */
                continue;
            }
            maxInternalQualityValue = internalQualityValueTmp;
            videoQualityStr = videoQualityStrTmp;
            videoHash = videoHashTmp;
            if (filesizeStrTmp != null) {
                /*
                 * Usually, filesize for official video downloads will be given but not in all cases. It may also happen that our upper
                 * RegEx fails e.g. for supervideo.tv.
                 */
                filesizeStr = filesizeStrTmp;
            }
            targetHTML = videoQualityHTML;
        }
        if (targetHTML == null || videoQualityStr == null || videoHash == null) {
            if (videoQualityHTMLs != null && videoQualityHTMLs.length > 0) {
                /* This should never happen */
                logger.info(String.format("Failed to find officially downloadable video quality although there are %d qualities available", videoQualityHTMLs.length));
            }
            return null;
        }
        logger.info("Selected videoquality: " + videoQualityStr);
        if (returnFilesize) {
            /* E.g. in availablecheck */
            return filesizeStr;
        }
        try {
            /* 2019-08-29: Waittime here is possible but a rare case e.g. deltabit.co */
            this.waitTime(link, System.currentTimeMillis());
            /*
             * TODO: Fix issue where first request leads to '<br><b class="err">Security error</b>' (reproduced over multiple filehosts e.g.
             * xvideosharing.com)
             */
            /* 2020-03-11: Special */
            getPage(brc, "/dl?op=download_orig_pre&id=" + this.fuid + "&mode=" + videoQualityStr + "&hash=" + videoHash);
            /* A lot of workarounds */
            final String oldHTML = br.toString();
            final String correctedBROld = this.correctedBR;
            this.correctedBR = brc.toString();
            /* Workaround: Set this htmlcode on our normal browser so captcha handling can do its job. */
            br.getRequest().setHtmlCode(brc.toString());
            checkErrors(link, account, false);
            /* 2019-08-29: This Form may sometimes be given e.g. deltabit.co */
            Form download1 = brc.getFormByInputFieldKeyValue("op", "download1");
            if (download1 == null) {
                download1 = brc.getFormbyProperty("id", "generate-download-link");
            }
            if (download1 != null) {
                /* 2020-03-11: Special: Captcha & waittime */
                final long timebefore = System.currentTimeMillis();
                handleCaptcha(link, download1);
                this.waitTime(link, timebefore);
                this.submitForm(brc, download1);
                /* Workaround */
                this.correctedBR = correctedBROld;
                br.getRequest().setHtmlCode(oldHTML);
            }
            /*
             * 2019-10-04: TODO: Unsure whether we should use the general 'getDllink' method here as it contains a lot of RegExes (e.g. for
             * streaming URLs) which are completely useless here.
             */
            dllink = this.getDllink(link, account, brc, brc.toString());
            if (StringUtils.isEmpty(dllink)) {
                /* 2019-05-30: Test - worked for: xvideosharing.com */
                dllink = new Regex(brc.toString(), "<a href=\"(https?[^\"]+)\"[^>]*>Direct Download Link</a>").getMatch(0);
            }
            if (StringUtils.isEmpty(dllink)) {
                /* 2019-08-29: Test - worked for: deltabit.co */
                dllink = regexVideoStreamDownloadURL(brc.toString());
            }
            if (StringUtils.isEmpty(dllink)) {
                logger.info("Failed to find final downloadurl");
            }
        } catch (final Throwable e) {
            e.printStackTrace();
            logger.warning("Official video download failed: Exception occured");
            /*
             * Continue via upper handling - usually videohosts will have streaming URLs available so a failure of this is not fatal for us.
             */
        }
        if (StringUtils.isEmpty(dllink)) {
            logger.warning("Failed to find dllink via official video download");
        } else {
            logger.info("Successfully found dllink via official video download");
        }
        return dllink;
    }

    @Override
    protected String regexWaittime() {
        /**
         * TODO: 2019-05-15: Try to grab the whole line which contains "id"="countdown" and then grab the waittime from inside that as it
         * would probably make this more reliable.
         */
        /* Ticket Time */
        String waitStr = super.regexWaittime();
        if (waitStr == null) {
            waitStr = new Regex(correctedBR, "id=\"[a-z0-9]+\">(\\d+)</span> seconds").getMatch(0);
        }
        return waitStr;
    }

    private final String regexVideoStreamDownloadURL(final String src) {
        String dllink = new Regex(src, Pattern.compile("(https?://[^/]+[^\"]+[a-z0-9]{60}/v\\.mp4)", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (StringUtils.isEmpty(dllink)) {
            /* Wider attempt */
            dllink = new Regex(src, Pattern.compile("\"(https?://[^/]+/[a-z0-9]{60}/[^\"]+)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        }
        return dllink;
    }
}