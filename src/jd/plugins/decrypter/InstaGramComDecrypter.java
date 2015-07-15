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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DummyScriptEnginePlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "instagram.com" }, urls = { "https?://(www\\.)?instagram\\.com/(?!p/)[^/]+" }, flags = { 0 })
public class InstaGramComDecrypter extends PluginForDecrypt {

    public InstaGramComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.containsHTML("user\\?username=.+")) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        final String username_url = parameter.substring(parameter.lastIndexOf("/") + 1);
        final String json = br.getRegex(">window\\._sharedData = (\\{.*?);</script>").getMatch(0);
        final String id_owner = br.getRegex("\"owner\":\\{\"id\":\"(\\d+)\"\\}").getMatch(0);
        if (id_owner == null || json == null) {
            return null;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username_url);

        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(json);
        String nextid = (String) DummyScriptEnginePlugin.walkJson(entries, "entry_data/ProfilePage/{0}/user/media/page_info/end_cursor");
        final String maxid = (String) DummyScriptEnginePlugin.walkJson(entries, "entry_data/ProfilePage/{0}/__get_params/max_id");
        ArrayList<Object> resource_data_list = (ArrayList) DummyScriptEnginePlugin.walkJson(entries, "entry_data/ProfilePage/{0}/user/media/nodes");

        int page = 0;
        do {
            if (this.isAbort()) {
                logger.info("User aborted decryption");
                return decryptedLinks;
            }
            if (page > 0) {
                final String csrftoken = br.getCookie("instagram.com", "csrftoken");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getHeaders().put("X-Instagram-AJAX", "1");
                br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                if (csrftoken != null) {
                    br.getHeaders().put("X-CSRFToken", csrftoken);
                }
                if (maxid != null) {
                    this.br.getHeaders().put("Referer", "https://instagram.com/falllawayboy/?max_id=" + maxid);
                }
                this.br.postPage("https://instagram.com/query/", "q=ig_user(" + id_owner + ")+%7B+media.after(" + nextid + "%2C+12)+%7B%0A++count%2C%0A++nodes+%7B%0A++++caption%2C%0A++++code%2C%0A++++comments+%7B%0A++++++count%0A++++%7D%2C%0A++++date%2C%0A++++display_src%2C%0A++++id%2C%0A++++is_video%2C%0A++++likes+%7B%0A++++++count%0A++++%7D%2C%0A++++owner+%7B%0A++++++id%0A++++%7D%0A++%7D%2C%0A++page_info%0A%7D%0A+%7D&ref=users%3A%3Ashow");
                entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(this.br.toString());
                resource_data_list = (ArrayList) DummyScriptEnginePlugin.walkJson(entries, "media/nodes");
                nextid = (String) DummyScriptEnginePlugin.walkJson(entries, "media/page_info/end_cursor");
            }
            if (resource_data_list.size() == 0) {
                logger.info("Found no new links on page " + page + " --> Stopping decryption");
                break;
            }
            for (final Object o : resource_data_list) {
                entries = (LinkedHashMap<String, Object>) o;
                final String linkid = (String) entries.get("code");
                final boolean isVideo = ((Boolean) entries.get("is_video")).booleanValue();
                final String filename;
                if (isVideo) {
                    filename = username_url + " - " + linkid + ".mp4";
                } else {
                    filename = username_url + " - " + linkid + ".jpg";
                }
                final String content_url = "https://instagram.com/p/" + linkid;
                final DownloadLink dl = this.createDownloadlink(content_url);
                dl.setContentUrl(content_url);
                dl.setLinkID(linkid);
                dl._setFilePackage(fp);
                dl.setAvailable(true);
                dl.setName(filename);
                decryptedLinks.add(dl);
                distribute(dl);
            }
            page++;
        } while (nextid != null);

        return decryptedLinks;
    }
}
