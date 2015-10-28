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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DummyScriptEnginePlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "photobucket.com" }, urls = { "https?://(?:www\\.)?s\\d+\\.photobucket\\.com/user/[^/]+/library.+" }, flags = { 0 })
public class PhotobucketComAlbum extends PluginForDecrypt {

    public PhotobucketComAlbum(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        LinkedHashMap<String, Object> json;
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String internal_album_name = this.br.getRegex("photobucket\\.com/albums/([^/]+)/[^/]+/story/").getMatch(0);
        final String token = this.br.getRegex("name=\"token\" id=\"token\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (token == null || internal_album_name == null) {
            return null;
        }
        final Regex linkinfo = new Regex(parameter, "https?://(?:www\\.)?(s\\d+)\\.photobucket\\.com/user/([^/]+)/");
        final String server = linkinfo.getMatch(0);
        final String username = linkinfo.getMatch(1);
        final String fpName = username;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));

        // this.br.getHeaders().put("X-NewRelic-ID", "VgQFVFJWGwIFVFlSAQE=");
        /* Not really needed */
        this.br.getHeaders().put("X-NewRelic-ID", "");
        this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        /* Don't try more than 24 per page - it won't work - state 2015-10-28 */
        final long max_entries_per_page = 24;
        long image_count_total = 0;
        int page = 1;
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return decryptedLinks;
            }
            final String pageurl = "http://" + server + ".photobucket.com/component/Common-PageCollection-Album-AlbumPageCollection?filters[album]=/albums/" + internal_album_name + "/" + username + "&filters[album_content]=2&sort=3&limit=" + max_entries_per_page + "&page=" + page + "&linkerMode=&json=1&hash=" + token + "&_=" + System.currentTimeMillis();
            this.br.getPage(pageurl);
            json = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            if (image_count_total == 0) {
                image_count_total = DummyScriptEnginePlugin.toLong(DummyScriptEnginePlugin.walkJson(json, "body/total"), 0);
            }
            final ArrayList<Object> ressourcelist = (ArrayList) DummyScriptEnginePlugin.walkJson(json, "body/objects");
            for (final Object pico : ressourcelist) {
                json = (LinkedHashMap<String, Object>) pico;
                final String fname = (String) json.get("name");
                final String dlink = (String) json.get("linkUrl");
                final String userid = Long.toString(DummyScriptEnginePlugin.toLong(json.get("userId"), -1));
                if (fname == null || dlink == null || userid.equals("-1")) {
                    return null;
                }
                final DownloadLink dl = createDownloadlink(dlink);
                dl.setContentUrl(dlink);
                dl.setName(fname);
                dl.setLinkID(userid + fname);
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
            }
            if (ressourcelist.size() < max_entries_per_page) {
                /* Fail safe */
                break;
            }
            page++;
        } while (decryptedLinks.size() < image_count_total);

        return decryptedLinks;
    }
}
