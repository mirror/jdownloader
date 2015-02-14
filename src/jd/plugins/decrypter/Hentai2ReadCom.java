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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hentai2read.com" }, urls = { "http://(www\\.)?hentai2read.com/[a-z0-9\\-_]+/\\d+" }, flags = { 0 })
public class Hentai2ReadCom extends PluginForDecrypt {

    public Hentai2ReadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter + "/1/");
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(getOffline(parameter));
            return decryptedLinks;
        } else if (br.containsHTML(">Sorry, this chapter is no longer available due to")) {
            decryptedLinks.add(getOffline(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("itemprop=\"itemreviewed\">([^<>]*?)</span>").getMatch(0);
        if (fpName == null) {
            fpName = new Regex(parameter, "hentai2read.com/([a-z0-9\\-_]+)/").getMatch(0);
        }
        String arraytext = br.getRegex("var wpm_mng_rdr_img_lst = \\[(.*?)\\]").getMatch(0);
        if (arraytext != null) {
            arraytext = arraytext.replace("\\", "");
            final String[] links = arraytext.split(",");
            for (final String pic : links) {
                final String finallink = "directhttp://" + pic.replace("\"", "");
                final DownloadLink dl = createDownloadlink(finallink);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        } else {
            final Regex linkStructure = br.getRegex("(http://hentai2read\\.com/wp\\-content/[a-z0-9\\-_]+/\\d+/\\d+/p)\\d{3}\\.jpg");
            final String lastpage = br.getRegex("value=\"(\\d+)\">\\d+</option></select></li><li>").getMatch(0);
            final String mainpart = linkStructure.getMatch(0);
            final DecimalFormat df = new DecimalFormat("000");
            for (int i = 1; i <= Integer.parseInt(lastpage); i++) {
                final String finallink = "directhttp://" + mainpart + df.format(i) + ".jpg";
                final DownloadLink dl = createDownloadlink(finallink);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    private DownloadLink getOffline(final String parameter) {
        final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
        offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
        offline.setAvailable(false);
        offline.setProperty("offline", true);
        return offline;
    }

}
