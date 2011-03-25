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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.swing.components.Balloon;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ncrypt.in" }, urls = { "http://(www\\.)?(ncrypt\\.in/folder\\-.+|urlcrypt\\.com/open\\-[A-Za-z0-9]+)" }, flags = { 0 })
public class NCryptIn extends PluginForDecrypt {

    private static final String             RECAPTCHA      = "recaptcha/api";
    private static final String             OTHERCAPTCHA   = "\"(/temp/anicaptcha/\\d+\\.gif)\"";
    private static final String             PASSWORDTEXT   = "password";
    private static final String             PASSWORDFAILED = "class=\"error\">\\&bull; Das Passwort ist ung\\&uuml;ltig";
    private static HashMap<String, Boolean> CNL_URL_MAP    = new HashMap<String, Boolean>();

    public NCryptIn(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String parameter = param.toString().replace("open-", "folder-").replace("urlcrypt.com/", "ncrypt.in/");
        br.getPage(parameter);
        if (br.getURL().contains("error=crypted_id_invalid")) { throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore.")); }
        // Handle Captcha and/or password
        Form allForm = br.getFormbyProperty("name", "protected");
        if (allForm != null && !(allForm.containsHTML("captcha") && !allForm.containsHTML("recaptcha_challenge")) && (!allForm.containsHTML(RECAPTCHA))) {
            allForm = br.getForm(2);
        }
        if (allForm != null) {
            if (allForm.containsHTML(RECAPTCHA)) {
                for (int i = 0; i <= 5; i++) {
                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.parse();
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode(cf, param);
                    if (allForm.containsHTML(PASSWORDTEXT)) {
                        final String passCode = getPassword(param);
                        rc.getForm().put(PASSWORDTEXT, passCode);
                    }
                    rc.setCode(c);
                    if (br.containsHTML(RECAPTCHA)) {
                        continue;
                    }
                    break;
                }
                if (br.containsHTML(PASSWORDFAILED)) { throw new DecrypterException(DecrypterException.PASSWORD); }
                if (br.containsHTML(RECAPTCHA)) { throw new DecrypterException(DecrypterException.CAPTCHA); }
            } else if (allForm.containsHTML("captcha") && !allForm.containsHTML("recaptcha_challenge")) {
                for (int i = 0; i <= 3; i++) {
                    final String captchaLink = br.getRegex(OTHERCAPTCHA).getMatch(0);
                    if (captchaLink == null) { return null; }

                    final File captchaFile = this.getLocalCaptchaFile(".gif");
                    Browser.download(captchaFile, br.cloneBrowser().openGetConnection("http://ncrypt.in" + captchaLink));
                    try {
                        jd.captcha.specials.Ncrypt.setDelay(captchaFile, 80);
                    } catch (final Throwable e) {
                        /* not existing in 09581 stable */
                    }
                    final String code = getCaptchaCode(captchaFile, param);

                    allForm.setAction(parameter);
                    allForm.put("captcha", code);
                    if (allForm.containsHTML(PASSWORDTEXT)) {
                        final String passCode = getPassword(param);
                        allForm.put(PASSWORDTEXT, passCode);
                    }
                    br.submitForm(allForm);
                    if (br.containsHTML(OTHERCAPTCHA)) {
                        continue;
                    }
                    break;
                }
                if (br.containsHTML(PASSWORDFAILED)) { throw new DecrypterException(DecrypterException.PASSWORD); }
                if (br.getRegex(OTHERCAPTCHA).getMatch(0) != null) { throw new DecrypterException(DecrypterException.CAPTCHA); }
            } else if (br.containsHTML(PASSWORDTEXT)) {
                for (int i = 0; i <= 3; i++) {
                    br.postPage(parameter, "password=" + getPassword(param) + "&submit_protected=Best%C3%A4tigen...+&submit_protected=Best%C3%A4tigen...+");
                    if (br.containsHTML(PASSWORDFAILED)) {
                        continue;
                    }
                    break;
                }
                if (br.containsHTML(PASSWORDFAILED)) { throw new DecrypterException(DecrypterException.PASSWORD); }
            }
        }
        String fpName = br.getRegex("<h1>(.*?)<img").getMatch(0);
        if (fpName == null) fpName = br.getRegex("name=\"cnl2_output\"></iframe>[\t\n\r ]+<h2><span class=\"arrow\">(.*?)<img src=\"").getMatch(0);
        // Container handling
        final String[] containerIDs = br.getRegex("/container/(rsdf|dlc|ccf)/([a-z0-9]+)\\.").getColumn(1);
        if (containerIDs != null && containerIDs.length != 0) {
            for (final String containerID : containerIDs) {
                ArrayList<DownloadLink> containerLinks = new ArrayList<DownloadLink>();
                if (br.containsHTML("\\.dlc")) {
                    containerLinks = loadcontainer("dlc/" + containerID + ".dlc");
                } else if (br.containsHTML("\\.rsdf")) {
                    containerLinks = loadcontainer("rsdf/" + containerID + ".rsdf");
                } else if (br.containsHTML("\\.ccf")) {
                    containerLinks = loadcontainer("ccf/" + containerID + ".ccf");
                }
                if (containerLinks != null) {
                    for (final DownloadLink containerLink : containerLinks) {
                        decryptedLinks.add(containerLink);
                    }
                }
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            // Webprotection decryption
            logger.info("ContainerID is null, trying webdecryption...");
            br.setFollowRedirects(false);
            final String[] links = br.getRegex("\\'(http://ncrypt\\.in/link-.*?=)\\'").getColumn(0);
            if (links == null || links.length == 0) {
                logger.info("No links found, let's see if CNL2 is available!");
                if (br.containsHTML("cnl2")) {
                    LocalBrowser.openDefaultURL(new URL(parameter));
                    NCryptIn.CNL_URL_MAP.put(parameter, Boolean.TRUE);
                    Balloon.show(JDL.L("jd.controlling.CNL2.checkText.title", "Click'n'Load"), null, JDL.L("jd.controlling.CNL2.checkText.message", "Click'n'Load URL opened"));
                    throw new DecrypterException(JDL.L("jd.controlling.CNL2.checkText.message", "Click'n'Load URL opened"));
                }
                logger.warning("Didn't find anything to decrypt, stopping...");
                return null;
            }
            progress.setRange(links.length);
            for (String singleLink : links) {
                singleLink = singleLink.replace("link-", "frame-");
                br.getPage(singleLink);
                final String finallink = br.getRedirectLocation();
                if (finallink == null) { return null; }
                decryptedLinks.add(createDownloadlink(finallink));
                progress.increase(1);
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String getPassword(final CryptedLink param) throws DecrypterException {
        final String passCode = getUserInput(null, param);
        return passCode;
    }

    private ArrayList<DownloadLink> loadcontainer(String theLink) throws IOException, PluginException {
        ArrayList<DownloadLink> decryptedLinks = null;
        final Browser brc = br.cloneBrowser();
        final String theID = theLink;
        theLink = "http://ncrypt.in/container/" + theLink;
        File file = null;
        final URLConnectionAdapter con = brc.openGetConnection(theLink);
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/ncryptin/" + theLink.replaceAll("(:|/)", "") + theID);
            if (file == null) { return null; }
            file.deleteOnExit();
            brc.downloadConnection(file, con);
            if (file != null && file.exists() && file.length() > 100) {
                decryptedLinks = JDUtilities.getController().getContainerLinks(file);
            }
        } else {
            con.disconnect();
            return null;
        }

        if (file != null && file.exists() && file.length() > 100) {
            if (decryptedLinks.size() > 0) { return decryptedLinks; }
        } else {
            return null;
        }
        return null;
    }
}
