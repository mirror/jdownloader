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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "kamababa.com" }, urls = { "https?://(?:www\\.)?kamababa2?\\.com/([a-z0-9\\-]+)/?" })
public class KamababaComCrawler extends PornEmbedParser {
    public KamababaComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** E.g. more domains: xvirgo.com */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String filename = jd.plugins.hoster.KamababaCom.getFiletitle(br);
        if (filename == null) {
            filename = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        }
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (decryptedLinks.size() == 0) {
            /* Probably selfhosted content */
            final DownloadLink dl = this.createDownloadlink(parameter);
            if (jd.plugins.hoster.KamababaCom.isOffline(this.br)) {
                dl.setAvailable(false);
            } else {
                dl.setAvailable(true);
            }
            dl.setName(filename + ".mp4");
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}