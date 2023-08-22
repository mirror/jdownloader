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
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pony.fm" }, urls = { "https?://(?:www\\.)?pony\\.fm/tracks/\\d+" })
public class PonyFm extends PluginForDecrypt {
    public PonyFm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = new Regex(parameter, "pony\\.fm/tracks/(\\d+)").getMatch(0);
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.getPage("https://pony.fm/api/web/tracks/" + fid + "?log=true");
        Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("\"Track not found")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        entries = (Map<String, Object>) entries.get("track");
        final String song_name = (String) entries.get("title");
        Map<String, Object> streams = (Map<String, Object>) entries.get("streams");
        Map<String, Object> covers = (Map<String, Object>) entries.get("covers");
        final String url = (String) streams.get("mp3");
        final String ext = ".mp3";
        final DownloadLink fina = createDownloadlink(url);
        fina.setFinalFileName(song_name + "." + ext);
        fina.setAvailable(true);
        decryptedLinks.add(fina);
        /* Add cover */
        final String urlCover = (String) covers.get("original");
        final String extCover = getFileNameExtensionFromString(urlCover);
        final DownloadLink dlcover = createDownloadlink(urlCover);
        dlcover.setFinalFileName(song_name + "_cover" + "." + extCover);
        dlcover.setAvailable(true);
        decryptedLinks.add(dlcover);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(song_name);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
