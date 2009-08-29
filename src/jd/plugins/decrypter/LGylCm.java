//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "lougyl.com" }, urls = { "http://[\\w\\.]*lougyl\\.com/files/[A-Z0-9]+/.+" }, flags = { 0 })
public class LGylCm extends PluginForDecrypt {

    public LGylCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);

        String dlsite0 = new Regex(param, "lougyl\\.com/files/(.*?)/").getMatch(0);
        String dlsite1 = "http://www.lougyl.com/status.php?uid=" + dlsite0;
        br.getPage(dlsite1);
        String[] links = br.getRegex(" href=(.*?) target").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String link : links) {
            String link1 = "http://www.lougyl.com" + link;
            br.getPage(link1);
            String finallink = br.getRegex("refresh\" content=\"[0-9]+;url=(.*?)\">").getMatch(0);
            if (finallink == null) return decryptedLinks;
            DownloadLink dl = createDownloadlink(finallink);
            decryptedLinks.add(dl);
            progress.increase(1);
        }
        return decryptedLinks;
    }


    // @Override

}
