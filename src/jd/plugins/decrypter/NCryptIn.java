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
package jd.plugins.decrypter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.utils.JDUtilities;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ncrypt.in" }, urls = { "https?://(www\\.)?(ncrypt\\.in/(folder|link)\\-.{3,}|urlcrypt\\.com/open\\-[A-Za-z0-9]+)" })
public class NCryptIn extends antiDDoSForDecrypt {
    private final String reCaptcha              = "recaptcha/api/challenge";
    private final String aniCaptcha             = "/temp/anicaptcha/\\d+\\.gif";
    private final String circleCaptcha          = "\"/classes/captcha/circlecaptcha\\.php\"";
    private final String passwordInputFieldName = "password";
    private String       aBrowser               = "";

    @Override
    protected boolean useRUA() {
        return super.useRUA();
    }

    @Override
    protected void runPostRequestTask(Browser ibr) throws Exception {
        haveFun(ibr);
        simulateBrowser(ibr);
    }

    public NCryptIn(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        br = new Browser();
        dupe.clear();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("open-", "folder-").replace("urlcrypt.com/", "ncrypt.in/");
        br.setLoadLimit(16777216);
        if (parameter.contains("ncrypt.in/link")) {
            final String finallink = decryptSingle(parameter);
            if (finallink == null) {
                return null;
            }
            if (finallink.contains("error=crypted_id_invalid")) {
                logger.info("This link might be offline: " + parameter);
                final String additional = br.getRegex("<h2>\r?\n?(.*?)<").getMatch(0);
                if (additional != null) {
                    logger.info(additional);
                }
                decryptedLinks.add(createOfflinelink(parameter, new Regex(parameter, "ncrypt\\.in/link(.+)").getMatch(0), null));
                return decryptedLinks;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            br.setFollowRedirects(true);
            getPage(parameter);
            if (br.getURL().contains("error=crypted_id_invalid")) {
                logger.info("This link might be offline: " + parameter);
                final String additional = br.getRegex("<h2>\r?\n?(.*?)<").getMatch(0);
                if (additional != null) {
                    logger.info(additional);
                }
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            // Handle Captcha and/or password
            Form allForm = null;
            for (final Form tempForm : Form.getForms(aBrowser)) {
                if (tempForm.getStringProperty("name").equals("protected")) {
                    if (!tempForm.getRegex("name=\"submit_protected\"").matches()) {
                        continue;
                    }
                    allForm = tempForm;
                    break;
                }
            }
            br.getRequest().setHtmlCode(aBrowser);
            boolean containsPassword = allForm == null ? false : allForm.hasInputFieldByName(passwordInputFieldName);
            String password = null;
            if (containsPassword) {
                password = Plugin.getUserInput(null, param);
                if (StringUtils.isEmpty(password)) {
                    throw new DecrypterException(DecrypterException.PASSWORD);
                }
            }
            if (allForm != null) {
                final int maxRetries = 3;
                if (allForm.containsHTML(reCaptcha)) {
                    for (int i = 0; i <= maxRetries; i++) {
                        final Recaptcha rc = new Recaptcha(br, this);
                        rc.parse();
                        final Form f = rc.getForm();
                        // password first because no point solving the captcha if user doesn't know the password!
                        if (containsPassword) {
                            f.put(passwordInputFieldName, Encoding.urlEncode(password));
                        }
                        rc.load();
                        final File cf = rc.downloadCaptcha(this.getLocalCaptchaFile());
                        final String c = this.getCaptchaCode("recaptcha", cf, param);
                        f.put("recaptcha_challenge_field", rc.getChallenge());
                        f.put("recaptcha_response_field", Encoding.urlEncode(c));
                        submitForm(f);
                        // they show when password and captcha is incorrect.
                        if (isPasswordIncorrect(containsPassword)) {
                            // no retry
                            throw new DecrypterException(DecrypterException.PASSWORD);
                        } else if (isCaptchaIncorrect()) {
                            invalidateLastChallengeResponse();
                            if (i + 1 == maxRetries) {
                                throw new DecrypterException(DecrypterException.CAPTCHA);
                            }
                            continue;
                        } else {
                            validateLastChallengeResponse();
                            break;
                        }
                    }
                } else if (allForm.containsHTML(aniCaptcha) && !allForm.containsHTML("recaptcha_challenge")) {
                    for (int i = 0; i <= maxRetries; i++) {
                        final String captchaLink = new Regex(aBrowser, aniCaptcha).getMatch(-1);
                        if (captchaLink == null) {
                            return null;
                        }
                        // password first because no point solving the captcha if user doesn't know the password!
                        if (containsPassword) {
                            allForm.put(passwordInputFieldName, Encoding.urlEncode(password));
                        }
                        String code = null;
                        final File captchaFile = this.getLocalCaptchaFile(".gif");
                        try {
                            final Request r = br.cloneBrowser().createGetRequest(captchaLink);
                            r.getHeaders().put("Accept", "image/webp,image/*,*/*;q=0.8");
                            Browser.download(captchaFile, br.cloneBrowser().openRequestConnection(r));
                            jd.captcha.specials.Ncrypt.setDelay(captchaFile, 80);
                            code = getCaptchaCode(captchaFile, param);
                        } finally {
                            captchaFile.delete();
                        }
                        // for some reason we post form twice
                        allForm.put("captcha", Encoding.urlEncode(code));
                        submitForm(allForm);
                        // they show when password and captcha is incorrect.
                        if (isPasswordIncorrect(containsPassword)) {
                            // no retry
                            throw new DecrypterException(DecrypterException.PASSWORD);
                        } else if (isCaptchaIncorrect()) {
                            invalidateLastChallengeResponse();
                            if (i + 1 == maxRetries) {
                                throw new DecrypterException(DecrypterException.CAPTCHA);
                            }
                            continue;
                        } else {
                            validateLastChallengeResponse();
                            break;
                        }
                    }
                } else if (allForm.containsHTML(circleCaptcha) && !allForm.containsHTML("recaptcha_challenge")) {
                    for (int i = 0; i <= maxRetries; i++) {
                        // password first because no point solving the captcha if user doesn't know the password!
                        if (containsPassword) {
                            allForm.put(passwordInputFieldName, Encoding.urlEncode(password));
                        }
                        final File captchaFile = this.getLocalCaptchaFile(".png");
                        ClickedPoint cp = null;
                        try {
                            // test
                            final Request r = br.cloneBrowser().createGetRequest("/classes/captcha/circlecaptcha.php");
                            r.getHeaders().put("Accept", "image/webp,image/*,*/*;q=0.8");
                            Browser.download(captchaFile, br.cloneBrowser().openRequestConnection(r));
                            cp = getCaptchaClickedPoint(getHost(), captchaFile, param, null, "Click on the open circle");
                        } finally {
                            captchaFile.delete();
                        }
                        allForm.put("circle.x", String.valueOf(cp.getX()));
                        allForm.put("circle.y", String.valueOf(cp.getY()));
                        submitForm(allForm);
                        // they show when password and captcha is incorrect.
                        if (isPasswordIncorrect(containsPassword)) {
                            // no retry
                            throw new DecrypterException(DecrypterException.PASSWORD);
                        } else if (isCaptchaIncorrect()) {
                            invalidateLastChallengeResponse();
                            if (i + 1 == maxRetries) {
                                throw new DecrypterException(DecrypterException.CAPTCHA);
                            }
                            continue;
                        } else {
                            validateLastChallengeResponse();
                            break;
                        }
                    }
                } else if (containsPassword) {
                    // this one has to be last!
                    for (int i = 0; i <= maxRetries; i++) {
                        allForm.put(passwordInputFieldName, Encoding.urlEncode(password));
                        submitForm(allForm);
                        if (isPasswordIncorrect(containsPassword)) {
                            if (i + 1 == maxRetries) {
                                throw new DecrypterException(DecrypterException.PASSWORD);
                            }
                            continue;
                        }
                        break;
                    }
                }
            }
            String fpName = br.getRegex("<h1>(.*?)<img").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("title>nCrypt\\.in - (.*?)</tit").getMatch(0);
            }
            if (fpName == null) {
                fpName = br.getRegex("name=\"cnl2_output\"></iframe>[\t\n\r ]+<h2><span class=\"arrow\">(.*?)<img src=\"").getMatch(0);
            }
            // Container handling
            final String regexContainer = "/container/(?:dlc|rsdf|ccf)/([a-z0-9]+)\\.(dlc|rsdf|ccf)";
            final HashSet<String> dupeContainers = new HashSet<String>();
            final String[] containerIDs = br.getRegex(regexContainer).getColumn(-1);
            if (containerIDs != null && containerIDs.length != 0) {
                for (final String containerLink : containerIDs) {
                    // they can have multiple containers for given link, one for each hoster and files can be split up differently for each
                    // hoster (guess this depends on uploader). Load all containers with unique id. http://svn.jdownloader.org/issues/52633
                    final String containerHash = new Regex(containerLink, regexContainer).getMatch(0);
                    if (!dupeContainers.contains(containerHash)) {
                        // need to find a container with a result first.
                        final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
                        links.addAll(loadcontainer(containerLink));
                        if (!links.isEmpty()) {
                            decryptedLinks.addAll(links);
                            dupeContainers.add(containerHash);
                        }
                    }
                }
            }
            if (decryptedLinks.size() == 0 || (containerIDs == null || containerIDs.length == 0)) {
                // Webprotection decryption
                logger.info("ContainerID is null, trying webdecryption...");
                br.setFollowRedirects(false);
                String[] links = br.getRegex("\\'(https?://ncrypt\\.in/link-.*?=)\\'").getColumn(0);
                if (links == null || links.length == 0) {
                    links = br.getRegex("'(/link-\\d+)'").getColumn(0);
                }
                if (links == null || links.length == 0) {
                    logger.info("No links found, let's see if CNL2 is available!");
                    if (br.containsHTML("cnl2")) {
                        final Form cnl2 = br.getFormbyActionRegex("addcrypted2");
                        if (cnl2 != null) {
                            final InputField crypted = cnl2.getInputField("crypted");
                            final InputField jk = cnl2.getInputField("jk");
                            final InputField k = cnl2.getInputField("k");
                            final InputField source = cnl2.getInputField("source");
                            final DownloadLink dummyCNL = DummyCNL.createDummyCNL(getValue(crypted), getValue(jk), getValue(k), getValue(source));
                            decryptedLinks.add(dummyCNL);
                            return decryptedLinks;
                        }
                    }
                    logger.warning("Didn't find anything to decrypt, stopping...");
                    return null;
                }
                for (final String singleLink : links) {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted by user: " + parameter);
                        return decryptedLinks;
                    }
                    final String finallink = decryptSingle(singleLink);
                    if (finallink == null) {
                        logger.info("Found a broken link for link: " + parameter);
                        continue;
                    }
                    decryptedLinks.add(createDownloadlink(finallink));
                }
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    private boolean isCaptchaIncorrect() {
        final boolean hasFailed = new Regex(aBrowser, "<td class=\"error\">&bull; The securitycheck was wrong!\\s*</td>").matches();
        return hasFailed;
    }

    private boolean isPasswordIncorrect(final boolean containsPassword) {
        if (!containsPassword) {
            return false;
        }
        final boolean hasFailed = new Regex(aBrowser, "<td class=\"error\">&bull; This password is invalid!\\s*</td>").matches();
        return hasFailed;
    }

    private String getValue(final InputField inputField) {
        if (inputField != null) {
            return Encoding.urlDecode(inputField.getValue(), false);
        }
        return null;
    }

    private String decryptSingle(final String dcrypt) throws Exception {
        final Browser br = this.br.cloneBrowser();
        br.setFollowRedirects(false);
        getPage(br, dcrypt.replace("link-", "frame-"));
        final String finallink = br.getRedirectLocation();
        return finallink;
    }

    public void haveFun(final Browser br) throws Exception {
        final ArrayList<String> someStuff = new ArrayList<String>();
        final ArrayList<String> regexStuff = new ArrayList<String>();
        // regexStuff.add("(<!--.*?-->)");
        // regexStuff.add("(type=\"hidden\".*?(name=\".*?\")?.*?value=\".*?\")");
        // regexStuff.add("display:none;\">(.*?)</(div|span)>");
        // regexStuff.add("(<div class=\"hidden\" id=\"error_box\">.*?</div>)");
        // regexStuff.add("(<div class=\"\\w+\">.*?</div>)");
        // regexStuff.add("(<form name=\"protected\".*?style=\"display:none;\">.*?</form>)");
        regexStuff.add("(<table>.*?<!--.*?-->)");
        for (final String aRegex : regexStuff) {
            aBrowser = br.toString();
            final String replaces[] = br.getRegex(aRegex).getColumn(0);
            if (replaces != null && replaces.length != 0) {
                for (final String dingdang : replaces) {
                    someStuff.add(dingdang);
                }
            }
        }
        for (final String gaMing : someStuff) {
            aBrowser = aBrowser.replace(gaMing, "");
        }
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
                file = JDUtilities.getResourceFile("tmp/ncryptin/" + theLink);
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

    private LinkedHashSet<String> dupe = new LinkedHashSet<String>();

    private void simulateBrowser(final Browser br) throws InterruptedException {
        // dupe.clear();
        final AtomicInteger requestQ = new AtomicInteger(0);
        final AtomicInteger requestS = new AtomicInteger(0);
        final ArrayList<String> links = new ArrayList<String>();
        String[] l1 = new Regex(br, "\\s+(?:src)=(\"|')(.*?)\\1").getColumn(1);
        if (l1 != null) {
            links.addAll(Arrays.asList(l1));
        }
        l1 = new Regex(br, "\\s+(?:src)=(?!\"|')([^\\s]+)").getColumn(0);
        if (l1 != null) {
            links.addAll(Arrays.asList(l1));
        }
        for (final String link : links) {
            // lets only add links related to this hoster.
            final String correctedLink = Request.getLocation(link, br.getRequest());
            if (this.getHost().equals(Browser.getHost(correctedLink)) && !correctedLink.endsWith(this.getHost() + "/") && !correctedLink.contains(".html") && !correctedLink.equals(br.getURL()) && !correctedLink.contains("captcha/") && !correctedLink.contains("'")) {
                if (dupe.add(correctedLink)) {
                    final Thread simulate = new Thread("SimulateBrowser") {
                        public void run() {
                            final Browser rb = br.cloneBrowser();
                            rb.getHeaders().put("Cache-Control", null);
                            // open get connection for images, need to confirm
                            if (correctedLink.matches(".+\\.png.*")) {
                                rb.getHeaders().put("Accept", "image/webp,*/*;q=0.8");
                            } else if (correctedLink.matches(".+\\.js.*")) {
                                rb.getHeaders().put("Accept", "*/*");
                            } else if (correctedLink.matches(".+\\.css.*")) {
                                rb.getHeaders().put("Accept", "text/css,*/*;q=0.1");
                            }
                            URLConnectionAdapter con = null;
                            try {
                                requestQ.getAndIncrement();
                                con = openAntiDDoSRequestConnection(rb, rb.createGetRequest(correctedLink));
                            } catch (final Exception e) {
                            } finally {
                                try {
                                    con.disconnect();
                                } catch (final Exception e) {
                                }
                                requestS.getAndIncrement();
                            }
                            return;
                        }
                    };
                    simulate.start();
                    Thread.sleep(100);
                }
            }
        }
        while (requestQ.get() != requestS.get()) {
            Thread.sleep(1000);
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }
}