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
import java.util.LinkedHashMap;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ampya.com" }, urls = { "http://(?:www\\.)?ampya\\.com/(artists/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+|shows/[A-Za-z0-9\\-_]+)" }) 
public class AmpyaComDecrypter extends PluginForDecrypt {

    public AmpyaComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        jd.plugins.hoster.AmpyaCom.initializeSession(this.br);

        br.getPage(jd.plugins.hoster.AmpyaCom.getApiUrl(parameter));
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        String fpName = null;
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("clips");
        for (final Object clipo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) clipo;
            final String title = (String) entries.get("title");
            final String artist = (String) entries.get("artist_name");
            final String video_id = Long.toString(JavaScriptEngineFactory.toLong(entries.get("video_id"), 0));
            if (fpName == null) {
                fpName = (String) entries.get("container_title");
            }
            if (title == null || artist == null || "0".equals(video_id)) {
                continue;
            }
            final DownloadLink dl = this.createDownloadlink("http://ampyadecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
            String filename = artist + " - " + title + ".mp4";
            filename = encodeUnicode(filename);
            dl.setFinalFileName(filename);
            dl.setAvailable(true);
            dl.setContentUrl(parameter);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("videoid", video_id);
            decryptedLinks.add(dl);
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
