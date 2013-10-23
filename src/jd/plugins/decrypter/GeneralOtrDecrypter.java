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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "die-schnelle-kuh.de", "otr-share.de", "tivootix.co.cc", "otr-drive.com", "otr.seite.com" }, urls = { "http://(www\\.)?die\\-schnelle\\-kuh\\.de/\\?file=[^<>\"\\']+", "http://(www\\.)?otr\\-share\\.de/\\?s=download\\&key=[^<>\"\\']+", "http://(www\\.)?tivootix\\.co\\.cc/\\?file=[^<>\"\\']+", "http://(www\\.)?otr\\-drive\\.com/(index\\.php)?\\?file=[^<>\"\\']+", "http://(www\\.)?otr\\.seite\\.com/get\\.php\\?file=[^<>\"\\']+" }, flags = { 0, 0, 0, 0, 0 })
public class GeneralOtrDecrypter extends PluginForDecrypt {

    public GeneralOtrDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        if (parameter.contains("die-schnelle-kuh.de/")) {
            br.getPage(parameter);
            if (br.containsHTML("Leider bieten wir diese Datei nicht als Download an")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            final String contnue = br.getRegex("onclick=\"window\\.location\\.href=\\'(\\?[^<>\"\\']+)\\'\"").getMatch(0);
            if (contnue == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage("http://die-schnelle-kuh.de/" + contnue);
            String finallink = br.getRegex("type=\"text\" style=\"width:100px;\" value=\"(http://die\\-schnelle\\-kuh\\.de/[^<>\"\\']+)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("\"(http://die\\-schnelle\\-kuh\\.de/index\\.php\\?kick\\&fileid=\\d+\\&ticket=\\d+\\&hash=[a-z0-9]+\\&filename=[^<>\"\\']+)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage(finallink);
            finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else if (parameter.contains("otr-share.de/")) {
            br.getPage(parameter);
            final String linkTable = br.getRegex("<table align=\"center\" class=\"stable\" width=\"360\">(.*?)</table>").getMatch(0);
            if (linkTable == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            String[] ochLinks = HTMLParser.getHttpLinks(linkTable, "");
            if (ochLinks == null || ochLinks.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String ochLink : ochLinks) {
                if (!ochLink.contains("otr-share.de/")) decryptedLinks.add(createDownloadlink(ochLink));
            }
            String ftpLink = br.getRegex("<td><center><strong><a href=\"(ftp://[^<<\"\\']+)\"").getMatch(0);
            if (ftpLink == null) ftpLink = br.getRegex("\"(ftp://[A-Za-z0-9]+:[A-Za-z0-9]+@dl\\d+\\.otr\\-share\\.de/dl\\d+/[^<>\"\\']+)\"").getMatch(0);
            if (ftpLink != null) decryptedLinks.add(createDownloadlink(ftpLink));
            FilePackage fp = FilePackage.getInstance();
            fp.setName(new Regex(parameter, "otr\\-share\\.de/\\?s=download\\&key=(.+)").getMatch(0));
            fp.addLinks(decryptedLinks);
        } else if (parameter.contains("tivootix.co.cc/")) {
            br.getPage(parameter);
            final String tmplink = br.getRegex("\"(go2\\.php\\?id=\\d+)\"").getMatch(0);
            if (tmplink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage("http://www.tivootix.co.cc/" + tmplink);
            String[] ochLinks = br.getRegex("title=\"Datei von [^<>\"\\']+ herunterladen\" src=\"[^<>\"\\']+\" alt=\"\" /> [^<>\"\\']+:</b></td><td><a href=\"(http://[^<>\"\\']+)\"").getColumn(0);
            if (ochLinks == null || ochLinks.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String ochLink : ochLinks) {
                if (!ochLink.contains("tivootix.co.cc/")) decryptedLinks.add(createDownloadlink(ochLink));
            }
            FilePackage fp = FilePackage.getInstance();
            fp.setName(new Regex(parameter, "tivootix\\.co\\.cc/\\?file=(.+)").getMatch(0));
            fp.addLinks(decryptedLinks);
        } else if (parameter.contains("otr-drive.com/")) {
            br.getPage(parameter);
            String continu = br.getRegex("<big><big>>>> <a href=\"(http://[^<>\"\\']+)\"").getMatch(0);
            if (continu == null) continu = br.getRegex("\"(http://(www\\.)?otr\\-drive\\.com/index\\.php\\?file=[^<>\"\\']+)\"").getMatch(0);
            if (continu == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage(Encoding.htmlDecode(continu));
            final String finallink = br.getRegex("name=\"download_iframe\"><p>(http://[^<>\"\\']+)</p></iframe>").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else if (parameter.contains("otr.seite.com/")) {
            br.setReadTimeout(3 * 60 * 1000);
            br.getPage(parameter);
            final String finallink = br.getRegex("name=\"Downloadpage\" src=\"([^<>\"\\']+)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}