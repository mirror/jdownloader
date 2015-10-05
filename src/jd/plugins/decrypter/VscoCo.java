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
import jd.plugins.hoster.DummyScriptEnginePlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vsco.co" }, urls = { "https?://[^/]+\\.vsco\\.co/grid/\\d+" }, flags = { 0 })
public class VscoCo extends PluginForDecrypt {

    public VscoCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String username = new Regex(parameter, "https?://([^/]+)\\.vsco\\.co/").getMatch(0);
        this.br.getPage(parameter);
        final String cookie_vs = this.br.getCookie(this.getHost(), "vs");
        final String siteid = this.getJson("id");
        long amount_total = 0;
        /* More than 500 possible */
        int max_count_per_page = 500;
        int page = 1;
        if (cookie_vs == null || siteid == null) {
            return null;
        }
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                break;
            }
            this.br.getPage("http://" + username + ".vsco.co/ajxp/" + cookie_vs + "/2.0/medias?site_id=" + siteid + "&page=" + page + "&size=" + max_count_per_page);
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            if (page == 1) {
                amount_total = DummyScriptEnginePlugin.toLong(entries.get("total"), 0);
            }
            final ArrayList<Object> ressources = (ArrayList) entries.get("media");
            for (final Object ressource : ressources) {
                entries = (LinkedHashMap<String, Object>) ressource;
                final String fid = (String) entries.get("_id");
                if (fid == null) {
                    return null;
                }
                final String url_content = "http://" + username + ".vsco.co/media/" + fid;
                final String filename = username + "_" + fid + ".jpg";
                final DownloadLink dl = this.createDownloadlink(url_content);
                dl.setContentUrl(url_content);
                dl.setName(filename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                distribute(dl);
            }
            if (ressources.size() < max_count_per_page) {
                /* Fail safe */
                break;
            }
            page++;
        } while (decryptedLinks.size() < amount_total);

        if (decryptedLinks.size() == 0 && !this.isAbort()) {
            return null;
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

}
