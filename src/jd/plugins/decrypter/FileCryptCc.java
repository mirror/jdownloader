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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.CaptchaException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.UserAgents;
import jd.plugins.components.UserAgents.BrowserName;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "filecrypt.cc" }, urls = { "https?://(?:www\\.)?filecrypt\\.cc/Container/([A-Z0-9]{10,16})(\\.html\\?mirror=\\d+)?" })
public class FileCryptCc extends PluginForDecrypt {
    private final String                   NO_SOLVEMEDIA      = "1";
    private String                         userretrys         = "10";
    private static AtomicReference<String> LAST_USED_PASSWORD = new AtomicReference<String>();

    public FileCryptCc(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br = new Browser();
        br.setLoadLimit(br.getLoadLimit() * 2);
        final String agent = UserAgents.stringUserAgent(BrowserName.Chrome);
        br.getHeaders().put("User-Agent", agent);
        br.getHeaders().put("Accept-Encoding", "gzip, deflate, sdch");
        br.getHeaders().put("Accept-Language", "en");
        br.setFollowRedirects(true);
        br.addAllowedResponseCodes(500);// submit captcha responds with 500 code
        final String uid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        // not all captcha types are skipable (recaptchav2 isn't). I tried with new response value - raztoki
        final String containsMirror = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        if (containsMirror == null) {
            getPage(parameter + "/.html");
        } else {
            getPage(parameter);
        }
        if (br.getURL().contains("filecrypt.cc/404.html")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        // Separate password and captcha. this is easier for count reasons!
        int counter = -1;
        final int retry = Integer.parseInt(userretrys);
        final List<String> passwords = getPreSetPasswords();
        final HashSet<String> avoidRetry = new HashSet<String>();
        final String lastUsedPassword = LAST_USED_PASSWORD.get();
        if (StringUtils.isNotEmpty(lastUsedPassword)) {
            passwords.add(0, lastUsedPassword);
        }
        String usedPassword = null;
        while (counter++ < retry && containsPassword()) {
            Form passwordForm = null;
            final Form[] allForms = br.getForms();
            if (allForms != null && allForms.length != 0) {
                for (final Form aForm : allForms) {
                    if (aForm.containsHTML("password")) {
                        passwordForm = aForm;
                        break;
                    }
                }
            }
            /* If there is captcha + password, password comes first, then captcha! */
            if (passwordForm != null) {
                final String passCode;
                if (passwords.size() > 0) {
                    passCode = passwords.remove(0);
                    if (!avoidRetry.add(passCode)) {
                        counter--;
                        continue;
                    }
                } else {
                    // when previous provided password has failed, or not provided we should ask
                    passCode = getUserInput("Password?", param);
                    if (passCode == null) {
                        throw new DecrypterException(DecrypterException.CAPTCHA);
                    } else {
                        if (!avoidRetry.add(passCode)) {
                            // no need to submit password that has already been tried!
                            continue;
                        }
                    }
                }
                usedPassword = passCode;
                passwordForm.put("password", Encoding.urlEncode(passCode));
                submitForm(passwordForm);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Could not find pasword form");
            }
        }
        if (usedPassword != null) {
            LAST_USED_PASSWORD.set(usedPassword);
        }
        if (counter == retry && containsPassword()) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
        // captcha time!
        counter = -1;
        while (counter++ < retry && containsCaptcha()) {
            Form captchaForm = null;
            final Form[] allForms = br.getForms();
            if (allForms != null && allForms.length != 0) {
                for (final Form aForm : allForms) {
                    if (aForm.containsHTML("captcha")) {
                        captchaForm = aForm;
                        break;
                    }
                }
            }
            final String captcha = captchaForm != null ? captchaForm.getRegex("(/captcha/[^<>\"]*?)\"").getMatch(0) : null;
            if (captcha != null && captcha.contains("circle.php")) {
                final File file = this.getLocalCaptchaFile();
                br.cloneBrowser().getDownload(file, captcha);
                final ClickedPoint cp = getCaptchaClickedPoint(getHost(), file, param, null, "Click on the open circle");
                captchaForm.put("button.x", String.valueOf(cp.getX()));
                captchaForm.put("button.y", String.valueOf(cp.getY()));
                captchaForm.remove("button");
                submitForm(captchaForm);
            } else if (captchaForm != null && captchaForm.containsHTML("=\"g-recaptcha\"")) {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                submitForm(captchaForm);
            } else if (captchaForm != null && captchaForm.containsHTML("solvemedia\\.com/papi/")) {
                if (getPluginConfig().getBooleanProperty(NO_SOLVEMEDIA, false) == false) {
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
                        if (counter + 1 < retry) {
                            continue;
                        } else {
                            throw new DecrypterException(DecrypterException.CAPTCHA);
                        }
                    }
                    final String chid = sm.getChallenge(code);
                    captchaForm.put("adcopy_response", Encoding.urlEncode(code));
                    captchaForm.put("adcopy_challenge", chid);
                    submitForm(captchaForm);
                } else {
                    continue;
                }
            } else if (captchaForm != null && captchaForm.containsHTML("capcode")) {
                Challenge<String> challenge = new KeyCaptcha(this, br, createDownloadlink(parameter)).createChallenge(this);
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
                submitForm(captchaForm);
            } else if (captcha != null) {
                // they use recaptcha response field key for non recaptcha.. math sum and text =
                // http://filecrypt.cc/captcha/captcha.php?namespace=container
                // using bismarck original observation, this type is skipable.
                if (counter > 0) {
                    final String code = getCaptchaCode(captcha, param);
                    if (StringUtils.isEmpty(code)) {
                        if (counter + 1 < retry) {
                            continue;
                        } else {
                            throw new DecrypterException(DecrypterException.CAPTCHA);
                        }
                    }
                    captchaForm.put("recaptcha_response_field", Encoding.urlEncode(code));
                } else {
                    captchaForm.put("recaptcha_response_field", "");
                }
                submitForm(captchaForm);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Could not find captcha form");
            }
        }
        if (counter == retry && containsCaptcha()) {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        final String fpName = br.getRegex("<h2>([^<>\"]*?)<").getMatch(0);
        // mirrors - note: containers no longer have uid within path! -raztoki20160117
        // mirrors - note: containers can contain uid within path... -raztoki20161204
        String[] mirrors = br.getRegex("\"([^\"]*/Container/[A-Z0-9]+" + (containsMirror != null ? Pattern.quote(containsMirror) : "\\.html\\?mirror=\\d+") + ")\"").getColumn(0);
        if (mirrors.length < 1) {
            mirrors = new String[1];
            mirrors[0] = parameter + "?mirror=0";
        } else {
            // first mirror shown should be mirror 0;
            Arrays.sort(mirrors);
        }
        for (String mirror : mirrors) {
            // if 0 we don't need to get new page
            if (!mirror.endsWith("mirror=0")) {
                br.getPage(mirror);
            }
            final ArrayList<DownloadLink> tdl = new ArrayList<DownloadLink>();
            // Use clicknload first as it doesn't rely on JD service.jdownloader.org, which can go down!
            handleCnl2(tdl, parameter);
            if (!tdl.isEmpty()) {
                decryptedLinks.addAll(tdl);
                if (fpName != null) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(Encoding.htmlDecode(fpName.trim()));
                    fp.addLinks(decryptedLinks);
                }
                if (containsMirror != null) {
                    return decryptedLinks;
                }
                continue;
            }
            /* Second try DLC, then single links */
            final String dlc_id = br.getRegex("DownloadDLC\\('([^<>\"]*?)'\\)").getMatch(0);
            if (dlc_id != null) {
                logger.info("DLC found - trying to add it");
                tdl.addAll(loadcontainer("http://filecrypt.cc/DLC/" + dlc_id + ".dlc"));
                if (tdl.isEmpty()) {
                    logger.warning("DLC is empty or something is broken!");
                    continue;
                }
                decryptedLinks.addAll(tdl);
            }
        }
        if (!decryptedLinks.isEmpty()) {
            logger.info("DLC successfully added");
            return decryptedLinks;
        }
        // this isn't always shown, see 104061178D - raztoki 20141118
        logger.info("Trying single link handling");
        final String[] links = br.getRegex("openLink\\('([^<>\"]*?)'").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.setFollowRedirects(false);
        br.setCookie(this.getHost(), "BetterJsPopCount", "1");
        for (final String singleLink : links) {
            final Browser br2 = br.cloneBrowser();
            br2.getPage("/Link/" + singleLink + ".html");
            if (br2.containsHTML("friendlyduck.com/") || br2.containsHTML("filecrypt\\.cc/usenet\\.html") || br2.containsHTML("share-online\\.biz/affiliate")) {
                /* Advertising */
                continue;
            }
            String finallink = null;
            final String first_rd = br2.getRedirectLocation();
            if (first_rd != null && first_rd.contains("filecrypt.cc/")) {
                br2.getPage(first_rd);
                finallink = br2.getRedirectLocation();
            } else if (first_rd != null && !first_rd.contains("filecrypt.cc/")) {
                finallink = first_rd;
            } else {
                final String nextlink = br2.getRegex("(\"|')(https?://(www\\.)?filecrypt\\.cc/index\\.php\\?Action=(G|g)o[^<>\"']+)").getMatch(1);
                if (nextlink != null) {
                    br2.getPage(nextlink);
                    finallink = br2.getRedirectLocation();
                }
            }
            if (finallink == null || finallink.contains("filecrypt.cc/")) {
                // commented these out so that unhandled ads or what ever don't kill failover.
                // logger.warning("Decrypter broken for link: " + parameter);
                // return null;
                continue;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void handleCnl2(final ArrayList<DownloadLink> decryptedLinks, final String parameter) throws Exception {
        final Form[] forms = br.getForms();
        Form CNLPOP = null;
        Form cnl = null;
        for (final Form f : forms) {
            if (f.containsHTML("CNLPOP")) {
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
        }
        if (cnl == null) {
            for (final Form f : forms) {
                if (f.hasInputFieldByName("jk")) {
                    cnl = f;
                    break;
                }
            }
        }
        if (cnl != null) {
            final HashMap<String, String> infos = new HashMap<String, String>();
            infos.put("crypted", Encoding.urlDecode(cnl.getInputField("crypted").getValue(), false));
            infos.put("jk", Encoding.urlDecode(cnl.getInputField("jk").getValue(), false));
            String source = cnl.getInputField("source").getValue();
            if (StringUtils.isEmpty(source)) {
                source = parameter.toString();
            } else {
                infos.put("source", source);
            }
            infos.put("source", source);
            final String json = JSonStorage.toString(infos);
            final DownloadLink dl = createDownloadlink("http://dummycnl.jdownloader.org/" + HexFormatter.byteArrayToHex(json.getBytes("UTF-8")));
            decryptedLinks.add(dl);
        }
    }

    private final boolean containsCaptcha() {
        return new Regex(cleanHTML, containsCaptcha).matches();
    }

    private final boolean containsPassword() {
        return new Regex(cleanHTML, "<h2>(?:Passwort erforderlich|Password required)</h2>").matches();
    }

    private final String containsCaptcha = "<h2>(?:Sicherheitsüberprüfung|Security prompt)</h2>";
    private String       cleanHTML       = null;

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

    private final void getPage(final String page) throws IOException, PluginException {
        if (page == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(page);
        cleanUpHTML();
    }

    private final void postPage(final String url, final String post) throws IOException, PluginException {
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

    private void setConfiguredDomain() {
        final int chosenRetrys = getPluginConfig().getIntegerProperty(retrys, 0);
        userretrys = this.allretrys[chosenRetrys];
    }

    private final String   retrys    = "retrys";
    private final String[] allretrys = new String[] { "10", "15", "20" };

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), NO_SOLVEMEDIA, JDL.L("plugins.decrypter.filecryptcc.nosolvemedia", "No solvemedia?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), retrys, allretrys, JDL.L("plugins.decrypter.filecryptcc.retrys", "Retrys")).setDefaultValue(0));
    }
}