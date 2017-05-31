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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "luscious.net" }, urls = { "https?://(?:www\\.)?members\\.luscious\\.net/albums/[a-z0-9\\-_]+_\\d+/" })
public class LusciousNetAlbum extends PluginForDecrypt {
    public LusciousNetAlbum(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML(">404 Not Found|No Album matches the given query")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String name_album = new Regex(parameter, "/albums/([^/]+)").getMatch(0);
        int page = 1;
        int addedItems = 0;
        boolean paginator_complete = false;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(name_album);
        do {
            addedItems = 0;
            if (this.isAbort()) {
                return decryptedLinks;
            }
            logger.info("Crawling page " + page);
            br.getPage("https://" + this.getHost() + "/c/wallpapers/pictures/album/" + name_album + "/sorted/newest/page/" + page + "/.json/");
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            paginator_complete = ((Boolean) entries.get("paginator_complete")).booleanValue();
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("documents");
            for (final Object pictureo : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) pictureo;
                final String title = (String) entries.get("title");
                final String view_page_url = (String) entries.get("view_page_url");
                if (StringUtils.isEmpty(title) || StringUtils.isEmpty(view_page_url)) {
                    continue;
                }
                final DownloadLink dl = this.createDownloadlink("https://" + this.getHost() + view_page_url);
                dl.setName(title + ".jpg");
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
                addedItems++;
            }
            page++;
        } while (addedItems >= 50 && !paginator_complete);
        return decryptedLinks;
    }
}
