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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hitbdsm.com" }, urls = { "https?://(?:www\\.)?hitbdsm\\.com/([\\w\\-]+)/?" })
public class HitbdsmCom extends PluginForDecrypt {
    public HitbdsmCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String title = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0).replace("-", " ");
        String base64 = br.getRegex("q=([a-zA-Z0-9_/\\+\\=\\-%]+)").getMatch(0);
        base64 = Encoding.htmlDecode(base64);
        final String html = Encoding.htmlDecode(Encoding.Base64Decode(base64));
        final String[][] qualities = new Regex(html, "src=\"(https?://[^\"]+\\.mp4)\"[^>]*label=\"(\\d+p)\"").getMatches();
        for (final String[] quality : qualities) {
            final DownloadLink dl = this.createDownloadlink(quality[0]);
            dl.setFinalFileName(title + "_" + quality[1] + ".mp4");
            decryptedLinks.add(dl);
            /* Only add first quality (first = best) */
            break;
        }
        return decryptedLinks;
    }
}
