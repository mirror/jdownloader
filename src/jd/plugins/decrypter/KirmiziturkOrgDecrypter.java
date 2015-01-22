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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kirmiziturk.org" }, urls = { "http://(www\\.)?kirmiziturk\\.org/video/\\d+" }, flags = { 0 })
public class KirmiziturkOrgDecrypter extends PluginForDecrypt {

    public KirmiziturkOrgDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        final DownloadLink main = createDownloadlink(parameter.replace("kirmiziturk.org/", "kirmiziturkdecrypted.org/"));
        try {
            main.setContentUrl(parameter);
        } catch (final Throwable e) {
            /* Not available in old 0.9.581 Stable */
            main.setBrowserUrl(parameter);
        }
        if (br.getURL().contains("<h3>404</h3>") || br.getHttpConnection().getResponseCode() == 404) {
            main.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }
        final String externID = br.getRegex("file: \"(https?://(www\\.)?youtube\\.com/watch\\?v=[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = new Regex(parameter, "(\\d+)$").getMatch(0);
        }
        filename += ".mp4";
        main.setAvailable(true);
        main.setName(filename);
        decryptedLinks.add(main);

        return decryptedLinks;
    }

}
