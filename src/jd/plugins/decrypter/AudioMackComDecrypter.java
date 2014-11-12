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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "audiomack.com" }, urls = { "http://(www\\.)?audiomack\\.com/album/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+" }, flags = { 0 })
public class AudioMackComDecrypter extends PluginForDecrypt {

    public AudioMackComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        /* Offline or not yet released */
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"countdown\\-clock\"|This song has been removed due to a DMCA Complaint")) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String fpName = br.getRegex("name=\"twitter:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (fpName == null) {
            final Regex paraminfo = new Regex(parameter, "audiomack\\.com/album/([A-Za-z0-9\\-_]+)/([A-Za-z0-9\\-_]+)");
            fpName = paraminfo.getMatch(0) + " - " + paraminfo.getMatch(1);
        }
        final String plaintable = br.getRegex("<div id=\"playlist\" class=\"plwrapper\" for=\"audiomack\\-embed\">(.*?</div>[\t\n\r ]+</div>[\t\n\r ]+</div>)[\t\n\r ]+</div>[\t\n\r ]+</div>").getMatch(0);
        final String[] links = plaintable.split("<div class=\"song\">");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String description = br.getRegex("<meta name=\"description\" content=\"(.*?)\" >").getMatch(0);
        for (final String singleinfo : links) {
            final Regex url_name = new Regex(singleinfo, "<a href=\"#\" data\\-url=\"(http://(www\\.)?audiomack\\.com/api/music/url/album/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+/\\d+)\">([^<>\"]*?)<");
            final String url = url_name.getMatch(0);
            String name = url_name.getMatch(2);
            final String titlenumber = new Regex(singleinfo, "<div class=\"index\">(\\d+\\.)</div>").getMatch(0);
            if (url != null && name != null && titlenumber != null) {
                name = Encoding.htmlDecode(name).trim();
                final DownloadLink fina = createDownloadlink(url);
                final String finalname = titlenumber + name + ".mp3";
                fina.setFinalFileName(finalname);
                fina.setAvailable(true);
                fina.setProperty("plain_filename", finalname);
                fina.setProperty("mainlink", parameter);
                if (description != null) {
                    try {
                        fina.setComment(Encoding.htmlDecode(description));
                    } catch (Throwable e) {
                    }
                }
                decryptedLinks.add(fina);
            }
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        final String ziplink = br.getRegex("\"(https?://music\\.audiomack\\.com/albums/[^<>\"]+\\.zip?[^<>\"]*?)\"").getMatch(0);
        if (ziplink != null) {
            final DownloadLink fina = createDownloadlink("directhttp://" + ziplink);
            fina.setFinalFileName(fpName + ".zip");
            fina.setAvailable(true);
            decryptedLinks.add(fina);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
