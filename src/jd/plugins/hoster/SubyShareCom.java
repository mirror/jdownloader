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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SubyShareCom extends XFileSharingProBasic {
    public SubyShareCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
        /* 2021-03-08: Trying to avoid running into "/checkddos.php" */
        this.setStartIntervall(10 * 1000l);
    }

    private final String PROPERTY_FAKE_OFFLINE_STATUS = "fake_offline_status";

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-07-08: Premium untested, set FREE account limits <br />
     * captchatype-info: 2019-07-08: 4dignum --> xfilesharingprobasic_subysharecom_special <br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "subyshare.com" });
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
    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        try {
            final AvailableStatus status = super.requestFileInformationWebsite(link, account, isDownload);
            link.removeProperty(PROPERTY_FAKE_OFFLINE_STATUS);
            return status;
        } catch (final PluginException e) {
            if (e.getLinkStatus() != LinkStatus.ERROR_FILE_NOT_FOUND) {
                /* Some other Exception happened -> Forward it */
                throw e;
            }
            /* Check if the file is really offline */
            logger.info("Looks like file is offline --> Checking if it really is");
            final String filename = this.getFnameViaAbuseLink(br, link); // Throws exception if file is offline
            if (filename != null) {
                link.setName(filename);
            }
            link.setProperty(PROPERTY_FAKE_OFFLINE_STATUS, true);
            logger.info("File is not offline but GEO-blocked");
        }
        return AvailableStatus.TRUE;
    }

    public String regexFilenameAbuse(final Browser br) {
        String filename = super.regexFilenameAbuse(br);
        if (filename == null) {
            filename = br.getRegex("(?i)>\\s*Filename\\s*</label>\\s*<[^>]*>\\s*<p class=\"form-control-static\"[^>]*>([^<]+)<").getMatch(0);
        }
        return filename;
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
            return 1;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 3;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 3;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 5;
    }

    @Override
    protected void checkErrors(final Browser br, final String correctedBR, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        super.checkErrors(br, correctedBR, link, account, checkAll);
        /* 2019-07-08: Special */
        if (new Regex(correctedBR, "(?i)Sorry\\s*,\\s*we do not support downloading from Dedicated servers|Please download from your PC without using any above services|If this is our mistake\\s*,\\s*please contact").matches()) {
            final String errormsg = "VPN download prohibited by this filehost";
            if (account != null) {
                throw new AccountUnavailableException(errormsg, 15 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, errormsg);
            }
        } else if (new Regex(correctedBR, "(?i)>\\s*The owner of this file blocked you to download it").matches()) {
            /*
             * 2020-07-17: This may sometimes happen in premium mode - user is then supposed to contact the uploader to ask for permission
             * to download the file (WTF?!)
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The owner of this file blocked you to download it");
        } else if (new Regex(correctedBR, "(?i)>\\s*You do no have enough traffic to download this file").matches()) {
            /* 2023-01-25 */
            if (account != null) {
                throw new AccountUnavailableException("Traffic limit reached", 5 * 60 * 1000);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Traffic limit reached", 5 * 60 * 1000);
            }
        }
    }

    @Override
    protected String regexWaittime(Browser br) {
        String waitStr = super.regexWaittime(br);
        if (StringUtils.isEmpty(waitStr)) {
            /* 2018-07-19: Special */
            waitStr = new Regex(br.getRequest().getHtmlCode(), "class\\s*=\\s*\"seconds\"[^>]*?>\\s*?(\\d+)\\s*?<").getMatch(0);
        }
        return waitStr;
    }

    @Override
    public void doFree(final DownloadLink link, final Account account) throws Exception, PluginException {
        /* First bring up saved final links */
        String dllink = checkDirectLink(link, account);
        if (StringUtils.isEmpty(dllink)) {
            if (link.hasProperty(PROPERTY_FAKE_OFFLINE_STATUS)) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "URL is referer protected or offline");
            }
            requestFileInformationWebsite(link, account, true);
            int download1counter = 0;
            final int download1max = 1;
            do {
                logger.info(String.format("Handling download1 loop %d / %d", download1counter + 1, download1max + 1));
                /**
                 * Try to find a downloadlink. Check different methods sorted from "usually available" to "rarely available" (e.g. there are
                 * a lot of sites which support video embedding).
                 */
                dllink = getDllinkViaOfficialVideoDownload(this.br.cloneBrowser(), link, account, false);
                /* Check for streaming/direct links on the first page. */
                if (StringUtils.isEmpty(dllink)) {
                    checkErrors(br, correctedBR, link, account, false);
                    dllink = getDllink(link, account, br, correctedBR);
                }
                /* Do they support standard video embedding? */
                if (StringUtils.isEmpty(dllink) && this.internal_isVideohosterEmbed(this.br)) {
                    try {
                        logger.info("Trying to get link via embed");
                        dllink = requestFileInformationVideoEmbed(br.cloneBrowser(), link, account, false);
                        if (StringUtils.isEmpty(dllink)) {
                            logger.info("FAILED to get link via embed");
                        } else {
                            logger.info("Successfully found link via embed");
                        }
                    } catch (final InterruptedException e) {
                        throw e;
                    } catch (final Throwable e) {
                        logger.log(e);
                        logger.info("Failed to get link via embed");
                    }
                }
                /* Do they provide direct video URLs? */
                if (StringUtils.isEmpty(dllink)) {
                    /* Legacy - most XFS videohosts do not support this anymore! */
                    try {
                        logger.info("Trying to get link via vidembed");
                        final Browser brv = br.cloneBrowser();
                        getPage(brv, "/vidembed-" + this.getFUIDFromURL(link), false);
                        dllink = brv.getRedirectLocation();
                        if (StringUtils.isEmpty(dllink)) {
                            logger.info("Failed to get link via vidembed because: " + br.toString());
                        } else {
                            logger.info("Successfully found link via vidembed");
                        }
                    } catch (final InterruptedException e) {
                        throw e;
                    } catch (final Throwable e) {
                        logger.log(e);
                        logger.info("Failed to get link via vidembed");
                    }
                }
                /* Do we have an imagehost? */
                if (StringUtils.isEmpty(dllink) && this.isImagehoster()) {
                    checkErrors(br, correctedBR, link, account, false);
                    Form imghost_next_form = null;
                    do {
                        imghost_next_form = findImageForm(this.br);
                        if (imghost_next_form != null) {
                            /* end of backward compatibility */
                            submitForm(imghost_next_form);
                            checkErrors(br, correctedBR, link, account, false);
                            dllink = getDllink(link, account, br, correctedBR);
                            /* For imagehosts, filenames are often not given until we can actually see/download the image! */
                            final String image_filename = regexImagehosterFilename(br);
                            if (image_filename != null) {
                                link.setName(Encoding.htmlOnlyDecode(image_filename));
                            }
                        }
                    } while (imghost_next_form != null);
                }
                /* Check for errors and download1 Form. Only execute this once! */
                if (StringUtils.isEmpty(dllink) && download1counter == 0) {
                    /*
                     * Check errors here because if we don't and a link is premiumonly, download1 Form will be present, plugin will send it
                     * and most likely end up with error "Fatal countdown error (countdown skipped)"
                     */
                    checkErrors(br, correctedBR, link, account, false);
                    final Form download1 = findFormDownload1Free(br);
                    if (download1 != null) {
                        logger.info("Found download1 Form");
                        /* 2020-06-15: Special: Two captchas in the row possible! */
                        this.handleCaptcha(link, br, download1);
                        submitForm(download1);
                        checkErrors(br, correctedBR, link, account, false);
                        dllink = getDllink(link, account, br, correctedBR);
                    } else {
                        logger.info("Failed to find download1 Form");
                    }
                }
                download1counter++;
            } while (download1counter <= download1max && dllink == null);
        }
        if (StringUtils.isEmpty(dllink)) {
            Form download2 = findFormDownload2Free(br);
            if (download2 == null) {
                /* Last chance - maybe our errorhandling kicks in here. */
                checkErrors(br, correctedBR, link, account, false);
                /* Okay we finally have no idea what happened ... */
                logger.warning("Failed to find download2 Form");
                checkErrorsLastResort(br, link, account);
            }
            logger.info("Found download2 Form");
            /*
             * E.g. html contains text which would lead to error ERROR_IP_BLOCKED --> We're not checking for it as there is a download Form
             * --> Then when submitting it, html will contain another error e.g. 'Skipped countdown' --> In this case we want to prefer the
             * first thrown Exception. Why do we not check errors before submitting download2 Form? Because html could contain faulty
             * errormessages!
             */
            Exception exceptionBeforeDownload2Submit = null;
            try {
                checkErrors(br, correctedBR, link, account, false);
            } catch (final Exception e) {
                logger.log(e);
                exceptionBeforeDownload2Submit = e;
                logger.info("Found Exception before download2 Form submit");
            }
            /* Define how many forms deep do we want to try? */
            final int download2start = 0;
            final int download2max = 2;
            for (int download2counter = download2start; download2counter <= download2max; download2counter++) {
                logger.info(String.format("Download2 loop %d / %d", download2counter + 1, download2max + 1));
                download2.remove(null);
                final long timeBefore = System.currentTimeMillis();
                handlePassword(download2, link);
                handleCaptcha(link, br, download2);
                /* 2019-02-08: MD5 can be on the subsequent pages - it is to be found very rare in current XFS versions */
                if (link.getMD5Hash() == null) {
                    final String md5hash = new Regex(correctedBR, "<b>\\s*MD5.*?</b>.*?nowrap>(.*?)<").getMatch(0);
                    if (md5hash != null) {
                        link.setMD5Hash(md5hash.trim());
                    }
                }
                waitTime(link, timeBefore);
                final URLConnectionAdapter formCon = openAntiDDoSRequestConnection(br, br.createFormRequest(download2));
                if (looksLikeDownloadableContent(formCon)) {
                    /* Very rare case - e.g. tiny-files.com */
                    handleDownload(link, account, dllink, formCon.getRequest());
                    return;
                } else {
                    br.followConnection(true);
                    this.correctBR(br);
                    try {
                        formCon.disconnect();
                    } catch (final Throwable e) {
                    }
                }
                logger.info("Submitted Form download2");
                checkErrors(br, correctedBR, link, account, true);
                dllink = getDllinkViaOfficialVideoDownload(this.br.cloneBrowser(), link, account, false);
                if (dllink == null) {
                    dllink = getDllink(link, account, br, correctedBR);
                }
                download2 = findFormDownload2Free(br);
                if (StringUtils.isEmpty(dllink) && (download2 != null || download2counter == download2max)) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    /* Check if maybe an error happened before stepping in download2 loop --> Throw that */
                    if (download2counter == download2start + 1 && exceptionBeforeDownload2Submit != null) {
                        logger.info("Throwing exceptionBeforeDownload2Submit");
                        throw exceptionBeforeDownload2Submit;
                    }
                    checkErrorsLastResort(br, link, account);
                } else if (StringUtils.isEmpty(dllink) && download2 != null) {
                    invalidateLastChallengeResponse();
                    continue;
                } else {
                    validateLastChallengeResponse();
                    break;
                }
            }
        }
        handleDownload(link, account, dllink, null);
    }

    @Override
    public void handleCaptcha(final DownloadLink link, final Browser br, final Form captchaForm) throws Exception {
        /*
         * 2019-07-08: Special for two reasons: 1. Upper handling won't find the '/captchas/' URL. 2. These captchas are special: 6 digits
         * instead of 4 and colored (orange instead of black) - thus our standard XFS captcha-mathod won't be able to recognize them!
         */
        if (StringUtils.containsIgnoreCase(correctedBR, "/captchas/")) {
            logger.info("Detected captcha method \"Standard captcha\" for this host");
            final String captchaurl = new Regex(correctedBR, "(/captchas/[^<>\"\\']*)").getMatch(0);
            if (captchaurl == null) {
                logger.warning("Standard captcha captchahandling broken!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String code = getCaptchaCode("xfilesharingprobasic_subysharecom_special", captchaurl, link);
            if (code.contains("0")) {
                logger.info("Replacing captcha result zero with lowercase o");
                code = code.replace("0", "o");
            }
            captchaForm.put("code", code);
            logger.info("Put captchacode " + code + " obtained by captcha metod \"Standard captcha\" in the form.");
            link.setProperty(PROPERTY_captcha_required, true);
        } else {
            super.handleCaptcha(link, br, captchaForm);
        }
    }

    @Override
    protected String regExTrafficLeft(final Browser br) {
        /* 2018-07-19: Special */
        String trafficleftStr = super.regExTrafficLeft(br);
        if (StringUtils.isEmpty(trafficleftStr)) {
            final String src = this.getCorrectBR(br);
            trafficleftStr = new Regex(src, "(?i)Usable Bandwidth\\s*<span class=\"[^\"]+\">\\s*(\\d+(?:\\.\\d{1,2})? [A-Za-z]{2,5}) / [^<]+<").getMatch(0);
        }
        return trafficleftStr;
    }

    @Override
    public boolean isPasswordProtectedHTML(final Browser br, final Form pwForm) {
        /* 2020-02-17: Special */
        boolean pwprotected = super.isPasswordProtectedHTML(br, pwForm);
        if (!pwprotected) {
            pwprotected = br.containsHTML("(?i)><b>Password</b>");
        }
        return pwprotected;
    }

    @Override
    protected void getPage(final Browser br, String page, final boolean correctBr) throws Exception {
        super.getPage(br, page, false);
        handleAntiDdosChallenge(br, page);
        if (correctBr) {
            correctBR(br);
        }
    }

    @Override
    protected void postPage(final Browser br, String page, final String postdata, final boolean correctBr) throws Exception {
        super.postPage(br, page, postdata, false);
        handleAntiDdosChallenge(br, null);
        if (correctBr) {
            correctBR(br);
        }
    }

    @Override
    protected void submitForm(final Browser br, final Form form, final boolean correctBr) throws Exception {
        super.submitForm(br, form, false);
        handleAntiDdosChallenge(br, null);
        if (correctBr) {
            correctBR(br);
        }
    }

    private void handleAntiDdosChallenge(final Browser br, String targetPage) throws PluginException, IOException {
        final String checkddosPage = "/checkddos.php";
        if (br.getURL().contains(checkddosPage)) {
            /* 2021-03-08 */
            if (targetPage != null) {
                targetPage = br.getURL(targetPage).toString();
            }
            // throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Antiddos check triggered", 2 * 60 * 1000l);
            final Form form = br.getFormbyProperty("id", "checkDDOS");
            if (form == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (!form.hasInputFieldByName("b")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String calcChallenge = br.getRegex("(?i)Verify\\s*:\\s*</span>\\s*<strong>([0-9\\-\\+\\*x ]+)=\\?</strong>").getMatch(0);
            calcChallenge = calcChallenge.trim().toLowerCase(Locale.ENGLISH);
            /* E.g. "3 x 3" -> "3 * 3" */
            calcChallenge = calcChallenge.replace("x", "*");
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
            try {
                final ScriptEngine engine = manager.getEngineByName("javascript");
                final String js = "var res = " + calcChallenge + ";";
                engine.eval(js);
                final String res = engine.get("res").toString();
                form.put("b", res);
            } catch (final Exception e) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
            }
            br.submitForm(form);
            if (br.getURL().contains(checkddosPage)) {
                logger.warning("Failed to solve challenge(?)");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                logger.info("Checkddos challenge solved successfully");
                if (br.getURL().matches("^https?://[^/]+/?$")) {
                    logger.info("Redirect to mainpage happened");
                    if (targetPage != null && !targetPage.equalsIgnoreCase(br.getURL())) {
                        logger.info("Trying to correct bad redirect to mainpage to: " + targetPage);
                        br.getPage(targetPage);
                    }
                }
            }
        }
    }

    @Override
    protected boolean supports_availablecheck_filename_abuse() {
        /* Set this to false to avoid it being called multiple times. */
        return false;
    }
}