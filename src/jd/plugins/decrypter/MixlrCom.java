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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mixlr.com" }, urls = { "http://(?:www\\.)?mixlr\\.com/[a-z0-9\\-]+/[a-z0-9\\-]+/?([a-z0-9\\-]+/?|\\?page=[0-9]+)?" })
public class MixlrCom extends PluginForDecrypt {
    public MixlrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_invalid = "https?://(?:www\\.)?mixlr\\.com/.+/(embed|chat|crowd).*?";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(type_invalid)) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        final boolean isShowreelPage = br.getURL().endsWith("showreel/") || br.getURL().contains("?page=");
        String fpName = br.getRegex("<title>(.+?)</title>").getMatch(0).trim();
        fpName = isShowreelPage ? fpName.replace(" | Mixlr", "") : fpName.replace(" broadcast live on Mixlr.", "");
        final String regex = isShowreelPage ? "var broadcasts = (\\[.+?\\]);" : "var savedBroadcast = (\\{.+?\\});";
        final String jsarray = br.getRegex(regex).getMatch(0);
        if (jsarray == null) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        ArrayList<Object> ressourcelist;
        if (isShowreelPage) {
            ressourcelist = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(jsarray);
        } else {
            ressourcelist = new ArrayList<Object>();
            ressourcelist.add(JavaScriptEngineFactory.jsonToJavaObject(jsarray));
        }
        for (final Object mobject : ressourcelist) {
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) mobject;
            final LinkedHashMap<String, Object> streams = (LinkedHashMap<String, Object>) entries.get("streams");
            final LinkedHashMap<String, Object> stream_http = (LinkedHashMap<String, Object>) streams.get("http");
            final String url = (String) stream_http.get("url");
            final String username = (String) entries.get("username");
            String title = (String) entries.get("title");
            if (url == null || username == null || title == null) {
                return null;
            }
            // Some artists use the same title for every show, so adding the number from the slug and the date helps to differentiate files
            final String slug = (String) entries.get("slug");
            final String[] slugParts = slug.split("-");
            // Add the last portion of the slug (normally a sequence number) if the slugified title has fewer parts than the slug
            final String slugifiedTitle = title.toLowerCase().replaceAll("[\\\\!,./]", "").replaceAll("\\s+", "-");
            final String[] slugifiedTitleParts = slugifiedTitle.split("-");
            if (slugParts.length == slugifiedTitleParts.length + 1) {
                title += " " + slugParts[slugParts.length - 1];
            }
            String broadcastDate = (String) entries.get("started_at");
            // Fall back to the date it was uploaded to Mixlr if the start date is not found
            if (broadcastDate == null) {
                broadcastDate = (String) entries.get("saved_at");
            }
            broadcastDate = broadcastDate.substring(0, "yyyy-MM-dd".length());
            final String filename = username + " - " + broadcastDate + " " + title + ".mp3";
            final DownloadLink dl = createDownloadlink("directhttp://" + url);
            dl.setFinalFileName(filename);
            dl.setAvailable(true);
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
