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

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "flowyourvideo.com" }, urls = { "https?://(?:stream|jwplayer)\\.flowyourvideo\\.com/embed/([a-z0-9]+)(?:\\?subtitles=.+)?" })
public class FlowyourvideoCom extends PluginForDecrypt {
    public FlowyourvideoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.getPage(parameter);
        if (jd.plugins.hoster.FlowyourvideoCom.isOffline(br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fid);
        try {
            final String json = br.getRegex("tracks\\s*:\\s*JSON\\.parse\\(\\'(.*?)'\\)").getMatch(0);
            LinkedHashMap<String, Object> entries = null;
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
            if (json != null) {
                logger.info("Subtitles seem to be available");
            }
            for (final Object subtitleO : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) subtitleO;
                final String label = (String) entries.get("label");
                String url = (String) entries.get("file");
                final String kind = (String) entries.get("kind");
                if (StringUtils.isEmpty(url) || StringUtils.isEmpty(label) || !"subtitles".equalsIgnoreCase(kind)) {
                    /* Skip invalid items */
                    continue;
                }
                if (url.startsWith("/")) {
                    url = "https://" + br.getHost() + url;
                }
                final String filename;
                if (ressourcelist.size() == 1) {
                    filename = fid + ".srt";
                } else {
                    /* Only put language in filename if there is more than 1 subtitle available. */
                    filename = fid + "_" + label + ".srt";
                }
                final DownloadLink dl = this.createDownloadlink(url + ".jdeatme");
                dl.setName(filename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        } catch (final Throwable e) {
            logger.info("Error in subtitle handling or no subtitles available");
        }
        final DownloadLink dl = createDownloadlink(parameter);
        dl.setName(fid + ".mp4");
        dl.setAvailable(true);
        decryptedLinks.add(dl);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
