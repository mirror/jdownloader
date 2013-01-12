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

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "relink.us" }, urls = { "http://(www\\.)?relink\\.us/(f/|(go|view|container_captcha)\\.php\\?id=)[0-9a-f]+" }, flags = { 0 })
public class Rlnks extends PluginForDecrypt {

    ProgressController   PROGRESS;
    private Form         ALLFORM = null;
    private String       UA      = RandomUserAgent.generate();
    public static Object LOCK    = new Object();

    public Rlnks(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String correctCryptedLink(final String input) {
        return input.replaceAll("(go|view|container_captcha)\\.php\\?id=", "f/");
    }

    private boolean decryptContainer(final String page, final String cryptedLink, final String containerFormat, final ArrayList<DownloadLink> decryptedLinks) throws IOException {
        final String containerURL = new Regex(page, "(download\\.php\\?id=[a-zA-z0-9]+\\&" + containerFormat + "=\\d+)").getMatch(0);
        if (containerURL != null) {
            final File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + containerFormat);
            final Browser browser = br.cloneBrowser();
            browser.getHeaders().put("Referer", cryptedLink);
            browser.getDownload(container, "http://relink.us/" + Encoding.htmlDecode(containerURL));
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
            return true;
        }
        return false;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        synchronized (LOCK) {
            PROGRESS = progress;
            final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            final String parameter = correctCryptedLink(param.toString());
            setBrowserExclusive();
            br.setFollowRedirects(true);
            br.getHeaders().put("User-Agent", UA);

            /* Handle Captcha and/or password */
            handleCaptchaAndPassword(parameter, param);
            if (!br.getURL().contains("relink.us/")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (ALLFORM != null && ALLFORM.getRegex("password").matches()) { throw new DecrypterException(DecrypterException.PASSWORD); }
            if (ALLFORM != null && ALLFORM.getRegex("captcha").matches()) { throw new DecrypterException(DecrypterException.CAPTCHA); }

            final String page = br.toString();
            progress.setRange(0);

            /* use cnl2 button if available */
            String cnlUrl = "http://127\\.0\\.0\\.1:9666/flash/addcrypted2";
            if (br.containsHTML(cnlUrl)) {
                final Browser cnlbr = br.cloneBrowser();

                Form cnlForm = null;
                for (Form f : cnlbr.getForms()) {
                    if (f.containsHTML(cnlUrl)) cnlForm = f;
                }
                if (cnlForm != null) {
                    String jk = cnlbr.getRegex("<input type=\"hidden\" name=\"jk\" value=\"([^\"]+)\"").getMatch(0);
                    cnlForm.remove("jk");
                    cnlForm.put("jk", (jk != null ? jk.replaceAll("\\+", "%2B") : "nothing"));
                    try {
                        cnlbr.submitForm(cnlForm);
                        if (cnlbr.containsHTML("success")) return decryptedLinks;
                        if (cnlbr.containsHTML("^failed")) {
                            logger.warning("relink.us: CNL2 Postrequest was failed! Please upload now a logfile, contact our support and add this loglink to your bugreport!");
                            logger.warning("relink.us: CNL2 Message: " + cnlbr.toString());
                        }
                    } catch (Throwable e) {
                        logger.info("relink.us: ExternInterface(CNL2) is disabled!");
                    }
                }
            }
            if (!br.containsHTML("download.php\\?id=[a-f0-9]+") && !br.containsHTML("getFile\\(")) return null;
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
                    br.getPage("http://relink.us/" + link);
                    decryptLinks(decryptedLinks, param);
                }
            }
            if (decryptedLinks.isEmpty() && br.containsHTML(cnlUrl)) throw new DecrypterException("CNL2 only, open this link in Browser");
            return decryptedLinks;
        }
    }

    private void decryptLinks(final ArrayList<DownloadLink> decryptedLinks, final CryptedLink param) throws Exception {
        br.setFollowRedirects(false);
        final String[] matches = br.getRegex("getFile\\('(cid=\\w*?&lid=\\d*?)'\\)").getColumn(0);
        try {
            Browser brc = null;
            PROGRESS.addToMax(matches.length);
            for (final String match : matches) {
                Thread.sleep(2333);
                handleCaptchaAndPassword("http://www.relink.us/frame.php?" + match, param);
                if (ALLFORM != null && ALLFORM.getRegex("captcha").matches()) {
                    logger.warning("Falsche Captcheingabe, Link wird übersprungen!");
                    continue;
                }
                brc = br.cloneBrowser();
                if (brc != null && brc.getRedirectLocation() != null && brc.getRedirectLocation().contains("relink.us/getfile")) {
                    brc.getPage(brc.getRedirectLocation());
                }
                if (brc.getRedirectLocation() != null) {
                    final DownloadLink dl = createDownloadlink(Encoding.htmlDecode(brc.getRedirectLocation()));
                    try {
                        distribute(dl);
                    } catch (final Throwable e) {
                        /* does not exist in 09581 */
                    }
                    decryptedLinks.add(dl);
                    break;
                } else {
                    final String url = brc.getRegex("iframe.*?src=\"(.*?)\"").getMatch(0);
                    final DownloadLink dl = createDownloadlink(Encoding.htmlDecode(url));
                    if (url != null) {
                        try {
                            distribute(dl);
                        } catch (final Throwable e) {
                            /* does not exist in 09581 */
                        }
                        decryptedLinks.add(dl);
                    } else {
                        /* as bot detected */
                        return;
                    }
                }
                PROGRESS.increase(1);
            }
        } finally {
            br.setFollowRedirects(true);
        }
    }

    private void handleCaptchaAndPassword(final String partLink, final CryptedLink param) throws Exception {
        br.getPage(partLink);
        ALLFORM = br.getFormbyProperty("name", "form");
        boolean b = ALLFORM == null ? true : false;
        if (b) {
            ALLFORM = br.getForm(0);
            ALLFORM = ALLFORM != null && ALLFORM.getAction().startsWith("http://www.relink.us/container_password.php") ? ALLFORM : null;
        }
        if (ALLFORM != null) {
            for (int i = 0; i < 5; i++) {
                if (ALLFORM.containsHTML("password")) {
                    final String passCode = Plugin.getUserInput(null, param);
                    ALLFORM.put("password", passCode);
                }
                if (ALLFORM.containsHTML("captcha")) {
                    ALLFORM.remove("button");
                    final String captchaLink = ALLFORM.getRegex("src=\"(.*?)\"").getMatch(0);
                    if (captchaLink == null) {
                        break;
                    }
                    final File captchaFile = this.getLocalCaptchaFile();
                    Browser.download(captchaFile, br.cloneBrowser().openGetConnection("http://www.relink.us/" + captchaLink));
                    final Point p = UserIO.getInstance().requestClickPositionDialog(captchaFile, "relink.us | " + String.valueOf(i + 1) + "/5", null);
                    if (p == null) { throw new DecrypterException(DecrypterException.CAPTCHA); }
                    ALLFORM.put("button.x", String.valueOf(p.x));
                    ALLFORM.put("button.y", String.valueOf(p.y));
                }
                br.submitForm(ALLFORM);
                if (br.getURL().contains("error.php")) {
                    br.getPage(partLink);
                }
                ALLFORM = br.getFormbyProperty("name", "form");
                ALLFORM = ALLFORM == null && b ? br.getForm(0) : ALLFORM;
                if (ALLFORM != null && ALLFORM.getAction().startsWith("http://www.relink.us/container_password.php")) continue;
                ALLFORM = null;
                break;
            }
        }
    }

}