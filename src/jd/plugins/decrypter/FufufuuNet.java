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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fufufuu.net" }, urls = { "http://(www\\.)?fufufuu\\.net/m/\\d+/[a-z0-9\\-_]+/thumbs/" }, flags = { 0 })
public class FufufuuNet extends PluginForDecrypt {

    public FufufuuNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("/thumbs/", "/");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<title>Fufufuu \\| ([^<>\"]*?)</title>").getMatch(0);
        final String b64 = br.getRegex("class=\"none\">(.*?)<").getMatch(0);
        final String b64decoded = Encoding.Base64Decode(b64);
        final String[] links = new Regex(b64decoded, "\"url\": \"(/[^<>\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            final DownloadLink dl = createDownloadlink("directhttp://http://fufufuu.net" + singleLink);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        /* Check if we have a .zip downloadlink */
        try {
            br.setFollowRedirects(false);
            final String csrftoken = br.getCookie("http://fufufuu.net/", "csrftoken");
            if (csrftoken != null) {
                final String dl_link = parameter + "download/";
                br.postPage(dl_link, "csrfmiddlewaretoken=" + csrftoken);
                final String final_dl_link = br.getRedirectLocation();
                if (final_dl_link != null) {
                    final DownloadLink dl = createDownloadlink("directhttp://" + final_dl_link);
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                }
            }
        } catch (final Throwable e) {
            /* Avoid failing here */
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
