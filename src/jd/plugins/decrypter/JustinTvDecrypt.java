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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "justin.tv" }, urls = { "http://(www\\.)?justin\\.tv/[a-z0-9\\-_]+/(b/\\d+|videos(\\?page=\\d+)?)" }, flags = { 0 })
public class JustinTvDecrypt extends PluginForDecrypt {

    public JustinTvDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String HOSTURL = "http://www.justindecrypted.tv";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (parameter.contains("/videos")) {
            String[] links = br.getRegex("<p class=\"title\"><a href=\"(/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) links = br.getRegex("<div class=\"left\">[\t\n\r ]+<a href=\"(/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) return null;
            for (String dl : links)
                decryptedLinks.add(createDownloadlink(HOSTURL + dl));
        } else {
            String filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?)\"/>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<h2 class=\"clip_title\">(.*?)</h2>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<meta name=\"title\" content=\"(.*?)\" />").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("<title>Justin\\.tv \\- [A-Za-z0-9_]+ \\- (.*?)</title>").getMatch(0);
                    }
                }
            }
            DownloadLink singleLink = createDownloadlink(parameter.replace("justin", "justindecrypted"));
            String[] links = br.getRegex("\\|[\t\n\r ]+<a href=\"(/.*?)\"").getColumn(0);
            int counter = 2;
            if (links != null && links.length != 0) {
                if (filename == null) return null;
                filename = Encoding.htmlDecode(filename.trim());
                singleLink.setFinalFileName(filename + " - Part 1.flv");
                for (String dl : links) {
                    DownloadLink dlink = createDownloadlink(HOSTURL + dl);
                    dlink.setFinalFileName(filename + " - Part " + counter + ".flv");
                    decryptedLinks.add(dlink);
                    counter++;
                }
            }
            decryptedLinks.add(singleLink);
        }
        return decryptedLinks;
    }
}
