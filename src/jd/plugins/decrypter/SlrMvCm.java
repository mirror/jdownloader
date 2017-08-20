//    jDownloader - Downloadmanager
//    Copyright (C) 2017  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.RefreshSessionLink;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.components.PluginJSonUtils;

/**
 *
 *
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision:$", interfaceVersion = 3, names = { "solarmoviez.to" }, urls = { "https?://(?:www\\.)?solarmoviez\\.to/movie/(?:[a-zA-Z0-9\\-_]+-\\d+/\\d+-\\d+|[a-zA-Z0-9\\-_]+-\\d+)(?:/watching)?\\.html" })
public class SlrMvCm extends antiDDoSForDecrypt implements RefreshSessionLink {

    private Browser ajax = null;

    public SlrMvCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (!parameter.contains("watching.html")) {
            parameter = parameter.replace(".html", "/watching.html");
        }
        final String mid = getMid(parameter);
        // problem with setting this, it will restrict results (other hosts/mirrors). but downside as it will decrypt large amounts as no
        // way to determine type
        final String eid = null; // getEid(parameter);
        br.setFollowRedirects(true);
        getPage(parameter);
        /* Error handling */
        if (isOffline()) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String title = br.getRegex("<title>\\s*(.*?)\\s*For Free[^<]+</title>").getMatch(0);
        if (title == null) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        // ajax stuff
        ajaxPost("/ajax/movie_update_view.html", "id=" + mid);
        // should have success
        if (PluginJSonUtils.parseBoolean(PluginJSonUtils.getJson(ajax, "status"))) {
            getPage(parameter.replace(".html", "/watching.html"));
        }
        ajaxGet(br, "/ajax/v4_movie_episodes/" + mid);
        // from here we get ep ids. these are the different servers.
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
        final HashSet<String> dupe = new HashSet<String>();
        final String html = (String) entries.get("html");
        // embed external links.. eg openload
        final String[] embed = new Regex(html, "<li id=\"sv-(\\d+)\"\\s+data-id=\"\\1\"\\s+class=\"server-item embed\"").getColumn(0);
        if (embed != null) {
            for (final String e : embed) {
                // match the id, to the html dataid
                final String dataid = new Regex(html, "data-server=\"" + Pattern.quote(e) + "\" data-id=\"(\\d+)").getMatch(0);
                if (dataid != null) {
                    ajax = br.cloneBrowser();
                    ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                    ajax.getPage("/ajax/movie_embed/" + dataid);
                    final String result = (String) ((LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString())).get("src");
                    decryptedLinks.add(createDownloadlink(result));
                    dupe.add(dataid);
                }
            }
        }
        // this contains the epid and titlename
        final String[][] epsTitle = new Regex(html, "data-id=\"(\\d+)\" id=\"ep-\\1\".*?<i class=\"icon-play_arrow\"></i>\\s*(.*?)\\s*</a>\\s*</li>").getMatches();
        for (final String[] eT : epsTitle) {
            // linked to embeded.
            if (!dupe.add(eT[0])) {
                continue;
            }
            // if the user copied link with epid we need only return that result
            if (eid != null && !eT[0].equals(eid)) {
                continue;
            }
            // not json...
            ajax = br.cloneBrowser();
            ajax.getHeaders().put("Accept", "text/javascript, application/javascript, application/ecmascript, application/x-ecmascript, */*; q=0.01");
            ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            getPage(ajax, "/ajax/movie_token?eid=" + eT[0] + "&mid=" + mid + "&_" + System.currentTimeMillis());
            // this gives you x and y
            final String x = ajax.getRegex("_x='([a-f0-9]{32})'").getMatch(0);
            final String y = ajax.getRegex("_y='([a-f0-9]{32})'").getMatch(0);
            ajaxGet(br, "/ajax/movie_sources/" + eT[0] + "?x=" + x + "&y=" + y);
            if (ajax.getHttpConnection().getLongContentLength() == 0 || ajax.toString().equals("No htmlCode read")) {
                continue;
            }
            // within json "file"
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
            // only ever one link returned by the requests above...
            // not all google video links, but two mirrors are? ones that are, maybe we can manipulate quals via changing the itag value?
            final ArrayList<Object> playlist = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "playlist/{0}/sources");
            for (final Object pl : playlist) {
                final LinkedHashMap<String, Object> playlizt = (LinkedHashMap<String, Object>) pl;
                String url = (String) playlizt.get("file");
                final String quality = (String) playlizt.get("label");
                final boolean m3u = url.contains(".m3u8");
                if (StringUtils.contains(url, "blogspot.com/")) {
                    final Browser test = br.cloneBrowser();
                    test.setFollowRedirects(false);
                    // 404 but image which throws IllegalStateException
                    URLConnectionAdapter con = null;
                    try {
                        con = test.openGetConnection(url);
                        url = test.getRedirectLocation();
                        if (url == null) {
                            continue;
                        }
                    } catch (final Exception e) {
                        continue;
                    } finally {
                        try {
                            if (con != null) {
                                con.disconnect();
                            }
                        } catch (Throwable t) {
                        }
                    }
                }
                final DownloadLink dl = createDownloadlink(url);
                // m3u plugin sets the quality figure already, we don't want duplicates.
                String fileName = eT[1];
                {
                    // some sections of the site don't have effectively have ep title other than quality reference.. so this is the fix for
                    // that
                    final boolean type1 = new Regex(fileName, "^[sh]d(?:\\s*-\\s*\\d+p)?$").matches();
                    final boolean type2 = new Regex(fileName, "^CAM$").matches();
                    if (type1 || type2) {
                        fileName = title + (type2 ? "-" + fileName : "");
                    }
                    if (!m3u) {
                        fileName += (quality != null ? "-" + quality : "");
                    }
                }
                dl.setName(fileName);
                dl.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
                dl.setProperty("refresh_url_plugin", getHost());
                dl.setProperty("source_quality", quality);
                dl.setProperty("source_url", parameter);
                dl.setProperty("source_eid", eT[0]);
                dl.setProperty("source_mid", mid);
                dl.setProperty("Referer", br.getURL());
                decryptedLinks.add(dl);
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private boolean isOffline() {
        final boolean result = br.getHttpConnection().getResponseCode() == 404;
        return result;
    }

    // this wont be always present
    private String getEid(String parameter) {
        String uid = new Regex(parameter, "\\d+/(\\d+)-\\d+(?:/watching)?\\.html").getMatch(0);
        return uid;
    }

    private String getMid(String parameter) {
        String uid = new Regex(parameter, "(\\d+)/\\d+-\\d+(?:/watching)?\\.html").getMatch(0);
        if (uid == null) {
            uid = new Regex(parameter, "(\\d+)(?:/watching)?\\.html").getMatch(0);
        }
        return uid;
    }

    private void ajaxGet(final Browser br, final String url) throws Exception {
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        getPage(ajax, url);
    }

    private void ajaxPost(final String url, final String string) throws Exception {
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        postPage(ajax, url, string);
    }

    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    /**
     * since each task below returns a single quality, we can just do this
     */
    @Override
    public String refreshVideoDirectUrl(DownloadLink dl) throws Exception {
        br = new Browser();
        final String ourl = dl.getStringProperty("source_url", null);
        final String oquality = dl.getStringProperty("source_quality", null);
        final String eid = dl.getStringProperty("source_eid", null);
        final String mid = dl.getStringProperty("source_mid", null);
        getPage(br, ourl);
        // ajax stuff
        ajaxPost("/ajax/movie_update_view.html", "id=" + mid);
        // should have success
        if (PluginJSonUtils.parseBoolean(PluginJSonUtils.getJson(ajax, "status"))) {
            getPage(ourl.replace(".html", "/watching.html"));
        }
        ajaxGet(br, "/ajax/v4_movie_episodes/" + mid);
        // from here we get ep ids. these are the different servers.
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
        final String html = (String) entries.get("html");
        // this contains the epid and titlename
        final String[][] epsTitle = new Regex(html, "data-id=\"(\\d+)\" id=\"ep-\\1\".*?<i class=\"icon-play_arrow\"></i>\\s*(.*?)\\s*</a>\\s*</li>").getMatches();
        for (final String[] eT : epsTitle) {
            // eid should never be null.. we also have to ONLY return the quality reference also!
            if (eid != null && !eT[0].equals(eid)) {
                continue;
            }
            // not json...
            ajax = br.cloneBrowser();
            ajax.getHeaders().put("Accept", "text/javascript, application/javascript, application/ecmascript, application/x-ecmascript, */*; q=0.01");
            ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            getPage(ajax, "/ajax/movie_token?eid=" + eT[0] + "&mid=" + mid + "&_" + System.currentTimeMillis());
            // this gives you x and y
            final String x = ajax.getRegex("_x='([a-f0-9]{32})'").getMatch(0);
            final String y = ajax.getRegex("_y='([a-f0-9]{32})'").getMatch(0);
            ajaxGet(br, "/ajax/movie_sources/" + eT[0] + "?x=" + x + "&y=" + y);
            if (ajax.getHttpConnection().getLongContentLength() == 0 || ajax.toString().equals("No htmlCode read")) {
                continue;
            }
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
            final ArrayList<Object> playlist = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "playlist/{0}/sources");
            for (final Object pl : playlist) {
                final LinkedHashMap<String, Object> playlizt = (LinkedHashMap<String, Object>) pl;
                String url = (String) playlizt.get("file");
                final String quality = (String) playlizt.get("label");
                // we need to match
                if (!oquality.equals(quality)) {
                    continue;
                }
                if (StringUtils.contains(url, "blogspot.com/")) {
                    final Browser test = br.cloneBrowser();
                    test.setFollowRedirects(false);
                    // 404 but image which throws IllegalStateException
                    URLConnectionAdapter con = null;
                    try {
                        con = test.openGetConnection(url);
                        url = test.getRedirectLocation();
                        if (url == null) {
                            return null;
                        }
                    } catch (final Exception e) {
                        return null;
                    } finally {
                        try {
                            if (con != null) {
                                con.disconnect();
                            }
                        } catch (Throwable t) {
                        }
                    }
                }
                return url;
            }
        }
        return null;
    }

}