//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
package jd.plugins.decrypter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.UserAgents;
import jd.utils.JDUtilities;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.antibotsystem.AntiBotSystem;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "relink.us" }, urls = { "https?://(www\\.)?relink\\.(?:us|to)/(?:(f/|(go|view|container_captcha)\\.php\\?id=)[0-9a-f]{30}|f/linkcrypt[0-9a-z]{15}|f/[a-f0-9]{10})" })
public class Rlnks extends antiDDoSForDecrypt {
    @Override
    public String[] siteSupportedNames() {
        return new String[] { "relink.to", "relink.us" };
    }

    private Form         allForm = null;
    public static Object LOCK    = new Object();
    private final String domains = "relink\\.(?:us|to)";

    public Rlnks(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String correctCryptedLink(final String input) {
        return input.replaceAll("(go|view|container_captcha)\\.php\\?id=", "f/");
        // they are not redirecting as of yet.
        // .replace("relink.us/", getHost() + "/");
    }

    private boolean decryptContainer(final String page, final String cryptedLink, final String containerFormat, final ArrayList<DownloadLink> decryptedLinks) throws IOException {
        final String containerURL = new Regex(page, "(/?download\\.php\\?id=[a-zA-z0-9]+\\&" + containerFormat + "=\\d+)").getMatch(0);
        if (containerURL != null) {
            final File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + containerFormat);
            final Browser browser = br.cloneBrowser();
            browser.getHeaders().put("Referer", cryptedLink);
            browser.getDownload(container, Encoding.htmlDecode(containerURL));
            decryptedLinks.addAll(loadContainerFile(container));
            container.delete();
            return decryptedLinks.isEmpty() ? false : true;
        }
        return false;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = correctCryptedLink(param.toString());
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", UserAgents.stringUserAgent());
        /* Handle Captcha and/or password */
        handleCaptchaAndPassword(parameter, param);
        if (!new Regex(br.getURL(), domains).matches()) {
            validateLastChallengeResponse();
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("<title>404</title>") || br.getURL().endsWith("/notfound.php")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (allForm != null && allForm.getRegex("password").matches()) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
        if (allForm != null && allForm.getRegex("captcha").matches()) {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        final String page = br.toString();
        String title = br.getRegex("shrink\"><th>(Titel|Baslik|Title)</th><td>(.*?)</td></tr>").getMatch(1);
        if (title != null && title.contains("No title")) {
            /* E.g. <i>No title</i> */
            title = null;
        }
        FilePackage fp = null;
        if (title != null && title.trim().length() > 0) {
            fp = FilePackage.getInstance();
            fp.setName(title);
            fp.setProperty("ALLOW_MERGE", true);
        }
        /* use cnl2 button if available */
        final String cnlUrl = "http://127\\.0\\.0\\.1:9666/flash/addcrypted2";
        if (br.containsHTML(cnlUrl)) {
            final Browser cnlbr = br.cloneBrowser();
            Form cnlForm = null;
            for (Form f : cnlbr.getForms()) {
                if (f.containsHTML(cnlUrl)) {
                    cnlForm = f;
                    break;
                }
            }
            if (cnlForm != null) {
                String jk = cnlbr.getRegex("<input type=\"hidden\" name=\"jk\" value=\"([^\"]+)\"").getMatch(0);
                String source = cnlForm.getInputField("source").getValue();
                if (StringUtils.isEmpty(source)) {
                    source = parameter.toString();
                } else {
                    source = Encoding.urlDecode(source, true);
                }
                final DownloadLink dl = DummyCNL.createDummyCNL(Encoding.urlDecode(cnlForm.getInputField("crypted").getValue(), false), jk, null, source);
                if (fp != null) {
                    fp.add(dl);
                }
                distribute(dl);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
        }
        if (!br.containsHTML("download.php\\?id=[a-f0-9]+") && !br.containsHTML("getFile\\(")) {
            return null;
        }
        if (!decryptContainer(page, parameter, "dlc", decryptedLinks)) {
            if (!decryptContainer(page, parameter, "ccf", decryptedLinks)) {
                decryptContainer(page, parameter, "rsdf", decryptedLinks);
            }
        }
        /* Webdecryption */
        if (decryptedLinks.isEmpty()) {
            decryptLinks(decryptedLinks, param);
            final String more_links[] = new Regex(page, Pattern.compile("<a href=\"(go\\.php\\?id=[a-zA-Z0-9]+\\&seite=\\d+)\">", Pattern.CASE_INSENSITIVE)).getColumn(0);
            for (final String link : more_links) {
                getPage(link);
                decryptLinks(decryptedLinks, param);
            }
        }
        if (decryptedLinks.isEmpty() && br.containsHTML(cnlUrl)) {
            throw new DecrypterException("CNL2 only, open this link in Browser");
        }
        validateLastChallengeResponse();
        if (fp != null) {
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void decryptLinks(final ArrayList<DownloadLink> decryptedLinks, final CryptedLink param) throws Exception {
        br.setFollowRedirects(false);
        final String[] matches = br.getRegex("getFile\\('(cid=\\w*?&lid=\\d*?)'\\)").getColumn(0);
        try {
            Browser brc = null;
            for (final String match : matches) {
                sleep(2333, param);
                handleCaptchaAndPassword("/frame.php?" + match, param);
                if (allForm != null && allForm.getRegex("captcha").matches()) {
                    logger.warning("Falsche Captcheingabe, Link wird übersprungen!");
                    continue;
                }
                brc = br.cloneBrowser();
                if (brc != null && brc.getRedirectLocation() != null && brc.getRedirectLocation().matches(".*?" + domains + "/getfile.*?")) {
                    getPage(brc, brc.getRedirectLocation());
                }
                if (brc.getRedirectLocation() != null) {
                    final DownloadLink dl = createDownloadlink(Encoding.htmlDecode(brc.getRedirectLocation()));
                    dl.setUrlProtection(org.jdownloader.controlling.UrlProtection.PROTECTED_DECRYPTER);
                    distribute(dl);
                    decryptedLinks.add(dl);
                } else {
                    final String url = brc.getRegex("iframe\\s*name=\"Container\".*?src=\"(https?://.*?)\"").getMatch(0);
                    if (url != null) {
                        final DownloadLink dl = createDownloadlink(Encoding.htmlDecode(url));
                        dl.setUrlProtection(org.jdownloader.controlling.UrlProtection.PROTECTED_DECRYPTER);
                        distribute(dl);
                        decryptedLinks.add(dl);
                    } else {
                        /* as bot detected */
                        return;
                    }
                }
            }
        } finally {
            br.setFollowRedirects(true);
        }
    }

    private void handleCaptchaAndPassword(final String partLink, final CryptedLink param) throws Exception {
        getPage(partLink);
        allForm = br.getFormbyProperty("name", "form");
        boolean b = allForm == null ? true : false;
        // 20150120 - raztoki
        if (allForm == null && br.containsHTML(">Please Wait\\.\\.\\.<") && br.containsHTML("class=\"timer\">\\d+</span>\\s*seconds</div>")) {
            // pile of redirects happen here
            final String link = br.getRegex("class=\"timer\">\\d+</span>\\s*seconds</div>\\s*<a href=\"\\s*(https?://(\\w+\\.)?" + domains + "/.*?)\\s*\"").getMatch(0);
            if (link != null) {
                getPage(link.trim());
                allForm = br.getFormbyProperty("name", "form");
                b = allForm == null ? true : false;
            } else {
                // possible plugin defect
                logger.warning("Possible Plugin Defect!");
            }
        }
        if (b) {
            allForm = br.getForm(0);
            allForm = allForm != null && allForm.getAction() != null && allForm.getAction().matches("^https?://(\\w+\\.)?" + domains + "/container_password\\d*\\.php.*") ? allForm : null;
        }
        if (allForm != null) {
            final List<String> passwords = getPreSetPasswords();
            for (int i = 0; i < 5; i++) {
                if (allForm.containsHTML("password")) {
                    final String passCode;
                    if (passwords.size() > 0) {
                        passCode = passwords.remove(0);
                        i = 0;
                    } else {
                        passCode = Plugin.getUserInput(null, param);
                    }
                    allForm.put("password", passCode);
                }
                if (allForm.containsHTML("captcha")) {
                    // fail over is circle, but they do randomly show antibotsystem captchas
                    if (AntiBotSystem.containsAntiBotSystem(allForm)) {
                        final AntiBotSystem abs = new AntiBotSystem(br, allForm);
                        final File captchaimage = abs.downloadCaptcha(getLocalCaptchaFile());
                        final String captchaCode = getCaptchaCode(captchaimage, param);
                        abs.setResponse(captchaCode);
                    } else {
                        allForm.remove("button");
                        final String captchaLink = allForm.getRegex("src=\"(.*?)\"").getMatch(0);
                        if (captchaLink == null) {
                            break;
                        }
                        if (StringUtils.containsIgnoreCase(captchaLink, "solvemedia.com")) {
                            final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                            try {
                                final File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                                final String code = getCaptchaCode("solvemedia", cf, param);
                                if ("".equals(code)) {
                                    // refresh (f5) button returns "", but so does a empty response by the user (send button)
                                    continue;
                                }
                                final String chid = sm.getChallenge(code);
                                allForm.put("adcopy_response", Encoding.urlEncode(code));
                                allForm.put("adcopy_challenge", Encoding.urlEncode(chid));
                            } catch (final Exception e) {
                                if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                                    throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                                }
                                throw e;
                            }
                        } else {
                            final File captchaFile = this.getLocalCaptchaFile();
                            Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaLink));
                            final ClickedPoint cp = getCaptchaClickedPoint(getHost(), captchaFile, param, getHost() + " | " + String.valueOf(i + 1) + "/5", null);
                            allForm.put("button.x", String.valueOf(cp.getX()));
                            allForm.put("button.y", String.valueOf(cp.getY()));
                        }
                    }
                }
                submitForm(allForm);
                if (br.getURL().contains("error.php")) {
                    getPage(partLink);
                    allForm = br.getFormbyProperty("name", "form");
                    continue;
                }
                allForm = br.getFormbyProperty("name", "form");
                allForm = allForm == null && b ? br.getForm(0) : allForm;
                if (allForm != null && (allForm.containsHTML("password") || allForm.containsHTML("captcha"))) {
                    continue;
                }
                allForm = null;
                break;
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }
}