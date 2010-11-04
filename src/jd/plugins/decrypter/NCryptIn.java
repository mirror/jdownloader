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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ncrypt.in" }, urls = { "http://(www\\.)?ncrypt\\.in/folder-.*?=" }, flags = { 0 })
public class NCryptIn extends PluginForDecrypt {

    public NCryptIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String RECAPTCHA         = "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)";
    private static final String OTHERCAPTCHA      = "\"(/temp/anicaptcha/\\d+\\.gif)\"";
    private static final String PASSWORDPARAMETER = "password";
    private static final String PASSWORDTEXT      = "<th>Passwort:</th>";
    private static final String PASSWORDFAILED    = "class=\"error\">\\&bull; Das Passwort ist ung\\&uuml;ltig";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("(<h1>Dieser Ordner/Link existiert nicht\\.\\.\\.</h1>|<li>Die URL ist nicht korrekt</li>|<li>Der Ordner/Link wurde Abused</li>)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        // Handle Captcha and/or password
        if (br.containsHTML(RECAPTCHA)) {
            for (int i = 0; i <= 5; i++) {
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.parse();
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, param);
                if (br.containsHTML(PASSWORDTEXT)) {
                    String passCode = getPassword(param);
                    rc.getForm().put(PASSWORDPARAMETER, passCode);
                }
                rc.setCode(c);
                if (br.containsHTML(RECAPTCHA) || br.containsHTML(PASSWORDTEXT)) continue;
                break;
            }
            if (br.containsHTML(PASSWORDFAILED)) throw new DecrypterException(DecrypterException.PASSWORD);
            if (br.containsHTML(RECAPTCHA)) throw new DecrypterException(DecrypterException.CAPTCHA);
        } else if (br.getRegex(OTHERCAPTCHA).getMatch(0) != null) {
            for (int i = 0; i <= 3; i++) {
                String captchaLink = br.getRegex(OTHERCAPTCHA).getMatch(0);
                Form captchaForm = br.getForm(1);
                if (captchaForm == null || !captchaForm.containsHTML("captcha")) return null;
                captchaForm.setAction(parameter);
                String code = getCaptchaCode("http://ncrypt.in" + captchaLink, param);
                captchaForm.put("captcha", code);
                if (br.containsHTML(PASSWORDTEXT)) {
                    String passCode = getPassword(param);
                    captchaForm.put(PASSWORDPARAMETER, passCode);
                }
                br.submitForm(captchaForm);
                if (br.containsHTML(OTHERCAPTCHA) || br.containsHTML(PASSWORDTEXT)) continue;
                break;
            }
            if (br.containsHTML(PASSWORDFAILED)) throw new DecrypterException(DecrypterException.PASSWORD);
            if (br.getRegex(OTHERCAPTCHA).getMatch(0) != null) throw new DecrypterException(DecrypterException.CAPTCHA);
        } else if (br.containsHTML(PASSWORDTEXT)) {
            for (int i = 0; i <= 3; i++) {
                br.postPage(parameter, "password=" + getPassword(param) + "&submit_protected=Best%C3%A4tigen...+&submit_protected=Best%C3%A4tigen...+");
                if (br.containsHTML(PASSWORDFAILED)) continue;
                break;
            }
            if (br.containsHTML(PASSWORDFAILED)) throw new DecrypterException(DecrypterException.PASSWORD);
        }
        String fpName = br.getRegex("<h1>(.*?)<img").getMatch(0);
        // Container handling
        String[] containerIDs = br.getRegex("/container/(rsdf|dlc|ccf)/([a-z0-9]+)\\.").getColumn(1);
        if (containerIDs != null && containerIDs.length != 0) {
            for (String containerID : containerIDs) {
                ArrayList<DownloadLink> containerLinks = new ArrayList<DownloadLink>();
                if (br.containsHTML("\\.dlc")) {
                    containerLinks = loadcontainer("dlc/" + containerID + ".dlc");
                } else if (br.containsHTML("\\.rsdf")) {
                    containerLinks = loadcontainer("rsdf/" + containerID + ".rsdf");
                } else if (br.containsHTML("\\.ccf")) {
                    containerLinks = loadcontainer("ccf/" + containerID + ".ccf");
                }
                if (containerLinks != null) {
                    for (DownloadLink containerLink : containerLinks)
                        decryptedLinks.add(containerLink);
                }
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            // Webprotection decryption
            logger.info("ContainerID is null, trying webdecryption...");
            br.setFollowRedirects(false);
            String[] links = br.getRegex("\\'(http://ncrypt\\.in/link-.*?=)\\'").getColumn(0);
            if (links == null || links.length == 0) return null;
            progress.setRange(links.length);
            for (String singleLink : links) {
                singleLink = singleLink.replace("link-", "frame-");
                br.getPage(singleLink);
                String finallink = br.getRedirectLocation();
                if (finallink == null) return null;
                decryptedLinks.add(createDownloadlink(finallink));
                progress.increase(1);
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String getPassword(CryptedLink param) throws DecrypterException {
        String passCode = getUserInput(null, param);
        return passCode;
    }

    private ArrayList<DownloadLink> loadcontainer(String theLink) throws IOException, PluginException {
        ArrayList<DownloadLink> decryptedLinks = null;
        Browser brc = br.cloneBrowser();
        String theID = theLink;
        theLink = "http://ncrypt.in/container/" + theLink;
        File file = null;
        URLConnectionAdapter con = brc.openGetConnection(theLink);
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/ncryptin/" + theLink.replaceAll("(:|/)", "") + theID);
            if (file == null) return null;
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
            if (decryptedLinks.size() > 0) return decryptedLinks;
        } else {
            return null;
        }
        return null;
    }
}
