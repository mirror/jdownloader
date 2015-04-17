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
import java.util.Map.Entry;
import java.util.Set;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "redbull.tv" }, urls = { "https?://(www\\.)?redbull.tv/(?:episodes|videos)/[A-Z0-9\\-]+/[a-z0-9\\-]+" }, flags = { 0 })
public class RedbullTv extends PluginForDecrypt {

    @SuppressWarnings("deprecation")
    public RedbullTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    // private static final String type_unsupported = "https?://(www\\.)?redbull.tv/videos/event\\-stream\\-\\d+/.+";

    /* Thx https://github.com/bromix/repository.bromix.storage/tree/master/plugin.video.redbull.tv */
    @SuppressWarnings("unchecked")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        boolean httpAvailable = false;
        final String parameter = param.toString();
        final String id = new Regex(parameter, "redbull.tv/[^/]+/([A-Z0-9\\-]+)/").getMatch(0);
        // if (parameter.matches(type_unsupported)) {
        // try {
        // decryptedLinks.add(this.createOfflinelink(parameter));
        // } catch (final Throwable e) {
        // /* Not available in old 0.9.581 Stable */
        // }
        // return decryptedLinks;
        // }
        br.getPage("https://api.redbull.tv/v1/videos/" + id);
        if (br.getHttpConnection().getResponseCode() == 404) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        final String title = (String) entries.get("title");
        final String subtitle = (String) entries.get("subtitle");
        String main_title = title;
        long season = -1;
        long episode = -1;
        if (entries.get("season_number") != null) {
            season = ((Number) entries.get("season_number")).longValue();
        }
        if (entries.get("episode_number") != null) {
            episode = ((Number) entries.get("episode_number")).longValue();
        }
        if (season > -1 && episode > -1) {
            final DecimalFormat df = new DecimalFormat("00");
            main_title += " - S" + df.format(season) + "E" + df.format(episode);
        }
        main_title = main_title + " - " + subtitle;
        entries = (LinkedHashMap<String, Object>) entries.get("videos");
        if (entries.get("master") != null) {
            httpAvailable = true;
            entries = (LinkedHashMap<String, Object>) entries.get("master");
        } else {
            entries = (LinkedHashMap<String, Object>) entries.get("demand");
        }
        if (httpAvailable) {
            /* Typically available for all normal videos. */
            entries = (LinkedHashMap<String, Object>) entries.get("renditions");
            final Set<Entry<String, Object>> entryset = entries.entrySet();
            for (Entry<String, Object> entry : entryset) {
                final String bitrate = entry.getKey();
                final String finallink = (String) entry.getValue();
                final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
                dl.setFinalFileName(main_title + "_" + bitrate + ".mp4");
                decryptedLinks.add(dl);
            }
        } else {
            /* Typically only needed for "live" streams (recordings) as for some reason they don't have http sources available. */
            final String hlsurl = (String) entries.get("uri");
            if (hlsurl == null) {
                return null;
            }
            decryptedLinks.add(createDownloadlink(hlsurl));
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(main_title);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
