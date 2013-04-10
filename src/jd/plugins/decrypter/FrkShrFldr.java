//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freakshare.com" }, urls = { "http://(www\\.)?freakshare\\.(com|net)/(folder/\\d+/[^<>\"]+\\.html|\\?x=folder\\&f_id=\\d+\\&f_md5=[a-z0-9]+)" }, flags = { 0 })
public class FrkShrFldr extends PluginForDecrypt {

    public FrkShrFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        final Regex fInfo = new Regex(parameter, "/folder/(\\d+)/([^<>\"]+)\\.html");
        final Regex fInfo2 = new Regex(parameter, "/\\?x=folder\\&f_id=(\\d+)\\&f_md5=([a-z0-9]+)\\&");
        String fid = fInfo.getMatch(0);
        String fmd5 = fInfo.getMatch(1);
        if (fid == null) fid = fInfo2.getMatch(0);
        if (fmd5 == null) fmd5 = fInfo2.getMatch(1);
        parameter = "http://freakshare.com/?x=folder&f_id=" + fid + "&f_md5=" + fmd5 + "&entrys=10000&page=0&order=";
        br.setCookiesExclusive(true);
        br.setReadTimeout(3 * 60 * 1000);
        br.getPage(parameter);
        // they return no data on invalid url
        if (br.containsHTML("No htmlCode read") || br.containsHTML("<b>Choose a File:</b>")) {
            logger.warning("Maybe invalid URL: " + parameter);
            return decryptedLinks;
        }

        String fpName = br.getRegex("<b>Folder:</b> ([^\r\n\t]+)").getMatch(0);
        String totalPages = br.getRegex("Files: \\d+ \\- Pages:  (\\d+)").getMatch(0);
        String pageURL = br.getRegex("<a href=\"([^\"]+)\"><b>1</b></a>").getMatch(0);

        // process first page
        parsePage(decryptedLinks);
        // process additional pages
        if (totalPages != null && !totalPages.equals("1") && pageURL != null) {
            int numberOfPages = Integer.parseInt(totalPages);
            progress.setRange(numberOfPages);
            for (int i = 1; i < numberOfPages; i++) {
                br.getPage(pageURL.replaceAll("\\&page=\\d+", "&page=" + i));
                parsePage(decryptedLinks);
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

    private void parsePage(ArrayList<DownloadLink> ret) {
        String[] links = br.getRegex("span class=\"f_name\"><a href=\"([^\"]+)").getColumn(0);
        if (links == null || links.length == 0) return;
        if (links != null && links.length != 0) {
            for (String dl : links)
                ret.add(createDownloadlink(dl));
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}