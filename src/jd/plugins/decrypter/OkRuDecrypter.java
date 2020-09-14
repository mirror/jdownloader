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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ok.ru" }, urls = { "https?://(?:[A-Za-z0-9]+\\.)?(?:ok\\.ru|odnoklassniki\\.ru)/(?:video|videoembed|web-api/video/moviePlayer|live)/(\\d+(-\\d+)?)" })
public class OkRuDecrypter extends PluginForDecrypt {
    public OkRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String vid = new Regex(param.toString(), this.getSupportedLinks()).getMatch(0);
        final String parameter = "https://ok.ru/video/" + vid;
        param.setCryptedUrl(parameter);
        jd.plugins.hoster.OkRu.prepBR(this.br);
        br.getPage("https://ok.ru/video/" + vid);
        if (jd.plugins.hoster.OkRu.isOffline(br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String externID = null;
        String provider = null;
        final LinkedHashMap<String, Object> entries = jd.plugins.hoster.OkRu.getFlashVars(br);
        if (entries != null) {
            provider = (String) entries.get("provider");
            externID = (String) JavaScriptEngineFactory.walkJson(entries, "movie/contentId");
        }
        if ("USER_YOUTUBE".equalsIgnoreCase(provider) && !StringUtils.isEmpty(externID)) {
            decryptedLinks.add(createDownloadlink("https://www.youtube.com/watch?v=" + externID));
            return decryptedLinks;
        }
        /* 2019-10-15: TODO: Check if this is still working */
        externID = this.br.getRegex("coubID=([A-Za-z0-9]+)").getMatch(0);
        if (externID == null) {
            externID = this.br.getRegex("coub\\.com%2Fview%2F([A-Za-z0-9]+)").getMatch(0);
        }
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(String.format("https://coub.com/view/%s", externID)));
            return decryptedLinks;
        }
        /* No external hosting provider found --> Content should be hosted by ok.ru --> Pass over to hosterplugin. */
        final DownloadLink main = createDownloadlink(param.toString());
        main.setLinkID(getHost() + "://" + vid);
        main.setName(vid);
        if (jd.plugins.hoster.OkRu.isOffline(this.br)) {
            main.setAvailable(false);
        }
        decryptedLinks.add(main);
        return decryptedLinks;
    }
}
