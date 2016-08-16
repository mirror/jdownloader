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
import jd.http.URLConnectionAdapter;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vid.me" }, urls = { "https?://(?:www\\.)?vid\\.me/(?:e/)?[^/]+" }, flags = { 0 })
public class VidMeProfile extends PluginForDecrypt {

    public VidMeProfile(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_EMBED = "https?://(?:www\\.)?vid\\.me/e/[^/]+";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Load sister host plugin */
        JDUtilities.getPluginForHost("vid.me");
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String linkid = parameter.substring(parameter.lastIndexOf("/") + 1);
        if (parameter.matches(TYPE_EMBED)) {
            /* Single video */
            final DownloadLink dl = this.createDownloadlink("https://viddecrypted.me/" + linkid);
            dl.setLinkID(linkid);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        jd.plugins.hoster.VidMe.api_prepBR(this.br);

        URLConnectionAdapter con = null;
        try {
            con = this.br.openHeadConnection(jd.plugins.hoster.VidMe.api_get_video(parameter));
            if (this.br.getHttpConnection().getResponseCode() == 200) {
                /* Single video */
                final DownloadLink dl = this.createDownloadlink("https://viddecrypted.me/" + linkid);
                dl.setLinkID(linkid);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            /* Typically 400 video offline - it could still be a users' profile. */
            this.br.getPage(jd.plugins.hoster.VidMe.api_get_userinfo(linkid));
            if (this.br.getHttpConnection().getResponseCode() != 200) {
                /* Typically 400 user not found */
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }

            /* Now we know for sure that we got a users' profile --> Get userID, then find all videos */
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
            entries = (LinkedHashMap<String, Object>) entries.get("user");
            final String user_id = Long.toString(JavaScriptEngineFactory.toLong(entries.get("user_id"), -1));
            final long video_count = JavaScriptEngineFactory.toLong(entries.get("user_id"), -1);
            final long max_videos_per_page = jd.plugins.hoster.VidMe.api_get_max_videos_per_page();
            long offset = 0;
            ArrayList<Object> ressourcelist = null;
            if ("-1".equals(user_id) || video_count == -1) {
                return null;
            } else if (video_count == 0) {
                /* User has no videos to download */
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }

            final FilePackage fp = FilePackage.getInstance();
            fp.setName(linkid);

            do {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user");
                    return decryptedLinks;
                }
                this.br.getPage(jd.plugins.hoster.VidMe.api_get_user_videos(user_id, Long.toString(offset)));
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
                ressourcelist = (ArrayList) entries.get("videos");
                for (final Object videoo : ressourcelist) {
                    entries = (LinkedHashMap<String, Object>) videoo;
                    final String title = jd.plugins.hoster.VidMe.getVideoTitle(this, entries);
                    final String videoid = jd.plugins.hoster.VidMe.getVideoID(entries);
                    if (title == null || videoid == null) {
                        return null;
                    }
                    final DownloadLink dl = this.createDownloadlink("https://viddecrypted.me/" + videoid);
                    dl._setFilePackage(fp);
                    dl.setAvailable(true);
                    dl.setName(title + jd.plugins.hoster.VidMe.default_Extension);
                    dl.setLinkID(videoid);
                    dl.setContentUrl("https://vid.me/" + videoid);
                    decryptedLinks.add(dl);
                    distribute(dl);
                    offset++;
                }
                if (ressourcelist == null || ressourcelist.size() < max_videos_per_page) {
                    /* Seems like we decrypted everything! */
                    break;
                }
            } while (decryptedLinks.size() < video_count);
            fp.addLinks(decryptedLinks);

        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return decryptedLinks;
    }

}
