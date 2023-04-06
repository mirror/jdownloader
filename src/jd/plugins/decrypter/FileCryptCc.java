//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
package jd.plugins.decrypter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.Application;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.challenge.cutcaptcha.CaptchaHelperCrawlerPluginCutCaptcha;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia;
import org.jdownloader.plugins.components.config.FileCryptConfig;
import org.jdownloader.plugins.components.config.FileCryptConfig.CrawlMode;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.InputField;
import jd.plugins.CaptchaException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.UserAgents;
import jd.plugins.components.UserAgents.BrowserName;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FileCryptCc extends PluginForDecrypt {
    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public FileCryptCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "filecrypt.cc", "filecrypt.co" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/Container/([A-Z0-9]{10,16})(\\.html\\?mirror=\\d+)?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        /* Most of all links are captcha-protected. */
        return true;
    }

    private final String PROPERTY_PLUGIN_LAST_USED_PASSWORD = "last_used_password";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /*
         * Not all captcha types change when re-loading page without cookies (recaptchav2 isn't). I tried with new response value - raztoki
         */
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String mirrorIdFromURL = UrlQuery.parse(param.getCryptedUrl()).get("mirror");
        if (mirrorIdFromURL == null) {
            param.setCryptedUrl(param.getCryptedUrl() + ".html");
        }
        String successfullyUsedPassword = null;
        int cutCaptchaRetryIndex = -1;
        final int cutCaptchaAvoidanceMaxRetries = PluginJsonConfig.get(this.getConfigInterface()).getMaxCutCaptchaAvoidanceRetries();
        cutcaptchaAvoidanceLoop: while (cutCaptchaRetryIndex++ <= cutCaptchaAvoidanceMaxRetries && !this.isAbort()) {
            logger.info("cutcaptchaAvoidanceLoop " + (cutCaptchaRetryIndex + 1) + " / " + (cutCaptchaAvoidanceMaxRetries + 1));
            br.setLoadLimit(br.getLoadLimit() * 2);
            br.getHeaders().put("Accept-Encoding", "gzip, deflate");
            /* Website has no language selection as it auto-chooses based on IP and/or URL but we can force English language. */
            br.setCookie(Browser.getHost(param.getCryptedUrl()), "lang", "en");
            br.setFollowRedirects(true);
            br.addAllowedResponseCodes(500);// submit captcha responds with 500 code
            /* Use new User-Agent for each attempt */
            br.getHeaders().put("User-Agent", UserAgents.stringUserAgent(BrowserName.Chrome));
            this.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().matches("(?i)https?://[^/]+/404\\.html.*")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("(?i)>\\s*Dieser Ordner enthält keine Mirror")) {
                /* Empty link/folder. */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Separate password and captcha handling. this is easier for count reasons! */
            if (containsPassword()) {
                int passwordCounter = 0;
                final int maxPasswordRetries = 3;
                final List<String> passwords = getPreSetPasswords();
                final HashSet<String> usedPasswords = new HashSet<String>();
                final String lastUsedPassword = this.getPluginConfig().getStringProperty(PROPERTY_PLUGIN_LAST_USED_PASSWORD);
                /**
                 * Magic auto passwords: </br>
                 * Creators can set custom logos on each folder. Each logo has a unique ID. This way we can try specific passwords first
                 * that are typically associated with folders published by those sources.
                 */
                final String customLogoID = br.getRegex("custom/([a-z0-9]+)\\.png").getMatch(0);
                if ("53d1b".equals(customLogoID) || "80d13".equals(customLogoID)) {
                    logger.info("Found presumed password by custom logo: " + customLogoID);
                    passwords.add(0, "serienfans.org");
                } else if ("51967".equals(customLogoID)) {
                    logger.info("Found presumed password by custom logo: " + customLogoID);
                    passwords.add(0, "kellerratte");
                }
                if (successfullyUsedPassword != null) {
                    /**
                     * This may happen if user first enters correct password but then wrong captcha or retry was done to try to avoid
                     * cutcaptcha.
                     */
                    logger.info("Entering password handling with known correct password [user probably entered wrong captcha before]: " + successfullyUsedPassword);
                    passwords.clear();
                    passwords.add(successfullyUsedPassword);
                } else if (StringUtils.isNotEmpty(lastUsedPassword)) {
                    logger.info("Trying last used password first: " + lastUsedPassword);
                    passwords.add(0, lastUsedPassword);
                }
                passwordLoop: while (true) {
                    passwordCounter++;
                    if (passwordCounter >= maxPasswordRetries) {
                        logger.info("Stopping because: Too many wrong password attempts");
                        break passwordLoop;
                    }
                    logger.info("Password attempt: " + passwordCounter + " / " + maxPasswordRetries);
                    Form passwordForm = null;
                    final String passwordFieldKey = "password_";
                    final Form[] allForms = br.getForms();
                    if (allForms != null && allForms.length != 0) {
                        findPwFormLoop: for (final Form aForm : allForms) {
                            if (aForm.containsHTML("password") || aForm.hasInputFieldByName(passwordFieldKey)) {
                                logger.info("Found password form by hasInputFieldByName(passwordFieldKey)");
                                passwordForm = aForm;
                                break findPwFormLoop;
                            } else if (aForm.containsHTML("password")) {
                                logger.info("Found password form by aForm.containsHTML(\"password\")");
                                passwordForm = aForm;
                                break findPwFormLoop;
                            }
                        }
                    }
                    /* If there is captcha + password, password comes first, then captcha! */
                    if (passwordForm == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find pasword Form");
                    }
                    final String passCode;
                    if (passwords.size() > 0) {
                        /* First check all stored passwords. */
                        passCode = passwords.remove(0);
                        logger.info("Trying auto password: " + passCode);
                    } else {
                        /* when previous provided passwords have failed, or not provided we should ask */
                        passCode = getUserInput("Password?", param);
                        if (StringUtils.isEmpty(passCode)) {
                            /* Bad user input */
                            throw new DecrypterException(DecrypterException.PASSWORD);
                        }
                    }
                    if (!usedPasswords.add(passCode)) {
                        // no need to submit password that has already been tried!
                        logger.info("Skipping already tried password: " + passCode);
                        continue;
                    }
                    passwordForm.put(passwordFieldKey, Encoding.urlEncode(passCode));
                    submitForm(passwordForm);
                    if (!containsPassword()) {
                        logger.info("Password success: " + passCode);
                        successfullyUsedPassword = passCode;
                        break passwordLoop;
                    }
                }
                if (passwordCounter >= maxPasswordRetries && containsPassword()) {
                    throw new DecrypterException(DecrypterException.PASSWORD);
                }
                logger.info("Saving correct password for future usage: " + successfullyUsedPassword);
                this.getPluginConfig().setProperty(PROPERTY_PLUGIN_LAST_USED_PASSWORD, successfullyUsedPassword);
            }
            /* Process captcha */
            int captchaCounter = -1;
            final int maxCaptchaRetries = 10;
            captchaLoop: while (captchaCounter++ < maxCaptchaRetries && containsCaptcha() && !this.isAbort()) {
                logger.info("Captcha loop: " + captchaCounter + " / " + maxCaptchaRetries);
                Form captchaForm = null;
                final Form[] forms = br.getForms();
                if (forms != null && forms.length != 0) {
                    for (final Form form : forms) {
                        if (form.containsHTML("captcha") || SolveMedia.containsSolvemediaCaptcha(form) || AbstractRecaptchaV2.containsRecaptchaV2Class(form)) {
                            captchaForm = form;
                            break;
                        }
                    }
                }
                if (captchaForm == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String captchaURL = captchaForm.getRegex("((https?://[^<>\"']*?)?/captcha/[^<>\"']*?)\"").getMatch(0);
                if (captchaURL != null && captchaURL.contains("circle.php")) {
                    final File file = this.getLocalCaptchaFile();
                    getCaptchaBrowser(br).getDownload(file, captchaURL);
                    final ClickedPoint cp = getCaptchaClickedPoint(getHost(), file, param, "Click on the open circle");
                    if (cp == null) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    final InputField button = captchaForm.getInputFieldByType(InputField.InputType.IMAGE.name());
                    if (button != null) {
                        captchaForm.removeInputField(button);
                        captchaForm.put(button.getKey() + ".x", String.valueOf(cp.getX()));
                        captchaForm.put(button.getKey() + ".y", String.valueOf(cp.getY()));
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                } else if (captchaForm != null && captchaForm.containsHTML("=\"g-recaptcha\"")) {
                    final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                    captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                } else if (captchaForm != null && SolveMedia.containsSolvemediaCaptcha(captchaForm)) {
                    final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                    File cf = null;
                    try {
                        cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    } catch (final Exception e) {
                        if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                            throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                        }
                        throw e;
                    }
                    final String code = getCaptchaCode("solvemedia", cf, param);
                    if (StringUtils.isEmpty(code)) {
                        if (captchaCounter + 1 < maxCaptchaRetries) {
                            continue;
                        } else {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                    }
                    final String chid = sm.getChallenge(code);
                    captchaForm.put("adcopy_response", Encoding.urlEncode(code));
                    captchaForm.put("adcopy_challenge", chid);
                } else if (captchaForm != null && captchaForm.containsHTML("capcode")) {
                    Challenge<String> challenge = new KeyCaptcha(this, br, createDownloadlink(param.getCryptedUrl())).createChallenge(this);
                    try {
                        final String result = handleCaptchaChallenge(challenge);
                        if (challenge.isRefreshTrigger(result)) {
                            continue;
                        }
                        if (StringUtils.isEmpty(result)) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        if ("CANCEL".equals(result)) {
                            throw new PluginException(LinkStatus.ERROR_FATAL);
                        }
                        captchaForm.put("capcode", Encoding.urlEncode(result));
                    } catch (CaptchaException e) {
                        e.throwMeIfNoRefresh();
                        continue;
                    } catch (Throwable e) {
                        e.printStackTrace();
                        continue;
                    }
                } else if (StringUtils.containsIgnoreCase(captchaURL, "cutcaptcha")) {
                    final boolean tryCutCaptchaInDevMode = false;
                    if (!Application.isHeadless() && DebugMode.TRUE_IN_IDE_ELSE_FALSE && tryCutCaptchaInDevMode) {
                        // current implementation via localhost no longer working
                        final String cutcaptcha = new CaptchaHelperCrawlerPluginCutCaptcha(this, br, "SAs61IAI").getToken();
                        if (StringUtils.isEmpty(cutcaptcha)) {
                            if (captchaCounter + 1 < maxCaptchaRetries) {
                                continue;
                            } else {
                                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                            }
                        }
                        captchaForm.put("cap_token", cutcaptcha);
                    } else {
                        logger.info("Trying to avoid cutcaptcha");
                        /* Clear cookies to increase the chances of getting a different captcha type than cutcaptcha. */
                        br.clearAll();
                        sleep(1000, param);
                        /*
                         * Continue from the beginning. If a password was required, we already know the correct password and won't have to
                         * ask the user again.
                         */
                        continue cutcaptchaAvoidanceLoop;
                    }
                } else {
                    /* Normal image captcha */
                    final String code = getCaptchaCode(captchaURL, param);
                    captchaForm.put("recaptcha_response_field", Encoding.urlEncode(code));
                }
                submitForm(captchaForm);
                if (this.containsCaptcha()) {
                    logger.info("User entered wrong captcha");
                    continue captchaLoop;
                } else {
                    logger.info("User entered correct captcha");
                    break;
                }
            }
            /* Dead end: No reason to continue this loop here. */
            logger.info("Stepping out of cutCaptchaAvoidanceLoop");
            break cutcaptchaAvoidanceLoop;
        }
        if (containsCaptcha()) {
            if (cutCaptchaRetryIndex >= cutCaptchaAvoidanceMaxRetries) {
                /* Fallback to rc2 no longer working or not desired by user. */
                throw new DecrypterRetryException(RetryReason.CAPTCHA, "CUTCAPTCHA_IS_NOT_SUPPORTED_" + folderID, "Cutcaptcha is not supported! Please read: support.jdownloader.org/Knowledgebase/Article/View/cutcaptcha-not-supported");
            } else {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        FilePackage fp = null;
        final String fpName = br.getRegex("<h2>([^<>\"]*?)<").getMatch(0);
        // mirrors - note: containers no longer have uid within path! -raztoki20160117
        // mirrors - note: containers can contain uid within path... -raztoki20161204
        String[] availableMirrors = br.getRegex("\"([^\"]*/Container/[A-Z0-9]+\\.html\\?mirror=\\d+)").getColumn(0);
        if (availableMirrors == null || availableMirrors.length < 1) {
            /* Fallback -> Only 1 mirror available */
            availableMirrors = new String[1];
            if (mirrorIdFromURL != null) {
                availableMirrors[0] = param.getCryptedUrl();
            } else {
                availableMirrors[0] = param.getCryptedUrl() + "?mirror=0";
            }
        }
        final List<String> mirrors = new ArrayList<String>();
        if (mirrorIdFromURL != null && PluginJsonConfig.get(this.getConfigInterface()).getCrawlMode() == CrawlMode.PREFER_GIVEN_MIRROR_ID) {
            for (String mirror : availableMirrors) {
                final String mirrorID = new Regex(mirror, "mirror=(\\d+)").getMatch(0);
                if (StringUtils.equals(mirrorID, mirrorIdFromURL)) {
                    mirrors.add(mirror);
                    break;
                }
            }
            if (mirrors.size() == 0) {
                logger.info("User preferred mirrorID inside URL does not exist in list of really existant mirrors: " + mirrorIdFromURL);
            }
        }
        if (mirrors.size() > 0) {
            logger.info("Crawl mirror according to mirrorID from URL:" + mirrors);
        } else {
            /* User preferred mirrorID not found or no preferred mirror given --> Crawl all */
            mirrors.addAll(Arrays.asList(availableMirrors));
            logger.info("Crawling all existing mirrors: " + mirrors);
        }
        Collections.sort(mirrors);
        try {
            int progressNumber = 0;
            for (final String mirrorURL : mirrors) {
                progressNumber++;
                logger.info("Crawling mirror " + progressNumber + "/" + mirrors.size() + " | " + mirrorURL);
                br.getPage(mirrorURL);
                // Use clicknload first as it doesn't rely on JD service.jdownloader.org, which can go down!
                final ArrayList<DownloadLink> cnlResults = handleCnl2(param.getCryptedUrl());
                if (!cnlResults.isEmpty()) {
                    logger.info("CNL success");
                    ret.addAll(cnlResults);
                    if (fpName != null) {
                        if (fp == null) {
                            fp = FilePackage.getInstance();
                            fp.setName(Encoding.htmlDecode(fpName).trim());
                        }
                        fp.addLinks(cnlResults);
                    }
                    distribute(cnlResults.toArray(new DownloadLink[0]));
                } else {
                    /* Second try DLC, then single links */
                    logger.info("CNL failure -> Trying DLC");
                    String dlc_id = br.getRegex("DownloadDLC\\('([^<>\"]*?)'\\)").getMatch(0);
                    if (dlc_id == null) {
                        /* 2023-02-13 */
                        dlc_id = br.getRegex("onclick=\"DownloadDLC[^\\(]*\\('([^']+)'").getMatch(0);
                    }
                    if (dlc_id == null) {
                        /* 2023-04-06 */
                        dlc_id = br.getRegex("class=\"dlcdownload\"[^>]* onclick=\"[^\\(]+\\('([^\\']+)").getMatch(0);
                    }
                    if (dlc_id != null) {
                        logger.info("DLC found - trying to add it");
                        final ArrayList<DownloadLink> dlcResults = loadcontainer(br.getURL("/DLC/" + dlc_id + ".dlc").toExternalForm());
                        if (dlcResults == null || dlcResults.isEmpty()) {
                            logger.warning("DLC for current mirror is empty or something is broken!");
                            continue;
                        } else {
                            logger.info("DLC success");
                            ret.addAll(dlcResults);
                        }
                    }
                }
                if (this.isAbort()) {
                    logger.info("Stopping because: Aborted by user");
                    return ret;
                }
            }
            if (!ret.isEmpty()) {
                logger.info("CNL/DLC successfully added");
                return ret;
            }
            // this isn't always shown, see 104061178D - raztoki 20141118
            logger.info("Trying single link handling");
            String[] links = br.getRegex("openLink\\('([^<>\"]*?)'").getColumn(0);
            if (links == null || links.length == 0) {
                /* 2023-04-06 */
                links = br.getRegex("onclick\\s*=\\s*\"[^\\(]*\\('([^<>\"\\']+)").getColumn(0);
                if (links == null || links.length == 0) {
                    /* 2023-02-03 */
                    links = br.getRegex("onclick=\"openLink[^\\(\"\\']*\\('([^<>\"\\']+)'").getColumn(0);
                    if (links == null || links.length == 0) {
                        /* 2023-02-13 */
                        links = br.getRegex("'([^\"']+)', this\\);\" class=\"download\"[^>]*target=\"_blank\"").getColumn(0);
                    }
                }
            }
            if (links == null || links.length == 0) {
                if (br.containsHTML("Der Inhaber dieses Ordners hat leider alle Hoster in diesem Container in seinen Einstellungen deaktiviert\\.")) {
                    return ret;
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            br.setFollowRedirects(false);
            br.setCookie(this.getHost(), "BetterJsPopCount", "1");
            int index = -1;
            redirectLinksLoop: for (final String singleLink : links) {
                index++;
                logger.info("Processing redirectLinksLoop position: " + index + " / " + links.length);
                String finallink = null;
                int retryLink = 2;
                singleRedirectLinkLoop: while (!isAbort()) {
                    finallink = handleLink(br, param, singleLink);
                    if (StringUtils.equals("IGNORE", finallink)) {
                        continue singleRedirectLinkLoop;
                    } else if (finallink != null || --retryLink == 0) {
                        logger.info(singleLink + "->" + finallink + "|" + retryLink);
                        break singleRedirectLinkLoop;
                    }
                }
                if (finallink != null) {
                    final DownloadLink link = createDownloadlink(finallink);
                    ret.add(link);
                    if (fpName != null) {
                        if (fp == null) {
                            fp = FilePackage.getInstance();
                            fp.setName(Encoding.htmlDecode(fpName).trim());
                        }
                        fp.add(link);
                    }
                    distribute(link);
                }
                if (isAbort()) {
                    logger.info("Stopping because: Aborted by user");
                    break redirectLinksLoop;
                }
            }
        } finally {
            if (successfullyUsedPassword != null) {
                /* Assume that required password is also extract password. */
                final ArrayList<String> pwlist = new ArrayList<String>();
                pwlist.add(successfullyUsedPassword);
                for (final DownloadLink result : ret) {
                    result.setSourcePluginPasswordList(pwlist);
                }
            }
        }
        return ret;
    }

    private String handleLink(final Browser br, final CryptedLink param, final String singleLink) throws Exception {
        final Browser br2 = br.cloneBrowser();
        if (StringUtils.startsWithCaseInsensitive(singleLink, "http://") || StringUtils.startsWithCaseInsensitive(singleLink, "https://")) {
            br2.getPage(singleLink);
        } else {
            br2.getPage("/Link/" + singleLink + ".html");
        }
        if (br2.containsHTML("friendlyduck.com/") || br2.containsHTML("filecrypt\\.(cc|co)/usenet\\.html") || br2.containsHTML("powerusenet.xyz")) {
            /* Advertising */
            return "IGNORE";
        }
        int retryCaptcha = 5;
        while (!isAbort() && retryCaptcha-- > 0) {
            if (br2.containsHTML("Security prompt")) {
                final String captcha = br2.getRegex("(/captcha/[^<>\"]*?)\"").getMatch(0);
                if (captcha != null && captcha.contains("circle.php")) {
                    final File file = this.getLocalCaptchaFile();
                    getCaptchaBrowser(br).getDownload(file, captcha);
                    final ClickedPoint cp = getCaptchaClickedPoint(getHost(), file, param, "Click on the open circle");
                    if (cp == null) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    final Form form = new Form();
                    form.setMethod(MethodType.POST);
                    form.setAction(br2.getURL());
                    form.put("button.x", String.valueOf(cp.getX()));
                    form.put("button.y", String.valueOf(cp.getY()));
                    form.put("button", "send");
                    br2.submitForm(form);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                break;
            }
        }
        String finallink = null;
        final String first_rd = br2.getRedirectLocation();
        if (first_rd != null && first_rd.matches(".*filecrypt\\.(cc|co)/.*")) {
            return handleLink(br2, param, first_rd);
        } else if (first_rd != null && !first_rd.matches(".*filecrypt\\.(cc|co)/.*")) {
            finallink = first_rd;
        } else {
            final String nextlink = br2.getRegex("(\"|')(https?://[^/]+/index\\.php\\?Action=(G|g)o[^<>\"']+)").getMatch(1);
            if (nextlink != null) {
                return handleLink(br2, param, nextlink);
            }
        }
        if (finallink == null) {
            return null;
        } else if (this.canHandle(finallink)) {
            return null;
        } else {
            return finallink;
        }
    }

    private ArrayList<DownloadLink> handleCnl2(final String url) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Form[] forms = br.getForms();
        Form CNLPOP = null;
        Form cnl = null;
        for (final Form f : forms) {
            if (f.containsHTML("CNLPOP") || f.containsHTML("cnlform")) {
                CNLPOP = f;
                break;
            }
        }
        if (CNLPOP != null) {
            final String infos[] = CNLPOP.getRegex("'(.*?)'").getColumn(0);
            cnl = new Form();
            cnl.addInputField(new InputField("crypted", infos[2]));
            cnl.addInputField(new InputField("jk", "function f(){ return \'" + infos[1] + "';}"));
            cnl.addInputField(new InputField("source", null));
        } else {
            /* 2nd attempt */
            for (final Form f : forms) {
                if (f.hasInputFieldByName("jk")) {
                    cnl = f;
                    break;
                }
            }
        }
        if (cnl != null) {
            final Map<String, String> infos = new HashMap<String, String>();
            infos.put("crypted", Encoding.urlDecode(cnl.getInputField("crypted").getValue(), false));
            infos.put("jk", Encoding.urlDecode(cnl.getInputField("jk").getValue(), false));
            String source = cnl.getInputField("source").getValue();
            if (StringUtils.isEmpty(source)) {
                source = url;
            } else {
                infos.put("source", source);
            }
            infos.put("source", source);
            final String json = JSonStorage.toString(infos);
            final DownloadLink dl = createDownloadlink("http://dummycnl.jdownloader.org/" + HexFormatter.byteArrayToHex(json.getBytes("UTF-8")));
            ret.add(dl);
        }
        return ret;
    }

    private final boolean containsCaptcha() {
        return new Regex(cleanHTML, ">(?:Sicherheitsüberprüfung|Security prompt)</").matches();
    }

    private final boolean containsPassword() {
        return new Regex(cleanHTML, "(?i)>(?:Passwort erforderlich|Password required)</").matches();
    }

    private String cleanHTML = null;

    private final void cleanUpHTML() {
        String toClean = br.toString();
        ArrayList<String> regexStuff = new ArrayList<String>();
        // generic cleanup
        regexStuff.add("<!(--.*?--)>");
        regexStuff.add("(<\\s*(\\w+)\\s+[^>]*style\\s*=\\s*(\"|')(?:(?:[\\w:;\\s#-]*(visibility\\s*:\\s*hidden;|display\\s*:\\s*none;|font-size\\s*:\\s*0;)[\\w:;\\s#-]*)|font-size\\s*:\\s*0|visibility\\s*:\\s*hidden|display\\s*:\\s*none)\\3[^>]*(>.*?<\\s*/\\2[^>]*>|/\\s*>))");
        for (String aRegex : regexStuff) {
            String results[] = new Regex(toClean, aRegex).getColumn(0);
            if (results != null) {
                for (String result : results) {
                    toClean = toClean.replace(result, "");
                }
            }
        }
        cleanHTML = toClean;
    }

    @SuppressWarnings("deprecation")
    private ArrayList<DownloadLink> loadcontainer(final String theLink) throws IOException, PluginException {
        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        final Browser brc = br.cloneBrowser();
        File file = null;
        URLConnectionAdapter con = null;
        try {
            con = brc.openGetConnection(theLink);
            if (con.getResponseCode() == 200) {
                file = JDUtilities.getResourceFile("tmp/filecryptcc/" + JDHash.getSHA1(theLink) + theLink.substring(theLink.lastIndexOf(".")));
                if (file == null) {
                    return links;
                }
                file.getParentFile().mkdirs();
                file.deleteOnExit();
                brc.downloadConnection(file, con);
                if (file != null && file.exists() && file.length() > 100) {
                    links.addAll(loadContainerFile(file));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
            if (file.exists()) {
                file.delete();
            }
        }
        return links;
    }

    private final void getPage(final String page) throws Exception {
        if (page == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(page);
        cleanUpHTML();
    }

    private final void postPage(final String url, final String post) throws Exception {
        if (url == null || post == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.postPage(url, post);
        cleanUpHTML();
    }

    private final void submitForm(final Form form) throws Exception {
        if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.submitForm(form);
        cleanUpHTML();
    }

    @Override
    public Class<? extends FileCryptConfig> getConfigInterface() {
        return FileCryptConfig.class;
    }
}