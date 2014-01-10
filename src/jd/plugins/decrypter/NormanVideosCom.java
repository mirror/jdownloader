//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
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
//
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "normanfaitdesvideos.com" }, urls = { "http://(www\\.)?normanfaitdesvideos\\.com/\\d{4}/\\d{2}/\\d{2}/[a-z0-9\\-]+/" }, flags = { 0 })
public class NormanVideosCom extends PluginForDecrypt {

    public NormanVideosCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String strParameter = param.toString();

        br.setFollowRedirects(true);
        br.getPage(strParameter);
        if (br.containsHTML(">Erreur 404 \\- Introuvable<") || br.containsHTML("HTTP/1\\.0 404 Not Found")) {
            logger.info("Link offline: " + strParameter);
            return decryptedLinks;
        }

        String fpName = br.getRegex("<title>([^<>\"]*?)\\- Norman fait des vidéos</title>").getMatch(0);
        if (fpName == null) fpName = new Regex(strParameter, "normanfaitdesvideos\\.com/(.+)").getMatch(0);
        fpName = Encoding.htmlDecode(fpName.trim());

        String[] links = br.getRegex("(http://(www\\.)?youtube.com/embed/[A-Za-z0-9\\-_]+)\\?").getColumn(0);
        if (links == null || links.length == 0) links = br.getRegex("file: \"(http://player\\.vimeo\\.com/external/[^<>\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) links = HTMLParser.getHttpLinks(br.toString(), "");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + strParameter);
            return null;
        }

        // Added links
        for (String redirectlink : links) {
            if (!redirectlink.matches("http://(www\\.)?normanfaitdesvideos\\.com/\\d{4}/\\d{2}/\\d{2}/[a-z0-9\\-]+/")) {
                if (redirectlink.contains("vimeo.com/")) redirectlink = "directhttp://" + redirectlink;
                final DownloadLink finallink = createDownloadlink(redirectlink);
                decryptedLinks.add(finallink);
            }
        }

        // Add all link in a package
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}