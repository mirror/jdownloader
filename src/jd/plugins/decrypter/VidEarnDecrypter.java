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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornxs.com" }, urls = { "http://(www\\.)?(pornxs\\.com/(video\\.php\\?id=|[a-z0-9\\-]+/$|playlists/\\d+[-\\w]+/)|(?:embed\\.pornxs\\.com/embed\\.php\\?id=)\\d+([a-z0-9\\-]+\\.html)?)" })
public class VidEarnDecrypter extends antiDDoSForDecrypt {

    public VidEarnDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    // This plugin takes videarn links and checks if there is also a filearn.com link available (partnersite)
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString();
        final String vid = new Regex(parameter, "embed\\.php\\?id=(\\d+)$").getMatch(0);
        if (vid != null) {
            parameter = "http://pornxs.com/teen-amateur-mature-cumshot-webcams/" + vid + "-0123456789.html";
        }
        final DownloadLink mainlink = createDownloadlink(parameter.replaceAll("pornxs\\.com/", "pornxsdecrypted.com/"));
        try {
            getPage(parameter);
        } catch (final Exception e) {
            mainlink.setAvailable(false);
            decryptedLinks.add(mainlink);
            return decryptedLinks;
        }
        String fpName = null;
        if (br.getHttpConnection().getResponseCode() == 404 || (false && !br.containsHTML("id=\"video\\-player\""))) {
            mainlink.setAvailable(false);
            mainlink.setProperty("offline", true);
        } else {
            fpName = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (fpName == null) {
                return null;
            }
            fpName = Encoding.htmlDecode(fpName);
            fpName = fpName.trim();
            String additionalDownloadlink = br.getRegex("\"(http://(www\\.)?filearn\\.com/files/get/.*?)\"").getMatch(0);
            if (additionalDownloadlink == null) {
                additionalDownloadlink = br.getRegex("<div class=\"video\\-actions\">[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
            }
            if (additionalDownloadlink != null) {
                final DownloadLink xdl = createDownloadlink(additionalDownloadlink);
                xdl.setProperty("videarnname", fpName);
                decryptedLinks.add(xdl);
            }

            mainlink.setName(fpName + ".mp4");
            mainlink.setAvailable(true);
        }

        decryptedLinks.add(mainlink);
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}