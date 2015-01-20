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

import java.text.DecimalFormat;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "manga9.com" }, urls = { "http://(www\\.)?manga9\\.com/[a-z0-9\\-_]+/\\d+/" }, flags = { 0 })
public class Manga9Com extends PluginForDecrypt {

    public Manga9Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^<>\"/]+Chapter \\d+) Page \\d+ \\- Read ").getMatch(0);
        if (fpName == null) {
            fpName = new Regex(parameter, "manga9\\.com/([a-z0-9\\-_]+)/").getMatch(0);
        }
        final String maxpage = br.getRegex(">(\\d+)</option></select>").getMatch(0);
        // http://z.mhcdn.net/store/manga/15494/033.0/compressed/c001.jpg?v=11421676781
        final Regex finallinkdata = br.getRegex("(http://z\\.mhcdn\\.net/store/manga/\\d+/\\d{3}\\.0/compressed/[a-z]+|http://(?:www\\.)?manga9\\.com/wp\\-content/manga/\\d+/\\d+/)");
        final String serverpart = finallinkdata.getMatch(0);
        final String v = br.getRegex("\\?v=(\\d+)").getMatch(0);
        if (serverpart == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final DecimalFormat df = new DecimalFormat("000");
        final DecimalFormat df2 = new DecimalFormat("00");
        for (int i = 1; i <= Integer.parseInt(maxpage); i++) {
            String formattednumber;
            String directlink = "directhttp://" + serverpart;
            if (serverpart.contains("/compressed/")) {
                formattednumber = df.format(i);
                directlink += formattednumber + ".jpg?v=" + v;
            } else {
                formattednumber = df2.format(i);
                directlink += formattednumber + ".jpg";
            }
            final DownloadLink dl = createDownloadlink(directlink);
            dl.setFinalFileName(fpName + "_" + formattednumber + ".jpg");
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
