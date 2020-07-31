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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "swrfernsehen.de" }, urls = { "https?://(?:www\\.)?swrfernsehen\\.de/[^<>\"]+\\.html" })
public class ArdmediathekEmbed extends PluginForDecrypt {
    public ArdmediathekEmbed(PluginWrapper wrapper) {
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
        /*
         * Very simple wrapper that finds embedded content --> Adds it via "new style" of URLs --> Goes into Ardmediathek main crawler and
         * content gets crawler
         */
        final String[] links = br.getRegex("data-cridid=\"(crid://[^\"]+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.info("Failed to find any downloadable content");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        for (final String ardAppURL : links) {
            String appURLEncoded = Encoding.Base64Encode(ardAppURL);
            /* WTF */
            appURLEncoded = appURLEncoded.replace("=", "");
            decryptedLinks.add(createDownloadlink("https://www.ardmediathek.de/ard/player/" + appURLEncoded));
        }
        return decryptedLinks;
    }
}
