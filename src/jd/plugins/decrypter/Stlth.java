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
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "stealth.to" }, urls = { "http://[\\w\\.]*?stealth\\.to/(\\?id\\=[\\w]+|index\\.php\\?id\\=[\\w]+|\\?go\\=captcha&id=[\\w]+)|http://[\\w\\.]*?stealth\\.to/folder/[\\w]+" }, flags = { 0 })
public class Stlth extends PluginForDecrypt {

    public Stlth(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception, DecrypterException {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>(0);
        final String parameter = param.toString();
        String stealthID;
        final int idx = parameter.indexOf("=");
        if (idx > 0) {
            stealthID = parameter.substring(idx + 1, parameter.length());
        } else {
            stealthID = parameter.substring(parameter.lastIndexOf("/") + 1);
        }
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("besucherpass\\.png")) {
            for (int i = 0; i <= 3; i++) {
                final Form form = br.getFormBySubmitvalue("Weiter");
                form.put("access_pass", Plugin.getUserInput(null, param));
                br.submitForm(form);
                if (br.containsHTML("besucherpass\\.png")) continue;
                break;
            }
            if (br.containsHTML("besucherpass\\.png")) throw new DecrypterException(DecrypterException.PASSWORD);
        }
        // Captcha
        if (br.containsHTML("Sicherheitsabfrage")) {
            logger.fine("The current page is captcha protected, getting captcha ID...");
            final int max = 3;
            for (int i = 0; i <= max; i++) {
                final String recaptchaID = br.getRegex("k=([a-zA-Z0-9]+)\"").getMatch(0);
                final Form captchaForm = br.getForm(0);
                if (recaptchaID == null || captchaForm == null) { return null; }
                logger.fine("The current recaptcha ID is '" + recaptchaID + "'");
                logger.fine("The current stealth ID is '" + stealthID + "'");
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((jd.plugins.hoster.DirectHTTP) recplug).getReCaptcha(br);
                rc.setId(recaptchaID);
                rc.setForm(captchaForm);
                rc.load();
                final File cf = rc.downloadCaptcha(this.getLocalCaptchaFile());
                final String c = this.getCaptchaCode(cf, param);
                rc.setCode(c);
                if (br.containsHTML("api\\.recaptcha\\.net")) {
                    continue;
                }
                break;
            }
            if (br.containsHTML("api\\.recaptcha\\.net")) { throw new DecrypterException(DecrypterException.CAPTCHA); }
        }
        final String name = br.getRegex("class=\"Foldername\">(.*?)<").getMatch(0);
        final String pass = br.getRegex(">Passwort: (.*?)</span>").getMatch(0);
        // Container handling
        final String containerDownloadLink = br.getRegex("\"(http://[a-z]+\\.stealth\\.to/dlc\\.php\\?name=[a-z0-9]+)\"").getMatch(0);
        if (containerDownloadLink == null) {
            logger.warning("containerDownloadLink equals null");
            return null;
        }
        Browser brc = br.cloneBrowser();
        File file = null;
        URLConnectionAdapter con = null;
        try {
            con = brc.openGetConnection(containerDownloadLink);
            if (con.getResponseCode() == 200) {
                file = JDUtilities.getResourceFile("tmp/stealthto/" + containerDownloadLink.replaceAll("(:|/|\\?|=)", "") + ".dlc");
                if (file != null) {
                    file.deleteOnExit();
                    brc.downloadConnection(file, con);
                    if (file != null && file.exists() && file.length() > 100) {
                        decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                    }
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }

        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            // File handling
            final int filePart = br.getRegex("form action").count();
            brc = br.cloneBrowser();
            if (filePart > 0) {
                progress.setRange(filePart);
                for (int i = 0; i < filePart; i++) {
                    final Form cryptedLink = br.getForm(i);
                    cryptedLink.remove("mirror");
                    brc.submitForm(cryptedLink);
                    if (brc.containsHTML("form action")) {
                        brc.submitForm(brc.getForm(0));
                    }
                    final String decLink = brc.getRegex("<td><iframe src=\"(.*?)\"").getMatch(0);
                    if (decLink != null) {
                        final DownloadLink dl = createDownloadlink(decLink);
                        decryptedLinks.add(dl);
                        progress.increase(1);
                    } else {
                        logger.warning("Filepart " + i + " Regex broken!");
                    }
                }
            } else {
                logger.warning("Decrypter out of date for link: " + parameter);
                return null;
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        if (name != null || pass != null) {
            final FilePackage fp = FilePackage.getInstance();
            if (name != null) {
                fp.setName(name.trim());
            }
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}