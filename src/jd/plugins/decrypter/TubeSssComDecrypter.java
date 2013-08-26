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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tubesss.com" }, urls = { "http://(www\\.)?tubesss\\.com/videos/\\d+/.*?\\.html" }, flags = { 0 })
public class TubeSssComDecrypter extends PluginForDecrypt {

    public TubeSssComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        final String finallink = br.getRedirectLocation();
        if (finallink != null && !finallink.contains("tubesss.com/")) {
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            final DownloadLink dl = createDownloadlink(parameter.replace("tubesss.com/", "tubesssdecrypted.com/"));
            if (!br.getURL().contains("tubesss.com") || br.containsHTML("<title> at Tubesss\\.com  \\- Free Videos Adult Sex Tube</title>") || br.getURL().equals("http://www.tubesss.com/404.php")) dl.setAvailable(false);
            String filename = br.getRegex("<title>([^<>\"]*?) at TubeSSS</title>").getMatch(0);
            if (filename != null) dl.setName(Encoding.htmlDecode(filename.trim()));
            decryptedLinks.add(dl);
        }

        return decryptedLinks;
    }

}
