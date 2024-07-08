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
import java.util.List;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.plugins.components.config.XFSConfigVideoHotlinkCc;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class HotlinkCc extends XFileSharingProBasic {
    public HotlinkCc(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    private static final String PROPERTY_account_required = "account_required";

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2021-03-08: No limits at all <br />
     * captchatype-info: 2019-05-11: null<br />
     * other: 2019-05-09: Login via username&pw not possible anymore, only via EMAIL&PASSWORD! <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "hotlink.cc", "redirect.codes" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    protected boolean supportsHEADRequestForDirecturlCheck() {
        // so we can see error messages in response
        return false;
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
            return 1;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return -10;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String apikey = getAPIKey();
        if (this.supportsAPISingleLinkcheck() && apikey != null) {
            /* API linkcheck */
            return this.requestFileInformationAPI(link, apikey);
        } else {
            /* Website linkcheck */
            /*
             * 2021-10-13: Special: Provide account for availablestatus if possible because they have special URLs that will be displayed as
             * offline for non-premium users.
             */
            final Account account = AccountController.getInstance().getValidAccount(this.getHost());
            if (account != null && isPremium(account)) {
                this.loginWebsite(link, account, false);
                return requestFileInformationWebsite(link, account, false);
            } else {
                return requestFileInformationWebsite(link, null, false);
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final String apikey = getAPIKey();
        if (this.supportsAPISingleLinkcheck() && apikey != null) {
            /* API linkcheck */
            return this.requestFileInformationAPI(link, apikey);
        } else {
            /* Website linkcheck */
            try {
                final AvailableStatus status = super.requestFileInformationWebsite(link, null, false);
                /*
                 * Let's pretend we know that this status can change: Remove premiumonly flag if we're sure that this link (status) can be
                 * viewed by anyone.
                 */
                link.removeProperty(PROPERTY_account_required);
                return status;
            } catch (final PluginException e) {
                /*
                 * Decide whether or not we want to do the extended check: If premium account is given, we can already trust the first found
                 * (offline-)status!.
                 */
                if (isPremium(account) && e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                    /**
                     * 2021-10-13: Website has special handling: Some files look like they're offline for free/free account users but we can
                     * find the real status!
                     */
                    logger.info("Checking if link is really offline or only available for premium users");
                    final AvailableStatus status = requestFileInformationWebsiteMassLinkcheckerSingle(link);
                    if (status == AvailableStatus.FALSE) {
                        logger.info("Link really is offline");
                        link.removeProperty(PROPERTY_account_required);
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else {
                        logger.info("Link is special premiumonly");
                        link.setProperty(PROPERTY_account_required, true);
                    }
                    return status;
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public Form findFormDownload2Premium(final DownloadLink downloadLink, final Account account, final Browser br) throws Exception {
        /* 2019-05-15: Special */
        Form formf1Premium = super.findFormDownload2Premium(downloadLink, account, br);
        if (formf1Premium == null) {
            /* 2021-01-26 */
            formf1Premium = this.getOfficialVideoDownloadForm(downloadLink, account, br);
        }
        fixFormF1(formf1Premium);
        return formf1Premium;
    }

    private Form fixFormF1(final Form formf1) {
        if (formf1 != null && formf1.hasInputFieldByName("dl_bittorrent")) {
            formf1.remove("dl_bittorrent");
            formf1.put("dl_bittorrent", "0");
        }
        return formf1;
    }

    @Override
    public ArrayList<String> getCleanupHTMLRegexes() {
        /*
         * 2019-05-15: Special: Return empty Array as default values will kill free mode of this plugin (important html code will get
         * removed!)
         */
        return new ArrayList<String>();
    }

    @Override
    public String regexWaittime(Browser br) {
        /* 2019-05-15: Special */
        String waitStr = super.regexWaittime(br);
        if (StringUtils.isEmpty(waitStr)) {
            waitStr = new Regex(br.getRequest().getHtmlCode(), "class=\"seconds yellow\"><b>(\\d+)</b>").getMatch(0);
            if (StringUtils.isEmpty(waitStr)) {
                waitStr = new Regex(br.getRequest().getHtmlCode(), "class=\"seconds[^\"]+\"><b>(\\d+)</b>").getMatch(0);
            }
        }
        return waitStr;
    }

    @Override
    public void doFree(final DownloadLink link, final Account account) throws Exception, PluginException {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        /* Check for special premiumonly */
        if (isPremium(account) && link.hasProperty(PROPERTY_account_required)) {
            throw new AccountRequiredException();
        }
        super.doFree(link, account);
    }

    @Override
    public String regExTrafficLeft(final Browser br) {
        String trafficleft = super.regExTrafficLeft(br);
        if (StringUtils.isEmpty(trafficleft)) {
            final String src = this.getCorrectBR(br);
            trafficleft = new Regex(src, "(?i)Traffic available today</TD></TR>\\s*?</thead>\\s*?<TR><TD><b>\\s*([^<>\"]+)\\s*</b><").getMatch(0);
            if (StringUtils.isEmpty(trafficleft)) {
                trafficleft = new Regex(src, "(?i)Download traffic available\\s*:\\s*</TD></TR>\\s*</thead>\\s*<TR><TD[^>]*><b>([^<]+)</b></TD></TR>").getMatch(0);
            }
        }
        return trafficleft;
    }

    @Override
    public boolean isPremiumOnly(final Browser br) {
        /*
         * 2020-01-30: Special because template code matches also on ">\\s*Available Only for Premium Members" which is always present in
         * their html
         */
        boolean premiumonly_filehost = br.containsHTML("( can download files up to |>\\s*Upgrade your account to download (?:larger|bigger) files|>\\s*The file you requested reached max downloads limit for Free Users|Please Buy Premium To download this file\\s*<|This file reached max downloads limit|>\\s*This file is available for Premium Users only|>File is available only for Premium users|>\\s*This file can be downloaded by)");
        if (!premiumonly_filehost) {
            /** 2021-01-28 */
            premiumonly_filehost = br.containsHTML("This video.{1,6}is available for viewing and downloading.{1,6}only for premium users");
        }
        if (!premiumonly_filehost) {
            /** 2021-01-28 */
            final Form officialVideoDownloadForm = this.getOfficialVideoDownloadForm(getDownloadLink(), null, br);
            final boolean fullStreamOnlyForPremium = br.containsHTML(">\\s*This is video preview, full video is available only for Premium");
            premiumonly_filehost = fullStreamOnlyForPremium && officialVideoDownloadForm == null;
        }
        /* 2019-05-30: Example: xvideosharing.com */
        final boolean premiumonly_videohost = br.containsHTML(">\\s*This video is available for Premium users only");
        return premiumonly_filehost || premiumonly_videohost;
    }

    @Override
    protected int getDllinkViaOfficialVideoDownloadExtraWaittimeSeconds() {
        /** 2021-01-20: Tested in premium mode by user */
        return 2;
    }

    public String[] scanInfo(final String[] fileInfo) {
        /* 2021-01-22: Prefer this as template will pickup filename without extension */
        super.scanInfo(fileInfo);
        String filename = new Regex(correctedBR, "<i class=\"glyphicon glyphicon-download\"></i>([^<>\"]+)<").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            /* 2021-03-02 */
            filename = new Regex(correctedBR, "class=\"glyphicon glyphicon-play-circle\"[^>]*></i>([^<>\"]+)<").getMatch(0);
        }
        /* 2021-04-15: Important workaround or we might set video filenames without file-extension. */
        final boolean isVideoFile = br.containsHTML(">\\s*Select quality for download video|id=\"over_player_msg\"");
        /* Do not check for Form because file could be premiumonly -> No Form present! */
        // final Form videoDL = this.getOfficialVideoDownloadForm(this.br);
        if (isVideoFile && !StringUtils.isEmpty(filename) && !filename.toLowerCase(Locale.ENGLISH).endsWith(".mp4")) {
            filename = Encoding.htmlDecode(filename).trim();
            filename = filename += ".mp4";
        }
        if (filename != null) {
            fileInfo[0] = filename;
        }
        return fileInfo;
    }

    private boolean premiumWorkaroundActive = false;

    @Override
    protected String getDllinkVideohost(DownloadLink link, Account account, Browser br, final String src) {
        final Form officialDownloadForm = this.getOfficialVideoDownloadForm(link, account, br);
        if (premiumWorkaroundActive || officialDownloadForm != null) {
            return null;
        } else {
            return super.getDllinkVideohost(link, account, br, src);
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /**
         * 2021-01-26: They're providing AES encrypted videostreaming to premium users. By default, this is the preferred download method
         * but it will fail -> This works around it
         */
        if (account.getType() == AccountType.PREMIUM) {
            premiumWorkaroundActive = true;
            try {
                super.handlePremium(link, account);
            } finally {
                premiumWorkaroundActive = false;
            }
        } else {
            premiumWorkaroundActive = false;
            super.handlePremium(link, account);
        }
    }

    @Override
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, String src) {
        /** 2021-01-26: Special */
        String dllink = new Regex(src, "href=\"(https?://[^\"]+)\"[^>]*>\\s*Direct Download Link").getMatch(0);
        if (dllink != null) {
            return dllink;
        } else {
            /* Fallback to template handling */
            return super.getDllink(link, account, br, src);
        }
    }

    /* 2021-04-12: Although most of the content hosted on this filehost is video content other file-types are also allowed! */
    // @Override
    // protected boolean isVideohoster_enforce_video_filename() {
    // /** 2021-01-27 */
    // return true;
    // }
    @Override
    protected boolean supports_availablecheck_alt() {
        /** 2021-01-28 */
        return false;
    }

    @Override
    protected boolean isVideohosterEmbedHTML(final Browser br) {
        /* 2021-03-02: Do not prefer download of potentially encrypted HLS -> Return false to force official video download */
        final Form officialDownloadForm = br == null ? null : br.getFormbyProperty("name", "F1");
        if (officialDownloadForm != null) {
            return false;
        } else {
            return super.isVideohosterEmbedHTML(br);
        }
    }

    private Form getOfficialVideoDownloadForm(final DownloadLink downloadLink, final Account account, final Browser br) {
        final Form ret = br.getFormByInputFieldKeyValue("op", "download_orig");
        modifyFreeDownloadForm(br, ret);
        return ret;
    }

    private boolean modifyFreeDownloadForm(Browser br, Form form) {
        if (form != null) {
            /* TODO: add handling to support premium mode here? */
            /* method_premium exists as input field AND button */
            /* free download button is done via javascript */
            /* premium download button does send method_premium twice to signal premium download */
            final List<InputField> methodPremiumFields = form.getInputFields("method_premium", null);
            if (methodPremiumFields.size() > 1) {
                form.remove("method_premium");
                return true;
            }
        }
        return false;
    }

    @Override
    public Form findFormDownload1Free(Browser br) throws Exception {
        final Form ret = super.findFormDownload1Free(br);
        modifyFreeDownloadForm(br, ret);
        return ret;
    }

    @Override
    protected Form findFormDownload2Free(Browser br) {
        final Form ret = super.findFormDownload2Free(br);
        modifyFreeDownloadForm(br, ret);
        return ret;
    }

    @Override
    protected boolean supports_availablecheck_filename_abuse() {
        /** 2021-01-28 */
        return false;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        /* 2022-05-31: No captchas at all */
        return false;
    }

    @Override
    protected Boolean requiresCaptchaForOfficialVideoDownload() {
        // Last changed: 2024-04-12
        return Boolean.FALSE;
    }

    @Override
    public Class<? extends XFSConfigVideoHotlinkCc> getConfigInterface() {
        return XFSConfigVideoHotlinkCc.class;
    }

    @Override
    protected boolean supportsShortURLs() {
        return true;
    }
}