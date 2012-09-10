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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ge.tt" }, urls = { "http://(www\\.)?ge\\.tt/(?!developers|press|tools|notifications|blog|about|javascript|button|contact|terms)#?[A-Za-z0-9]+(/v/0)?" }, flags = { 0 })
public class GeTtDecrypter extends PluginForDecrypt {

    public GeTtDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String IDREGEX = "ge\\.tt/(.+)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString().replace("#", "");
        if (new Regex(parameter, "http://(www\\.)?ge\\.tt/[A-Za-z0-9]+/v/0").matches()) {
            br.getPage(parameter);
            String pictureLink = br.getRegex("<img class=\\'image\\-view loadable\\' src=\\'(http://[^<>\"]*?)\\'>").getMatch(0);
            if (pictureLink == null) pictureLink = br.getRegex("\\'(http://w\\d+\\.open\\.ge\\.tt/\\d+/files/[^<>\"]*?)\\'").getMatch(0);
            if (pictureLink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink("directhttp://" + pictureLink));

        } else {
            String fid = new Regex(parameter, IDREGEX).getMatch(0);
            br.getPage("http://api.ge.tt/0/" + fid + "?jsonp=_tmp_jsonp.cb" + System.currentTimeMillis());
            if (br.containsHTML("\"error\":\"share not found\"") || br.containsHTML(">404 Not Found<")) {
                final DownloadLink dlink = createDownloadlink("http://api.ge.tt/0/" + fid + "/0/" + fid);
                dlink.setAvailable(false);
                decryptedLinks.add(dlink);
                return decryptedLinks;
            }
            String[] links = br.getRegex("downloadurl\":\"(http://api\\d+?\\.ge\\.tt/\\d/[A-Za-z0-9]+/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String dl : links) {
                if (!dl.contains("/download")) {
                    /* important to get a download url */
                    dl = dl + "/download";
                }
                decryptedLinks.add(createDownloadlink(dl));
            }
        }

        return decryptedLinks;
    }

}
