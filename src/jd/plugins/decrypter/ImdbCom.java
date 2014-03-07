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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imdb.com" }, urls = { "http://(www\\.)?imdb\\.com/name/nm\\d+/mediaindex" }, flags = { 0 })
public class ImdbCom extends PluginForDecrypt {

    public ImdbCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        int maxpage = 1;
        final String[] pages = br.getRegex("\\?page=(\\d+)\\&ref_=").getColumn(0);
        if (pages != null && pages.length != 0) {
            for (final String page : pages) {
                final int curpage = Integer.parseInt(page);
                if (curpage > maxpage) maxpage = curpage;
            }
        }
        String fpName = br.getRegex("itemprop=\\'url\\'>([^<>\"]*?)</a>").getMatch(0);
        if (fpName == null) fpName = "imdb.com - " + new Regex(parameter, "(nm\\d+)").getMatch(0);
        fpName = Encoding.htmlDecode(fpName.trim());
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        for (int i = 1; i <= maxpage; i++) {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return decryptedLinks;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            if (i > 1) br.getPage(parameter + "?page=" + i);
            final String[][] links = br.getRegex("\"(/media/rm\\d+/nm\\d+)\\?ref_=[^<>\"/]+\" title=\"([^<>\"]*?)\"").getMatches();
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String linkinfo[] : links) {
                final String link = "http://imdb.com" + linkinfo[0];
                final DownloadLink dl = createDownloadlink(link);
                final String id = new Regex(link, "rm(\\d+)").getMatch(0);
                dl._setFilePackage(fp);
                dl.setName(fpName + "_" + id + "_" + Encoding.htmlDecode(linkinfo[1].trim()) + ".jpg");
                dl.setAvailable(true);
                try {
                    distribute(dl);
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                decryptedLinks.add(dl);
            }
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
