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
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pornsexwank.com" }, urls = { "https?://(?:www\\.)?pornsexwank\\.com/[A-Za-z0-9\\-_]+\\-\\d+\\.html" })
public class PornsexwankCom extends PornEmbedParser {

    public PornsexwankCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        this.br.setCookiesExclusive(true);
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String filename = getTitle(this.br);
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (decryptedLinks.size() == 0) {
            /* No external URL found? Video must be hosted on their own servers! */
            decryptedLinks.add(this.createDownloadlink(parameter.replaceAll("https?://", "pornsexwankdecrypted://")));
        }
        return decryptedLinks;
    }

    public static String getTitle(final Browser br) {
        String filename = br.getRegex("<title>([^<>\"]+) \\- PornSexWank</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]+)</title>").getMatch(0);
        }
        return filename;
    }

}
