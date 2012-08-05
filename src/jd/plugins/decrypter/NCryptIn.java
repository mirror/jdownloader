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

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ncrypt.in" }, urls = { "http://(www\\.)?(ncrypt\\.in/(folder|link)\\-{3,}|urlcrypt\\.com/open\\-[A-Za-z0-9]+)" }, flags = { 0 })
public class NCryptIn extends PluginForDecrypt {

    private static final String            RECAPTCHA      = "recaptcha/api/challenge";
    private static final String            ANICAPTCHA     = "/temp/anicaptcha/\\d+\\.gif";
    private static final String            CIRCLECAPTCHA  = "\"/classes/captcha/circlecaptcha\\.php\"";
    private static final String            PASSWORDTEXT   = "password";
    private static final String            PASSWORDFAILED = "<h2><span class=\"arrow\">Gesch\\&uuml;tzter Ordner</span></h2>";
    private final HashMap<String, Boolean> CNL_URL_MAP    = new HashMap<String, Boolean>();
    private String                         aBrowser       = "";

    public NCryptIn(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("open-", "folder-").replace("urlcrypt.com/", "ncrypt.in/");
        if (parameter.contains("ncrypt.in/link")) {
            final String finallink = decryptSingle(parameter);
            if (finallink == null) { return null; }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            br.setFollowRedirects(true);
            br.getPage(parameter);
            haveFun();
            if (br.getURL().contains("error=crypted_id_invalid")) { throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore.")); }
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
            boolean password = false;
            boolean captcha = false;
            if (allForm != null) {
                if (allForm.containsHTML(RECAPTCHA)) {
                    captcha = true;
                    for (int i = 0; i <= 5; i++) {
                        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                        rc.parse();
                        rc.load();
                        final File cf = rc.downloadCaptcha(this.getLocalCaptchaFile());
                        final String c = this.getCaptchaCode(cf, param);
                        if (allForm.containsHTML(PASSWORDTEXT)) {
                            final String passCode = getPassword(param);
                            rc.getForm().put(PASSWORDTEXT, passCode);
                        }
                        rc.setCode(c);
                        haveFun();
                        if (new Regex(aBrowser, RECAPTCHA).matches()) {
                            continue;
                        }
                        break;
                    }
                    if (password && new Regex(aBrowser, PASSWORDFAILED).matches()) { throw new DecrypterException(DecrypterException.PASSWORD); }
                    if (captcha && new Regex(aBrowser, RECAPTCHA).matches()) { throw new DecrypterException(DecrypterException.CAPTCHA); }
                } else if (allForm.containsHTML(ANICAPTCHA) && !allForm.containsHTML("recaptcha_challenge")) {
                    captcha = true;
                    for (int i = 0; i <= 3; i++) {
                        final String captchaLink = new Regex(aBrowser, ANICAPTCHA).getMatch(-1);
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
                        haveFun();
                        if (new Regex(aBrowser, ANICAPTCHA).matches()) {
                            continue;
                        }
                        break;
                    }
                    if (password && new Regex(aBrowser, PASSWORDFAILED).matches()) { throw new DecrypterException(DecrypterException.PASSWORD); }
                    if (captcha && new Regex(aBrowser, ANICAPTCHA).matches()) { throw new DecrypterException(DecrypterException.CAPTCHA); }
                } else if (allForm.containsHTML(CIRCLECAPTCHA) && !allForm.containsHTML("recaptcha_challenge")) {
                    captcha = true;
                    for (int i = 0; i <= 3; i++) {
                        final File captchaFile = this.getLocalCaptchaFile(".png");
                        Browser.download(captchaFile, br.cloneBrowser().openGetConnection("http://ncrypt.in/classes/captcha/circlecaptcha.php"));
                        final Point p = UserIO.getInstance().requestClickPositionDialog(captchaFile, "Click on the open circle", null);
                        if (p == null) { throw new DecrypterException(DecrypterException.CAPTCHA); }
                        allForm.put("circle.x", String.valueOf(p.x));
                        allForm.put("circle.y", String.valueOf(p.y));
                        if (allForm.containsHTML(PASSWORDTEXT)) {
                            final String passCode = getPassword(param);
                            allForm.put(PASSWORDTEXT, passCode);
                        }
                        br.submitForm(allForm);
                        haveFun();
                        if (new Regex(aBrowser, CIRCLECAPTCHA).matches()) {
                            continue;
                        }
                        break;
                    }
                    if (password && new Regex(aBrowser, PASSWORDFAILED).matches()) { throw new DecrypterException(DecrypterException.PASSWORD); }
                    if (captcha && new Regex(aBrowser, ANICAPTCHA).matches()) { throw new DecrypterException(DecrypterException.CAPTCHA); }
                    if (captcha && new Regex(aBrowser, CIRCLECAPTCHA).matches()) { throw new DecrypterException(DecrypterException.CAPTCHA); }
                } else if (allForm.containsHTML(PASSWORDTEXT)) {
                    password = true;
                    for (int i = 0; i <= 3; i++) {
                        allForm.put(PASSWORDTEXT, getPassword(param));
                        br.submitForm(allForm);
                        haveFun();
                        if (new Regex(aBrowser, PASSWORDFAILED).matches()) {
                            continue;
                        }
                        break;
                    }
                    if (new Regex(aBrowser, PASSWORDFAILED).matches()) { throw new DecrypterException(DecrypterException.PASSWORD); }
                }
            }
            String fpName = br.getRegex("<h1>(.*?)<img").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("name=\"cnl2_output\"></iframe>[\t\n\r ]+<h2><span class=\"arrow\">(.*?)<img src=\"").getMatch(0);
            }
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
                        CNL_URL_MAP.put(parameter, Boolean.TRUE);
                        throw new DecrypterException(JDL.L("jd.controlling.CNL2.checkText.message", "Click'n'Load URL opened"));
                    }
                    logger.warning("Didn't find anything to decrypt, stopping...");
                    return null;
                }
                progress.setRange(links.length);
                for (final String singleLink : links) {
                    final String finallink = decryptSingle(singleLink);
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
        }
        return decryptedLinks;
    }

    private String decryptSingle(final String dcrypt) throws IOException {
        br.setFollowRedirects(false);
        br.getPage(dcrypt.replace("link-", "frame-"));
        final String finallink = br.getRedirectLocation();
        return finallink;
    }

    private String getPassword(final CryptedLink param) throws DecrypterException {
        final String passCode = Plugin.getUserInput(null, param);
        return passCode;
    }

    public void haveFun() throws Exception {
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

    private ArrayList<DownloadLink> loadcontainer(String theLink) throws IOException, PluginException {
        ArrayList<DownloadLink> decryptedLinks = null;
        final Browser brc = br.cloneBrowser();
        final String theID = theLink;
        theLink = "http://ncrypt.in/container/" + theLink;
        File file = null;
        URLConnectionAdapter con = null;
        try {
            con = brc.openGetConnection(theLink);
            if (con.getResponseCode() == 200) {
                file = JDUtilities.getResourceFile("tmp/ncryptin/" + theID);
                if (file == null) { return null; }
                file.getParentFile().mkdirs();
                file.deleteOnExit();
                brc.downloadConnection(file, con);
                if (file != null && file.exists() && file.length() > 100) {
                    decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }

        if (file != null && file.exists() && file.length() > 100) {
            if (decryptedLinks.size() > 0) { return decryptedLinks; }
        }
        return null;
    }
}
