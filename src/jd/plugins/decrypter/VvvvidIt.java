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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DummyScriptEnginePlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vvvvid.it" }, urls = { "https?://(?:www\\.)?vvvvid\\.it/#\\!show/\\d+/[a-z0-9\\-]+" }, flags = { 0 })
public class VvvvidIt extends PluginForDecrypt {

    public VvvvidIt(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * A lot of countries (e.g. Germany) are GEO-blocked. VPN needed to access the website but download of their (hls) streams is possible
     * without VPN.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        this.br.setFollowRedirects(true);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final Regex urlinfo = new Regex(parameter, "vvvvid\\.it/#\\!show/(\\d+)/([a-z0-9\\-]+)");
        final String show_id_str = urlinfo.getMatch(0);
        final long show_id = Long.parseLong(show_id_str);
        final long season_id = show_id - 10;
        this.br.getPage("http://www.vvvvid.it/vvvvid/ondemand/" + show_id + "/info/");
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.getHttpConnection().getContentType().contains("json")) {
            /* Offline or geo-blocked */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("data");

        String show_title_main = (String) entries.get("title");
        if (inValidate(show_title_main)) {
            show_title_main = urlinfo.getMatch(1);
        }

        this.br.getPage("/vvvvid/ondemand/" + show_id + "/season/" + season_id);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(show_title_main);

        final DecimalFormat df = new DecimalFormat("00");
        entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        final ArrayList<Object> resource_data_list = (ArrayList) entries.get("data");
        for (final Object resource_o : resource_data_list) {
            entries = (LinkedHashMap<String, Object>) resource_o;
            final String show_title = (String) entries.get("show_title");
            final String title = (String) entries.get("title");
            final String source_type = (String) entries.get("source_type");
            final String hls_master = (String) entries.get("embed_info");

            final long season = DummyScriptEnginePlugin.toLong(entries.get("season_number"), -1);
            final long episode = DummyScriptEnginePlugin.toLong(entries.get("number"), -1);

            if (inValidate(show_title) || inValidate(title) || inValidate(source_type) || inValidate(hls_master) || season == -1 || episode == -1) {
                return null;
            }

            if (!source_type.equals("video")) {
                continue;
            }

            final DownloadLink dl = createDownloadlink("http://vvvviddecrypted.it/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
            String filename = show_title + "_S" + df.format(season) + "E" + df.format(episode) + "_" + title + ".mp4";
            dl.setProperty("filename", filename);
            dl.setProperty("directlink", hls_master);
            dl.setName(filename);
            dl.setContentUrl(parameter);
            dl.setAvailable(true);
            dl.setLinkID(show_id_str + "_" + filename);
            dl._setFilePackage(fp);
            distribute(dl);
            decryptedLinks.add(dl);
        }

        return decryptedLinks;
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }
}
