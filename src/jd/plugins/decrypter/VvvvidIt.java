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
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vvvvid.it" }, urls = { "https?://(?:www\\.)?vvvvid\\.it/#\\!show/\\d+/[a-z0-9\\-]+(?:/\\d+/\\d+)?" })
public class VvvvidIt extends PluginForDecrypt {

    public VvvvidIt(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * A lot of countries (e.g. Germany) are GEO-blocked. VPN needed to access the website but download of their (hls) streams is possible
     * without VPN.
     */
    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        this.br.setFollowRedirects(true);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final Regex urlinfo = new Regex(parameter, "vvvvid\\.it/#\\!show/(\\d+)/([a-z0-9\\-]+)");
        final Regex urlinfo_2 = new Regex(parameter, "vvvvid\\.it/#\\!show/\\d+/[a-z0-9\\-]+/(\\d+)/(\\d+)");
        final String show_id_str = urlinfo.getMatch(0);
        final String season_id_str = urlinfo_2.getMatch(0);
        final String episode_videoid = urlinfo_2.getMatch(1);
        String conn_id = null;
        final Account aa = AccountController.getInstance().getValidAccount(JDUtilities.getPluginForHost(this.getHost()));
        if (aa != null) {
            jd.plugins.hoster.VvvvidIt.login(this.br, aa, false);
            conn_id = jd.plugins.hoster.VvvvidIt.getConnIDFromAccount(aa);
        } else {
            prepBR(this.br);
            br.getPage(parameter);
            conn_id = getConnID(this.br);
        }

        long episode_videoid_target = 0;
        if (episode_videoid != null) {
            episode_videoid_target = Long.parseLong(episode_videoid);
        }
        final long show_id = Long.parseLong(show_id_str);
        final long season_id;
        LinkedHashMap<String, Object> entries = null;
        if (season_id_str != null) {
            /* season_id given via URL */
            season_id = Long.parseLong(season_id_str);
        } else {
            /* season_id not given --> Get it */
            this.br.getPage("/vvvvid/ondemand/" + show_id + "/seasons/?conn_id=" + conn_id);
            if (this.br.getHttpConnection().getResponseCode() == 403) {
                logger.info("GEO-blocked");
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            } else if (this.br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }

            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
            final Object season_id_o = JavaScriptEngineFactory.walkJson(entries, "data/{0}/season_id");
            season_id = JavaScriptEngineFactory.toLong(season_id_o, (show_id - 10));
        }

        this.br.getPage("/vvvvid/ondemand/" + show_id + "/info/" + "?conn_id=" + conn_id);
        if (this.br.getHttpConnection().getResponseCode() == 403) {
            logger.info("GEO-blocked");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.getHttpConnection().getContentType().contains("json")) {
            /* Offline or geo-blocked */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        entries = (LinkedHashMap<String, Object>) entries.get("data");

        String show_title_main = (String) entries.get("title");
        if (inValidate(show_title_main)) {
            show_title_main = urlinfo.getMatch(1);
        }

        this.br.getPage("/vvvvid/ondemand/" + show_id + "/season/" + season_id + "?conn_id=" + conn_id);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(show_title_main);

        final DecimalFormat df = new DecimalFormat("00");
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final ArrayList<Object> resource_data_list = (ArrayList) entries.get("data");
        for (final Object resource_o : resource_data_list) {
            entries = (LinkedHashMap<String, Object>) resource_o;
            final String show_title = (String) entries.get("show_title");
            final String title = (String) entries.get("title");
            final String source_type = (String) entries.get("source_type");
            final String hls_master = (String) entries.get("embed_info");

            final long videoid = JavaScriptEngineFactory.toLong(entries.get("video_id"), -1);
            final long season = JavaScriptEngineFactory.toLong(entries.get("season_number"), -1);
            final long episode = JavaScriptEngineFactory.toLong(entries.get("number"), -1);

            if (inValidate(hls_master)) {
                /* Probably video is not playable without account */
                continue;
            }

            if (inValidate(show_title) || inValidate(title) || inValidate(source_type) || season == -1 || episode == -1 || videoid == -1) {
                return null;
            }

            if (!source_type.equals("video")) {
                continue;
            }

            if (episode_videoid_target > 0 && videoid != episode_videoid_target) {
                /* User wants one specified episode only --> Skip all others! */
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

    public static Browser prepBR(final Browser br) {
        br.setCookie("https://www.vvvvid.it/", "vvvvid_cookies_accepted", "1");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:45.0) Gecko/20100101 Firefox/45.0");
        br.setFollowRedirects(true);
        return br;
    }

    public static String getConnID(final Browser br) throws Exception {
        br.postPageRaw("http://www.vvvvid.it/user/login", "{\"action\":\"login\",\"email\":\"\",\"password\":\"\",\"facebookParams\":\"\",\"mobile\":false,\"hls\":true,\"flash\":true,\"isIframe\":false}");
        String conn_id = PluginJSonUtils.getJsonValue(br, "conn_id");
        if (conn_id != null) {
            conn_id = Encoding.urlEncode(conn_id);
        }
        return conn_id;
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
