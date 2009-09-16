//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "link-host.de" }, urls = { "http://[\\w\\.]*?(link-host\\.de/get\\.php\\?id=[0-9]+|shorturl\\.link-host\\.de/\\?id=[A-Z0-9]+)" }, flags = { 0 })
public class LnkHstD extends PluginForDecrypt {

    public LnkHstD(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);

        if (parameter.contains("shorturl")) {
            String finallink = br.getRedirectLocation();
            if (finallink == null && br.containsHTML("Invalid URL")) {
                logger.warning("Wrong link");
                logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
                return new ArrayList<DownloadLink>();
            }
            DownloadLink dl = createDownloadlink(finallink);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }

        /* Error handling */
        if (br.containsHTML("Ordner gelöscht oder nicht vorhanden")) {
            logger.warning("Wrong link");
            logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return new ArrayList<DownloadLink>();
        }
        /* File package handling */
        Form captchaForm = br.getForm(0);
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String passCode = null;
        String botprotect1 = br.getRegex("Gib hier \"(.*?)\" ein:").getMatch(0);
        String botprotect2 = br.getRegex("/>Rechne aus: (.*?) =").getMatch(0);
        if (((botprotect1 == null) && br.containsHTML("Rechne aus:")) || ((botprotect2 == null) && br.containsHTML("Gib hier"))) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        if (botprotect1 != null) {
            captchaForm.put("Big", botprotect1);
        }
        if (botprotect2 != null) {
            String summand1 = new Regex(botprotect2, "(\\d+) [+-]{1} \\d+").getMatch(0);
            String op = new Regex(botprotect2, "\\d+ ([+-]{1}) \\d+").getMatch(0);
            String summand2 = new Regex(botprotect2, "\\d+ [+-]{1} (\\d+)").getMatch(0);
            if ((summand1 == null) || (summand2 == null) || (op == null)) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            int summe = 0;
            if (op.equals("+")) {
                summe = Integer.parseInt(summand1) + Integer.parseInt(summand2);
            } else if (op.equals("-")) {
                summe = Integer.parseInt(summand1) - Integer.parseInt(summand2);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            }
            captchaForm.put("rechencode", ""+summe);
        }
        if (br.containsHTML("Gib hier das Passwort ein")) {
            passCode = Plugin.getUserInput("Password?", param);

            captchaForm.put("pw", passCode);
        }
        br.submitForm(captchaForm);
        if (br.containsHTML("Falsches Passwort <a")) {
            logger.warning("Wrong password!");
            param.setProperty("pass", null);
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
        if (br.containsHTML("Verrechnet oder Vertippt")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);

        // Container handling
        if (br.containsHTML("typ=dlc")) {
            decryptedLinks = loadcontainer(br, "dlc");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        if (br.containsHTML("typ=rsdf")) {
            decryptedLinks = loadcontainer(br, "rsdf");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        if (br.containsHTML("typ=ccf")) {
            decryptedLinks = loadcontainer(br, "ccf");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        // Webdecryption
        String[] links = br.getRegex("action=\"(out\\.php.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String link : links) {
            link = "http://link-host.de/" + link;
            br.getPage(link);
            String finallink = br.getRegex("src=\"(&.*?;)\"").getMatch(0);
            finallink = Encoding.htmlDecode(finallink);
            if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            DownloadLink dl = createDownloadlink(finallink);
            decryptedLinks.add(dl);
            progress.increase(1);
        }
        return decryptedLinks;
    }

    // by jiaz
    private ArrayList<DownloadLink> loadcontainer(Browser br, String format) throws IOException, PluginException {
        Browser brc = br.cloneBrowser();
        String[] dlclinks = br.getRegex("<td><a href=\"(container.*?typ=" + format + ".*?)\"").getColumn(0);
        if (dlclinks == null || dlclinks.length == 0) return null;
        for (String link : dlclinks) {
            link = "http://link-host.de/" + link;
            String test = Encoding.htmlDecode(link);
            File file = null;
            URLConnectionAdapter con = brc.openGetConnection(link);
            if (con.getResponseCode() == 200) {
                file = JDUtilities.getResourceFile("tmp/linkhost/" + test.replaceAll("(http://link-host.de/|\\?)", "") + "." + format);
                if (file == null) return null;
                file.deleteOnExit();
                brc.downloadConnection(file, con);
            } else {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            }

            if (file != null && file.exists() && file.length() > 100) {
                ArrayList<DownloadLink> decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                if (decryptedLinks.size() > 0) return decryptedLinks;
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            }
        }
        return null;
    }

    // @Override

}
