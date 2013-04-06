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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "books.google.com" }, urls = { "http://books\\.google\\.(de|pt)/books\\?id=[0-9a-zA-Z-_]+.*" }, flags = { 0 })
public class GglBks extends PluginForDecrypt {

    public GglBks(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String param = parameter.toString();
        String url = param;
        if (url.contains("&")) url = url.split("&")[0];
        String url2 = url.concat("&printsec=frontcover&jscmd=click3");
        br.setFollowRedirects(true);
        br.getPage(url2);
        if (!br.containsHTML("\"page\"")) {
            logger.info("Link offline or book not downloadable: " + parameter);
            return decryptedLinks;
        }
        // logger.info(br.toString());
        // page moved + capcha - secure for automatic downloads
        if (br.containsHTML("http://sorry.google.com/sorry/\\?continue=.*")) {
            url = br.getRedirectLocation() != null ? br.getRedirectLocation() : br.getRegex("<A HREF=\"(http://sorry.google.com/sorry/\\?continue=http://books.google.com/books.*?)\">").getMatch(0);
            if (url == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            logger.info("Reconnect needed to continue downloading, quitting decrypter...");
            return decryptedLinks;
            // TODO: can make redirect and capcha but this only for secure to
            // continue connect not for download page
        }

        // if (br.containsHTML("http://sorry.google.com/sorry/\\?continue=.*"))
        // { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 *
        // 1000); }
        String[] links = br.getRegex("\\{\\\"pid\\\":\\\"(.*?)\\\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String dl : links) {
            DownloadLink link = createDownloadlink(url.replace("books.google", "googlebooksdecrypter") + "&pg=" + dl);
            int counter = 120000;
            String filenumber = new Regex(dl, ".*?(\\d+)").getMatch(0);
            if (filenumber != null && !dl.contains("-")) {
                counter = counter + Integer.parseInt(filenumber);
                String regexedCounter = new Regex(Integer.toString(counter), "12(\\d+)").getMatch(0);
                link.setName(dl.replace(filenumber, "") + regexedCounter);
            } else {
                link.setName(dl + ".jpg");
            }
            decryptedLinks.add(link);
        }
        br.getPage(url);
        final String fpName = br.getRegex("name=\"title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
