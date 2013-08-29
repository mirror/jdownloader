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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "clips-and-pics.org" }, urls = { "http://(www\\.)?clips\\-and\\-pics\\.org/(hosted/media/.*?,\\d+\\.php|out(\\-sticky)?\\-id\\d+[a-z0-9\\-]+\\.html|hosted\\-id\\d+[a-z0-9\\-]+\\.html)" }, flags = { 0 })
public class ClipsAndPicsOrgDecrypter extends PluginForDecrypt {

    public ClipsAndPicsOrgDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_OUT    = "http://(www\\.)?clips\\-and\\-pics\\.org/out(\\-sticky)?\\-id\\d+[a-z0-9\\-]+\\.html";
    private static final String TYPE_HOSTED = "http://(www\\.)?clips\\-and\\-pics\\.org/hosted(/media/.*?,\\d+\\.php|\\-id\\d+[a-z0-9\\-]+\\.html)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        // http://www.clips-and-pics.org/out-sticky-id13277-caught-his-sister-totally-nude.html
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (parameter.matches(TYPE_OUT)) {
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            final String[] links = br.getRegex("\"(http://(www\\.)fvfileserver\\.com/cnp/.*?)\"").getColumn(0);
            if (links != null && links.length != 0) {
                String fpName = br.getRegex("<div id=\"mid\">[\t\n\r ]+<div class=\"navi_m_top\">(.*?)</div>").getMatch(0);
                if (fpName == null) fpName = br.getRegex("<meta name=\"description\" content=\"(.*?)\">").getMatch(0);
                for (String dl : links)
                    decryptedLinks.add(createDownloadlink("directhttp://" + dl));
                if (fpName != null) {
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName(fpName.trim());
                    fp.addLinks(decryptedLinks);
                }
            } else {
                final DownloadLink dl = createDownloadlink("http://clips-and-pics.orgdecrypted/" + System.currentTimeMillis() + new Random().nextInt(1000000));
                String filename = new Regex(parameter, "hosted\\-id\\d+\\-(.*?)\\.html").getMatch(0);
                if (filename == null) filename = br.getRegex("<div id=\"mid\">[\t\n\r ]+<div class=\"navi_m_top\">(.*?)</div>").getMatch(0);
                if (filename == null) filename = br.getRegex("<meta name=\"description\" content=\"(.*?)\">").getMatch(0);
                if (filename != null) {
                    dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
                    dl.setAvailable(true);
                }
                dl.setProperty("mainlink", parameter);
                decryptedLinks.add(dl);
            }
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}