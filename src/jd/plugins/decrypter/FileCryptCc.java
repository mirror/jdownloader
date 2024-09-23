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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
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
    public FileCryptCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setLoadLimit(br.getLoadLimit() * 2);
        br.getHeaders().put("Accept-Encoding", "gzip, deflate");
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "filecrypt.cc", "filecrypt.co", "filecrypt.to" });
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
        /* Most of all filecrypt links are captcha-protected. */
        return true;
    }

    private static final String PROPERTY_PLUGIN_LAST_USED_PASSWORD = "last_used_password";
    private String              logoPW                             = null;
    private String              successfullyUsedFolderPassword     = null;

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        /*
         * Not all captcha types change when re-loading page without cookies (recaptchav2 doesn't).
         */
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String mirrorIdFromURL = UrlQuery.parse(param.getCryptedUrl()).get("mirror");
        String contenturl = param.getCryptedUrl();
        if (mirrorIdFromURL == null && !StringUtils.endsWithCaseInsensitive(contenturl, ".html")) {
            /* Fix url */
            contenturl += ".html";
        }
        /* Nullification */
        this.logoPW = null;
        this.successfullyUsedFolderPassword = null;
        this.handlePasswordAndCaptcha(param, folderID, contenturl);
        ArrayList<String> extractionPasswordList = null;
        if (successfullyUsedFolderPassword != null || logoPW != null) {
            /* Assume that the required password is also the extract password. */
            extractionPasswordList = new ArrayList<String>();
            if (successfullyUsedFolderPassword != null) {
                extractionPasswordList.add(successfullyUsedFolderPassword);
            }
            /* Password by custom logo can differ from folder password and can also be given if no folder password is needed. */
            if (logoPW != null && !logoPW.equals(successfullyUsedFolderPassword)) {
                extractionPasswordList.add(logoPW);
            }
        }
        /* Crawl mirrors */
        FilePackage fp = null;
        final String fpName = br.getRegex("<h2>([^<]+)<").getMatch(0);
        if (fpName != null) {
            fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
        }
        String[] availableMirrorurls = br.getRegex("\"([^\"]*/Container/[A-Z0-9]+\\.html\\?mirror=\\d+)").getColumn(0);
        if (availableMirrorurls == null || availableMirrorurls.length == 0) {
            /* Fallback -> Probably 1 mirror available */
            if (br.containsHTML(">\\s*Der Inhaber dieses Ordners hat leider alle Hoster in diesem Container in seinen Einstellungen deaktiviert")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            logger.info("Failed to find any mirrors in html -> Looks like only one mirror is available");
            availableMirrorurls = new String[1];
            if (mirrorIdFromURL != null) {
                availableMirrorurls[0] = contenturl;
            } else {
                availableMirrorurls[0] = contenturl + "?mirror=0";
            }
        }
        final List<String> mirrorurls = new ArrayList<String>();
        String urlWithUserPreferredMirrorID = null;
        for (final String mirrorurl : availableMirrorurls) {
            final String mirrorID = UrlQuery.parse(mirrorurl).get("mirror");
            if (StringUtils.equals(mirrorID, mirrorIdFromURL)) {
                urlWithUserPreferredMirrorID = mirrorurl;
            }
            /* Prevent duplicates */
            if (!mirrorurls.contains(mirrorurl)) {
                mirrorurls.add(mirrorurl);
            }
        }
        if (urlWithUserPreferredMirrorID != null) {
            logger.info("Found user preferred mirrorID " + mirrorIdFromURL);
        } else {
            logger.info("User preferred mirrorID " + mirrorIdFromURL + " does not exist in list of really existing mirrors");
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int numberofOfflineMirrors = 0;
        int numberofSkippedFakeAdvertisementMirrors = 0;
        mirrorLoop: for (int mirrorindex = 0; mirrorindex < mirrorurls.size(); mirrorindex++) {
            final String mirrorurl = mirrorurls.get(mirrorindex);
            final String currentMirrorID = UrlQuery.parse(mirrorurl).get("mirror");
            logger.info("Crawling mirror " + (mirrorindex + 1) + "/" + mirrorurls.size() + " | MirrorID: " + currentMirrorID + " | " + mirrorurl);
            if (mirrorindex > 0) {
                /* Password and captcha can be required for each mirror */
                this.handlePasswordAndCaptcha(param, folderID, mirrorurl);
            } else {
                logger.info("Do not access mirrirurl because we are currently crawling the first mirror");
            }
            boolean mirrorLooksToBeOffline = false;
            boolean mirrorLooksToBeAdvertisement = false;
            if (br.containsHTML("class=\"window container offline\"")) {
                logger.info("Mirror looks to be offline: " + mirrorurl);
                numberofOfflineMirrors++;
                mirrorLooksToBeOffline = true;
            } else if (br.getURL().contains("mirror=666") && br.containsHTML("usenet")) {
                logger.info("Mirror looks to be a fake advertisement mirror: " + mirrorurl);
                mirrorLooksToBeAdvertisement = true;
            }
            /* Try CNL/clicknload first as it doesn't rely on JD service.jdownloader.org, which can go down! */
            final boolean testDevCnlFailure = false;
            final ArrayList<DownloadLink> thisMirrorResults = new ArrayList<DownloadLink>();
            final ArrayList<DownloadLink> cnlResults;
            cldHandling: if (true) {
                if (testDevCnlFailure && DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    logger.warning("CNL failure test active!");
                    cnlResults = new ArrayList<DownloadLink>();
                } else {
                    cnlResults = handleCnl2(contenturl, successfullyUsedFolderPassword);
                    if (cnlResults.isEmpty()) {
                        logger.info("Failed to find CNL results");
                        break cldHandling;
                    }
                    logger.info("CNL success");
                    for (final DownloadLink link : cnlResults) {
                        if (fp != null) {
                            link._setFilePackage(fp);
                        }
                        if (extractionPasswordList != null) {
                            link.setSourcePluginPasswordList(extractionPasswordList);
                        }
                        distribute(link);
                        thisMirrorResults.add(link);
                    }
                }
            }
            dlcContainerHandling: if (thisMirrorResults.isEmpty()) {
                /* Second try DLC, then single links */
                logger.info("CNL failure -> Trying DLC");
                String dlc_id = br.getRegex("DownloadDLC\\('([^<>\"]*?)'\\)").getMatch(0);
                if (dlc_id == null) {
                    /* 2023-02-13 */
                    dlc_id = br.getRegex("onclick=\"DownloadDLC[^\\(]*\\('([^']+)'").getMatch(0);
                    if (dlc_id == null) {
                        /* 2023-04-06 */
                        dlc_id = br.getRegex("class=\"dlcdownload\"[^>]* onclick=\"[^\\(]+\\('([^\\']+)").getMatch(0);
                        if (dlc_id == null) {
                            /* 2024-01-25 */
                            dlc_id = br.getRegex("DownloadDLC\\('([^\\']+)'\\)").getMatch(0);
                        }
                    }
                }
                if (dlc_id == null) {
                    logger.info("Failed to find DLC container");
                    break dlcContainerHandling;
                }
                logger.info("DLC found - trying to add it");
                final ArrayList<DownloadLink> dlcResults = loadcontainer(br.getURL("/DLC/" + dlc_id + ".dlc").toExternalForm());
                if (dlcResults == null || dlcResults.isEmpty()) {
                    logger.warning("DLC for current mirror is empty or something is broken!");
                    break dlcContainerHandling;
                }
                logger.info("DLC success");
                for (final DownloadLink link : dlcResults) {
                    if (fp != null) {
                        link._setFilePackage(fp);
                    }
                    if (extractionPasswordList != null) {
                        link.setSourcePluginPasswordList(extractionPasswordList);
                    }
                    distribute(link);
                    thisMirrorResults.add(link);
                }
            }
            redirectLinksHandling: if (thisMirrorResults.isEmpty()) {
                /* Last resort: Try most time intensive way to crawl links: Crawl each link individually. */
                logger.info("Trying single link redirect handling");
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
                    logger.info("Failed to find redirectLinks");
                    break redirectLinksHandling;
                }
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(false);
                brc.setCookie(br.getHost(), "BetterJsPopCount", "1");
                int index = -1;
                final HashSet<String> dupes = new HashSet<String>();
                redirectLinksLoop: for (final String singleLink : links) {
                    index++;
                    logger.info("Processing redirectLinksLoop position: " + index + "/" + links.length + " | " + singleLink);
                    if (!dupes.add(singleLink)) {
                        logger.info("Skipping dupe: " + singleLink);
                        continue;
                    }
                    String finallink = null;
                    int retryLink = 2;
                    singleRedirectLinkLoop: while (!isAbort()) {
                        finallink = handleLink(brc, param, singleLink, 0);
                        if (StringUtils.equals("IGNORE", finallink)) {
                            continue singleRedirectLinkLoop;
                        } else if (finallink != null || --retryLink == 0) {
                            logger.info(singleLink + " -> " + finallink + " | " + retryLink);
                            break singleRedirectLinkLoop;
                        }
                    }
                    if (finallink == null) {
                        logger.warning("Failed to find any result for: " + singleLink);
                        continue;
                    }
                    final DownloadLink link = createDownloadlink(finallink);
                    if (fp != null) {
                        link._setFilePackage(fp);
                    }
                    if (extractionPasswordList != null) {
                        link.setSourcePluginPasswordList(extractionPasswordList);
                    }
                    thisMirrorResults.add(link);
                    distribute(link);
                    if (isAbort()) {
                        logger.info("Stopping because: Aborted by user");
                        break redirectLinksLoop;
                    }
                }
            }
            logger.info("Mirror " + currentMirrorID + "results: " + thisMirrorResults.size());
            if (thisMirrorResults.isEmpty()) {
                if (mirrorLooksToBeOffline) {
                    logger.info("Skipping mirror which looks to be offline: " + mirrorurl);
                    numberofOfflineMirrors++;
                    continue;
                } else if (mirrorLooksToBeAdvertisement) {
                    logger.info("Skipping fake advertisement mirror: " + mirrorurl);
                    numberofSkippedFakeAdvertisementMirrors++;
                    continue;
                } else {
                    logger.warning("Failed at mirror: " + mirrorurl);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            ret.addAll(thisMirrorResults);
            if (PluginJsonConfig.get(this.getConfigInterface()).getCrawlMode() == CrawlMode.PREFER_GIVEN_MIRROR_ID && mirrorIdFromURL != null && currentMirrorID.equals(mirrorIdFromURL)) {
                logger.info("Stopping because: Found user desired mirror: " + mirrorIdFromURL);
                break mirrorLoop;
            }
        }
        if (ret.isEmpty()) {
            if (numberofOfflineMirrors == mirrorurls.size() - numberofSkippedFakeAdvertisementMirrors) {
                /* In this case filecrypt is only using the link to show ads. */
                logger.info("All mirrors are offline -> Whole folder is offline");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (numberofOfflineMirrors == mirrorurls.size() - numberofSkippedFakeAdvertisementMirrors) {
                /* In this case filecrypt is only using the link to show ads. */
                logger.info("All mirrors are offline -> Whole folder is offline");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return ret;
    }

    private void handlePasswordAndCaptcha(final CryptedLink param, final String folderID, final String url) throws Exception {
        /* Prepare browser */
        br.addAllowedResponseCodes(500);// submit captcha responds with 500 code
        int cutCaptchaRetryIndex = -1;
        final FileCryptConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final int cutCaptchaAvoidanceMaxRetries = cfg.getMaxCutCaptchaAvoidanceRetries();
        final HashSet<String> usedWrongPasswords = new HashSet<String>();
        boolean captchaSuccess = false;
        boolean lastCaptchaIsCutCaptcha = false;
        boolean tryToSolveCutCaptcha = false;
        cutcaptchaAvoidanceLoop: while (cutCaptchaRetryIndex++ <= cutCaptchaAvoidanceMaxRetries && !this.isAbort()) {
            logger.info("cutcaptchaAvoidanceLoop " + (cutCaptchaRetryIndex + 1) + " / " + (cutCaptchaAvoidanceMaxRetries + 1));
            /* Website has no language selection as it auto-chooses based on IP and/or URL but we can force English language. */
            final String host;
            if (br.getRequest() != null) {
                host = br.getHost();
            } else {
                host = Browser.getHost(url);
            }
            br.setCookie(host, "lang", "en");
            if (cutCaptchaRetryIndex > 0) {
                /* Use new User-Agent for each attempt */
                br.getHeaders().put("User-Agent", UserAgents.stringUserAgent(BrowserName.Chrome));
            }
            this.getPage(url);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getURL().matches("(?i)https?://[^/]+/404\\.html.*")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("(?i)>\\s*Dieser Ordner enthält keine Mirror")) {
                /* Empty link/folder. */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (cutCaptchaRetryIndex == 0 && logoPW == null) {
                /**
                 * Search password based on folder-logo. </br>
                 * Only do this one time in the first run of this loop.
                 */
                final String customLogoID = br.getRegex("custom/([a-z0-9]+)\\.png").getMatch(0);
                if (customLogoID != null) {
                    /**
                     * Magic auto passwords: </br>
                     * Creators can set custom logos on each folder. Each logo has a unique ID. This way we can try specific passwords first
                     * that are typically associated with folders published by those sources.
                     */
                    if ("53d1b".equals(customLogoID) || "80d13".equals(customLogoID) || "fde1d".equals(customLogoID) || "8abe0".equals(customLogoID) || "8f073".equals(customLogoID)) {
                        logoPW = "serienfans.org";
                    } else if ("975e4".equals(customLogoID)) {
                        logoPW = "filmfans.org";
                    } else if ("51967".equals(customLogoID)) {
                        logoPW = "kellerratte";
                    } else if ("aaf75".equals(customLogoID)) {
                        /* 2023-10-23 */
                        logoPW = "cs.rin.ru";
                    }
                    if (logoPW != null) {
                        logger.info("Found possible PW by logoID: " + logoPW);
                    } else {
                        logger.info("Found unknown logoID: " + customLogoID);
                    }
                } else {
                    logger.info("Failed to find logoID");
                }
            }
            /* Separate password and captcha handling. This is easier for several reasons! */
            if (containsPassword(this.cleanHTML)) {
                int passwordCounter = 0;
                final int maxPasswordRetries = 3;
                final List<String> passwords = getPreSetPasswords();
                final String lastUsedPassword = this.getPluginConfig().getStringProperty(PROPERTY_PLUGIN_LAST_USED_PASSWORD);
                if (logoPW != null) {
                    logger.info("Try PW by logo: " + logoPW);
                    passwords.add(0, logoPW);
                }
                if (successfullyUsedFolderPassword != null) {
                    /**
                     * This may happen if user first enters correct password but then wrong captcha or retry was done to try to avoid
                     * cutcaptcha.
                     */
                    logger.info("Entering password handling with known correct password: " + successfullyUsedFolderPassword);
                    passwords.add(0, successfullyUsedFolderPassword);
                } else if (StringUtils.isNotEmpty(lastUsedPassword)) {
                    logger.info("Trying last used password first: " + lastUsedPassword);
                    passwords.add(0, lastUsedPassword);
                }
                passwordLoop: while (true) {
                    passwordCounter++;
                    if (passwordCounter > maxPasswordRetries) {
                        logger.info("Stopping because: Too many wrong password attempts");
                        break passwordLoop;
                    }
                    logger.info("Password attempt: " + passwordCounter + " / " + maxPasswordRetries);
                    Form passwordForm = null;
                    /* Place current password field value on position [0]! */
                    final String[] possiblePasswordFieldKeys = new String[] { "pssw", "password__" };
                    String passwordFieldKey = null;
                    final Form[] allForms = br.getForms();
                    if (allForms != null && allForms.length != 0) {
                        findPwFormLoop: for (final Form aForm : allForms) {
                            for (final String possiblePasswordFieldKey : possiblePasswordFieldKeys) {
                                if (aForm.hasInputFieldByName(possiblePasswordFieldKey)) {
                                    logger.info("Found password form by hasInputFieldByName(passwordFieldKey) | passwordFieldKey = " + possiblePasswordFieldKey);
                                    passwordFieldKey = possiblePasswordFieldKey;
                                    passwordForm = aForm;
                                    break findPwFormLoop;
                                }
                            }
                        }
                    }
                    /* If there is captcha + password, password comes first, then captcha! */
                    if (passwordForm == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find pasword Form");
                    } else if (StringUtils.isEmpty(passwordFieldKey)) {
                        /* Developer mistake */
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "passwordFieldKey can't be empty");
                    }
                    String passCode = null;
                    /* Get next password of which we know that it isn't wrong. */
                    while (passwords.size() > 0) {
                        /* List of previously used passwords */
                        final String pw = passwords.remove(0);
                        if (!usedWrongPasswords.contains(pw)) {
                            passCode = pw;
                            break;
                        } else {
                            // no need to submit password that has already been tried!
                            logger.info("Skipping already tried wrong password: " + pw);
                            continue;
                        }
                    }
                    if (passCode == null) {
                        /* when previous provided passwords have failed -> Ask user */
                        passCode = getUserInput("Password?", param);
                        if (StringUtils.isEmpty(passCode)) {
                            /* Bad user input */
                            throw new DecrypterException(DecrypterException.PASSWORD);
                        }
                        if (!usedWrongPasswords.contains(passCode)) {
                            // no need to submit password that has already been tried!
                            logger.info("Skipping already tried password: " + passCode);
                            continue;
                        }
                    }
                    passwordForm.put(passwordFieldKey, Encoding.urlEncode(passCode));
                    submitForm(passwordForm);
                    if (!containsPassword(this.cleanHTML)) {
                        /* Success */
                        logger.info("Password success: " + passCode);
                        successfullyUsedFolderPassword = passCode;
                        break passwordLoop;
                    } else {
                        logger.info("Password failure | Wrong password: " + passCode);
                        usedWrongPasswords.add(passCode);
                        continue passwordLoop;
                    }
                }
                if (passwordCounter >= maxPasswordRetries && containsPassword(this.cleanHTML)) {
                    throw new DecrypterException(DecrypterException.PASSWORD);
                }
                if (!StringUtils.equals(this.getPluginConfig().getStringProperty(PROPERTY_PLUGIN_LAST_USED_PASSWORD), successfullyUsedFolderPassword)) {
                    /* Avoid log spam thus only log this if the password is new. */
                    logger.info("Saving correct password for future usage: " + successfullyUsedFolderPassword);
                }
                this.getPluginConfig().setProperty(PROPERTY_PLUGIN_LAST_USED_PASSWORD, successfullyUsedFolderPassword);
            }
            lastCaptchaIsCutCaptcha = false;
            if (containsCaptcha(this.cleanHTML)) {
                /* Process captcha */
                int captchaCounter = -1;
                final int maxCaptchaRetries = 10;
                captchaLoop: while (captchaCounter++ < maxCaptchaRetries && !this.isAbort()) {
                    logger.info("Captcha loop: " + captchaCounter + "/" + maxCaptchaRetries);
                    final boolean isLastLoop = captchaCounter >= maxCaptchaRetries;
                    Form captchaForm = null;
                    final Form[] forms = br.getForms();
                    if (forms != null && forms.length != 0) {
                        for (final Form form : forms) {
                            if (form.containsHTML("captcha") || AbstractRecaptchaV2.containsRecaptchaV2Class(form)) {
                                captchaForm = form;
                                break;
                            } else if (form.containsHTML("cform")) {
                                captchaForm = form;
                                break;
                            }
                        }
                    }
                    if (captchaForm == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find captchaForm");
                    }
                    final String captchaURL = captchaForm.getRegex("((https?://[^<>\"']*?)?/captcha/[^<>\"']*?)\"").getMatch(0);
                    if (captchaURL != null && captchaURL.contains("circle.php")) {
                        /* Click-captcha */
                        final File file = this.getLocalCaptchaFile();
                        getCaptchaBrowser(br).getDownload(file, captchaURL);
                        final ClickedPoint cp = getCaptchaClickedPoint(getHost(), file, param, "Click on the open circle");
                        if (cp == null) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        final InputField button = captchaForm.getInputFieldByType(InputField.InputType.IMAGE.name());
                        if (button == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        captchaForm.removeInputField(button);
                        captchaForm.put(button.getKey() + ".x", String.valueOf(cp.getX()));
                        captchaForm.put(button.getKey() + ".y", String.valueOf(cp.getY()));
                    } else if (captchaForm != null && captchaForm.containsHTML("=\"g-recaptcha\"")) {
                        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                        captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    } else if (captchaForm != null && captchaForm.containsHTML("capcode")) {
                        Challenge<String> challenge = new KeyCaptcha(this, br, createDownloadlink(url)).createChallenge(this);
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
                        lastCaptchaIsCutCaptcha = true;
                        if (cutCaptchaRetryIndex == 0 || tryToSolveCutCaptcha) {
                            logger.info("Attempting to solve CutCaptcha");
                            try {
                                final String cutcaptchaToken = new CaptchaHelperCrawlerPluginCutCaptcha(this, br, null).getToken();
                                captchaForm.put("cap_token", Encoding.urlEncode(cutcaptchaToken));
                                tryToSolveCutCaptcha = true;
                            } catch (final Exception e) {
                                if (tryToSolveCutCaptcha) {
                                    /* Handling worked before and failed now -> Throw exception */
                                    throw e;
                                }
                                logger.log(e);
                                logger.info("CutCaptcha failed - most likely no CutCaptcha solver is available");
                                /* Don't try again! */
                                tryToSolveCutCaptcha = false;
                            }
                        }
                        if (!tryToSolveCutCaptcha) {
                            logger.info("Trying to avoid cutcaptcha | cutCaptchaRetryIndex = " + cutCaptchaRetryIndex);
                            /* Clear cookies to increase the chances of getting a different captcha type than cutcaptcha. */
                            br.clearCookies(null);
                            /* Only wait if we know that we will try again */
                            if (!isLastLoop) {
                                sleep(1000, param);
                            }
                            /*
                             * Continue from the beginning. If a password was required, we already know the correct password and won't have
                             * to ask the user again.
                             */
                            continue cutcaptchaAvoidanceLoop;
                        }
                    } else {
                        /* Normal image captcha */
                        final String code = getCaptchaCode(captchaURL, param);
                        captchaForm.put("recaptcha_response_field", Encoding.urlEncode(code));
                    }
                    submitForm(captchaForm);
                    if (this.containsCaptcha(this.cleanHTML)) {
                        logger.info("User entered wrong captcha");
                        this.invalidateLastChallengeResponse();
                        continue captchaLoop;
                    } else {
                        logger.info("User entered correct captcha");
                        this.validateLastChallengeResponse();
                        captchaSuccess = true;
                        break captchaLoop;
                    }
                }
            } else {
                captchaSuccess = true;
            }
            /* Dead end: No reason to continue this loop here. */
            logger.info("Stepping out of cutCaptchaAvoidanceLoop");
            break cutcaptchaAvoidanceLoop;
        }
        if (!captchaSuccess) {
            if (cutCaptchaRetryIndex >= cutCaptchaAvoidanceMaxRetries && lastCaptchaIsCutCaptcha) {
                throw new DecrypterRetryException(RetryReason.CAPTCHA, "CUTCAPTCHA_IS_NOT_SUPPORTED_" + folderID, "Cutcaptcha is not supported! Please read: support.jdownloader.org/Knowledgebase/Article/View/cutcaptcha-not-supported");
            } else {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
    }

    private String handleLink(final Browser br, final CryptedLink param, final String singleLink, final int round) throws Exception {
        if (round >= 5) {
            /* Prevent endless recursive loop */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String domainPattern = buildHostsPatternPart(getPluginDomains().get(0));
        if (StringUtils.startsWithCaseInsensitive(singleLink, "http://") || StringUtils.startsWithCaseInsensitive(singleLink, "https://")) {
            br.getPage(singleLink);
        } else {
            br.getPage("/Link/" + singleLink + ".html");
        }
        if (br.containsHTML("friendlyduck\\.com/") || br.containsHTML(domainPattern + "/usenet\\.html") || br.containsHTML("powerusenet.xyz")) {
            /* Advertising */
            return "IGNORE";
        }
        int retryCaptcha = 5;
        while (!isAbort() && retryCaptcha-- > 0) {
            if (br.containsHTML("Security prompt")) {
                /* Rare case: Captcha required to access single link. */
                final String captcha = br.getRegex("(/captcha/[^<>\"]*?)\"").getMatch(0);
                if (captcha == null || !captcha.contains("circle.php")) {
                    logger.warning("Unsupported/unexpected captcha for single redirect link.");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final File file = this.getLocalCaptchaFile();
                getCaptchaBrowser(br).getDownload(file, captcha);
                final ClickedPoint cp = getCaptchaClickedPoint(getHost(), file, param, "Click on the open circle");
                if (cp == null) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                final Form form = new Form();
                form.setMethod(MethodType.POST);
                form.setAction(br.getURL());
                form.put("button.x", String.valueOf(cp.getX()));
                form.put("button.y", String.valueOf(cp.getY()));
                form.put("button", "send");
                br.submitForm(form);
            } else {
                break;
            }
        }
        String finallink = null;
        final String first_rd = br.getRedirectLocation();
        if (first_rd != null && first_rd.matches(".*" + domainPattern + "/.*")) {
            return handleLink(br, param, first_rd, round + 1);
        } else if (first_rd != null && !first_rd.matches(".*" + domainPattern + "/.*")) {
            finallink = first_rd;
        } else {
            final String nextlink = br.getRegex("(\"|')(https?://[^/]+/index\\.php\\?Action=(G|g)o[^<>\"']+)").getMatch(1);
            if (nextlink != null) {
                return handleLink(br, param, nextlink, round + 1);
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

    private ArrayList<DownloadLink> handleCnl2(final String url, final String password) throws Exception {
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
            if (password != null) {
                infos.put("passwords", password);
            }
            final String json = JSonStorage.toString(infos);
            final DownloadLink dl = createDownloadlink("http://dummycnl.jdownloader.org/" + HexFormatter.byteArrayToHex(json.getBytes("UTF-8")));
            ret.add(dl);
        }
        return ret;
    }

    private final boolean containsCaptcha(final String html) {
        if (new Regex(html, ">\\s*(?:Sicherheitsüberprüfung|Security prompt)\\s*</").patternFind()) {
            return true;
        } else if (containsCircleCaptcha(html)) {
            return true;
        } else {
            return false;
        }
    }

    private final boolean containsCircleCaptcha(final String html) {
        if (html.contains("circle.php")) {
            return true;
        } else {
            return false;
        }
    }

    private final boolean containsPassword(final String html) {
        if (new Regex(html, "(?i)>\\s*(?:Passwort erforderlich|Password required)\\s*</").patternFind()) {
            return true;
        } else {
            return false;
        }
    }

    private String cleanHTML = null;

    private final void cleanUpHTML(final Browser br) {
        String toClean = br.getRequest().getHtmlCode();
        final ArrayList<String> regexStuff = new ArrayList<String>();
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
        cleanUpHTML(br);
    }

    private final void postPage(final String url, final String post) throws Exception {
        if (url == null || post == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.postPage(url, post);
        cleanUpHTML(br);
    }

    private final void submitForm(final Form form) throws Exception {
        if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.submitForm(form);
        cleanUpHTML(br);
    }

    @Override
    public Class<? extends FileCryptConfig> getConfigInterface() {
        return FileCryptConfig.class;
    }
}