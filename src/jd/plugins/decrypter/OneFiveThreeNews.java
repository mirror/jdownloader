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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "153news.net" }, urls = { "https?://(?:www\\.)?153news\\.net/watch_video\\.php?.*" })
public class OneFiveThreeNews extends PluginForDecrypt {
    public OneFiveThreeNews(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final String fpName = br.getRegex("<title>\\s*([^<]+)\\s+-\\s+153News\\.net\\s+-\\s+Because\\s+Censorship\\s+Kills").getMatch(0);
        String[][] links = br.getRegex("<source[^>]+src\\s*=\\s*[\"']([^\"']+)[\"'][^>]+res\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>").getMatches();
        for (String[] link : links) {
            String decodedLink = link[0];
            String resolution = link[1];
            String extension = getFileNameExtensionFromURL(decodedLink, ".mp4");
            DownloadLink dl = createDownloadlink(Encoding.htmlDecode(decodedLink));
            if (StringUtils.isNotEmpty(fpName)) {
                String filename = Encoding.htmlDecode(fpName.trim()) + "_" + resolution + extension;
                dl.setFinalFileName(filename);
            }
            decryptedLinks.add(dl);
        }
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.setProperty("ALLOW_MERGE", true);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}