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
import java.util.ArrayList;
import java.util.logging.Level;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "stealth.to" }, urls = { "http://[\\w\\.]*?stealth\\.to/(\\?id\\=[\\w]+|index\\.php\\?id\\=[\\w]+|\\?go\\=captcha&id=[\\w]+)|http://[\\w\\.]*?stealth\\.to/folder/[\\w]+" }, flags = { 0 })
public class Stlth extends PluginForDecrypt {

    public Stlth(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception, DecrypterException {

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>(0);
        String url = param.toString();
        String stealthID;
        int idx = url.indexOf("=");
        if (idx > 0) {
            stealthID = url.substring(idx + 1, url.length());
        } else {
            stealthID = url.substring(url.lastIndexOf("/") + 1);
        }
        this.setBrowserExclusive();
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.getPage(url);
        Browser clone = null;
        if (br.containsHTML("besucherpass.png")) {

            Form form = br.getFormBySubmitvalue("Weiter");
            form.put("access_pass", getUserInput(JDL.LF("jd.plugins.decrypter.stealth.askpassword", "Downloadpassword required for %s", param.getCryptedUrl()), param));
            br.submitForm(form);

        }
        if (br.containsHTML("Sicherheitsabfrage")) {
            logger.fine("The current page is captcha protected, getting captcha ID...");
            int max = 3;
            while (max-- >= 0) {
                String recaptchaID = br.getRegex("<script type=\"text/javascript\" src=\"http://api.recaptcha.net/challenge\\?(.*?)\">").getMatch(0);
                // trim() will throw Nullointer. which will be caught internally
                // in
                // end up in an Decrypter out of date error
                recaptchaID = Request.parseQuery(recaptchaID).get("k").trim();
                logger.fine("The current recaptcha ID is '" + recaptchaID + "'");

                logger.fine("The current stealth ID is '" + stealthID + "'");

                clone = br.cloneBrowser();

                Form form = br.getFormBySubmitvalue("Ordner+%C3%B6ffnen");

                Browser xs = clone.cloneBrowser();
                xs.getPage("http://api.recaptcha.net/challenge?k=" + recaptchaID);

                String challenge = xs.getRegex("challenge : '(.*?)',").getMatch(0);
                String server = xs.getRegex("server : '(.*?)',").getMatch(0);
                File captchaFile = this.getLocalCaptchaFile();
                Browser.download(captchaFile, xs.openGetConnection(server + "image?c=" + challenge));

                String code = getCaptchaCode(captchaFile, param);

                form.put("recaptcha_challenge_field", challenge);
                form.put("recaptcha_response_field", code);
                form.setMethod(MethodType.GET);

                clone.getHeaders().put("Referer", url.replaceFirst("\\?id=", "folder/"));
                clone.submitForm(form);
                if (clone.containsHTML("incorrect-captcha-sol")) {
                    if (max == 0) {
                        logger.warning("Captcha error");

                        return null;
                    } else {
                        continue;
                    }
                }

                break;
            }
        }

        File container = JDUtilities.getResourceFile("container/stealth.to_" + System.currentTimeMillis() + ".dlc");

        String name = clone.getRegex("<span class=\"Name\">(.*?)</span>").getMatch(0);
        String pass = clone.getRegex("<span class=\".*?\">Passwort: (.*?)</span>").getMatch(0);
        FilePackage fp = FilePackage.getInstance();

        fp.setName(name);
        fp.setPassword(pass);

        Browser.download(container, clone.openGetConnection("http://stealth.to:2999/dlc.php?name=" + stealthID));
        ArrayList<DownloadLink> links = JDUtilities.getController().getContainerLinks(container);

        if (links != null && links.size() > 0) {
            for (DownloadLink l : links)
                l.setFilePackage(fp);
            decryptedLinks.addAll(links);
        } else {
            logger.log(Level.WARNING, "Cannot decrypt download links file ['" + container.getName() + "']");
        }

        container.delete();

        int numberOfDecryptedLinks = decryptedLinks.size();
        if (numberOfDecryptedLinks == 0) {
            logger.warning("There were no links obtained for the URL '" + url + "'");
        }

        return decryptedLinks;
    }

}
