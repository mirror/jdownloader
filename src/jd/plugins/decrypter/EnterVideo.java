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

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision: 41212 $", interfaceVersion = 2, names = { "entervideo.net" }, urls = { "https?://(www\\.)?entervideo\\.net/watch/.*" })
public class EnterVideo extends antiDDoSForDecrypt {
    public EnterVideo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String videoBlock = br.getRegex("<video id=\"player\"[^>]+>(.*)</video>").getMatch(0);
        String[][] links = new Regex(videoBlock, "src=\"([^\"]+)\"").getMatches();
        for (String[] link : links) {
            DownloadLink dl = createDownloadlink(Encoding.htmlDecode(link[0]));
            dl.setProperty("Referer", parameter);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }
}