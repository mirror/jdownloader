//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import jd.PluginWrapper;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.html.XPath;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "lastfm.de" }, urls = { "http://wwwik3mg23seryi4521\\.(fm|de|pl|es|fr|it|jp|com\\.br|ru|se|com\\.tr)/(music/.+/_/.+)" }, flags = { PluginWrapper.DEBUG_ONLY })
public class LastFm extends PluginForHost {
    private static final HashMap<String, FilePackage> PACKAGE_CACHE = new HashMap<String, FilePackage>();
    private static final String API_ROOT = "http://ext.last.fm/2.0/?";
    private static final String API_KEY = "api_key";
    private static final String API_KEY_VALUE = "da6ae1e99462ee22e81ac91ed39b43a4";
    private static final String API_SIG = "api_sig";
    private static final String API_SIG_SECRET = "4655c1c8293fcf46fc1bee6a07162e25";
    private static final String API_METHOD = "method";

    public LastFm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.last.fm/legal/terms";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("ik3mg23seryi4521", ".last"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        // TODO some songs are not available...so this plugin downloads another
        // song....
        // we should filter these unavailable links.
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        
        if(br.containsHTML("Track not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String title = br.getRegex("class=\"preview track\">(.*?)</p>").getMatch(0);
        String artist = br.getRegex("class=\"preview artist\">(.*?)</p>").getMatch(0);
        title = Encoding.htmlDecode(title);
        artist = Encoding.htmlDecode(artist);

        if (br.containsHTML("Wir haben keine Titel")) {
            link.getLinkStatus().setStatusText(JDL.L("jd.plugins.hoster.LastFm.requestFileInformation.nodownload", "Not Downloadable"));
            link.getLinkStatus().setErrorMessage(JDL.L("jd.plugins.hoster.LastFm.requestFileInformation.nodownload", "Not Downloadable"));
            return AvailableStatus.FALSE;
        }
        String duration = br.getRegex("\"duration\"\\:\"(.*?)\"").getMatch(0);
        // //aproximate size
        int size = Integer.parseInt(duration) * 128 * 1024 / 8;
        link.setFinalFileName(artist + " - " + title + ".mp3");
        link.setDownloadSize(size);
        // create package with artistnames
        FilePackage fp = PACKAGE_CACHE.get(artist);
        if (fp == null) {
            fp = FilePackage.getInstance();
            fp.setName(artist);
            fp.setComment("Songs from lastfm by " + artist);
            PACKAGE_CACHE.put(artist, fp);
        }
        link.setFilePackage(fp);
        //     
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        br.getPage(link.getDownloadURL());
        if(br.containsHTML("Track not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        
        String title = br.getRegex("class=\"preview track\">(.*?)</p>").getMatch(0);
        String artist = br.getRegex("class=\"preview artist\">(.*?)</p>").getMatch(0);
        title = Encoding.htmlDecode(title);
        artist = Encoding.htmlDecode(artist);

        // getWebSession
        Map<String, String> args = new HashMap<String, String>();
        args.put("flashresponse", "true");
        args.put(API_KEY, API_KEY_VALUE);
        args.put(API_METHOD, "auth.getWebSession");
        args.put("y", Long.toString(System.currentTimeMillis() / 1000));
        args.put(API_SIG, createApiSig(args));

        br.getPage(createUrl(args));
        if(br.containsHTML("error")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        String query = "/lfm/session/key";
        XPath xpath = new XPath(br.toString(), query, false);
        String token = xpath.getMatches().get(0);

        // getPlaylist
        args.clear();
        args.put("flashresponse", "true");
        args.put("track", title);
        args.put("artist", artist);
        args.put(API_KEY, API_KEY_VALUE);
        args.put(API_METHOD, "track.getplayermenu");
        args.put("y", Long.toString(System.currentTimeMillis() / 1000));

        br.getPage(createUrl(args));
        if(br.containsHTML("error")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        query = "/lfm/playlists/playlist/url";
        xpath = new XPath(br.toString(), query, false);
        String playlist = xpath.getMatches().get(0);

        // playlist.fetch #1
        args.clear();
        args.put(API_KEY, API_KEY_VALUE);
        args.put("flashresponse", "true");
        args.put("fod", "true");
        args.put("lang", "de");
        args.put(API_METHOD, "playlist.fetch");
        args.put("playlistURL", playlist);
        args.put("sk", token);
        args.put("viewport", "true");
        args.put("y", Long.toString((System.currentTimeMillis() / 1000)));
        args.put(API_SIG, createApiSig(args));

        br.getPage(createUrl(args));
        if(br.containsHTML("error")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        query = "/lfm/playlist/trackList/track/location";
        xpath = new XPath(br.toString(), query, false);
        String songLink = xpath.getMatches().get(0);

        // playlist.fetch #2
        args.remove(API_SIG);
        args.remove("y");
        args.remove("viewport");
        args.put("streaming", "true");
        args.put("y", Long.toString((System.currentTimeMillis() / 1000)));
        args.put(API_SIG, createApiSig(args));

        br.getPage(createUrl(args));
        if(br.containsHTML("error")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        xpath = new XPath(br.toString(), query, false);
        songLink = xpath.getMatches().get(0);

        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, songLink, false, 1);
        dl.startDownload();
    }

    private String createUrl(Map<String, String> args) {
        String url = API_ROOT;
        Set<Entry<String, String>> test = args.entrySet();
        Iterator<Entry<String, String>> testIt = test.iterator();
        while (testIt.hasNext()) {
            Entry<String, String> ent = testIt.next();
            url = url.concat(Encoding.urlEncode(ent.getKey()) + "=" + Encoding.urlEncode(ent.getValue()));
            if (testIt.hasNext()) {
                url = url.concat("&");
            }
        }
        return url;
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private String createApiSig(Map<String, String> parameter) {
        Set<String> sorted = new TreeSet<String>(parameter.keySet());
        StringBuilder builder = new StringBuilder(50);
        for (String s : sorted) {
            builder.append(Encoding.urlEncode_light(s));
            builder.append(Encoding.urlEncode_light(parameter.get(s)));
        }
        builder.append(API_SIG_SECRET);
        return JDHash.getMD5(builder.toString());

    }
}
