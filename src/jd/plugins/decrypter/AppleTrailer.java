//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.List;
import java.util.Map;

import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.UserAgents;

/**
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "trailers.apple.com" }, urls = { "https?://[\\w\\.]*?apple\\.com/trailers/(disney|dreamworks|entertainmentone|filmdistrict|focus_features|fox|fox_searchlight|independent|ifcfilms|lions_gate|lucasfilm|magnolia|marvel|mgm|oscilloscope|paramount|picturehouse|relativity|roadsideattractions|sony|sony_pictures|summit|(universial|universal)|wb|(weinstein|weinstien))/[a-zA-Z0-9_\\-]+/" })
public class AppleTrailer extends PluginForDecrypt {

    public AppleTrailer(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Browser                       br2            = null;
    private String                        parameter      = null;
    private String                        title          = null;
    private final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();;
    private final HashSet<String>         dupe           = new HashSet<String>();

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        // prevent results from carring over when debugging
        decryptedLinks.clear();
        dupe.clear();

        // cleanup required
        parameter = param.toString().replaceAll("://(\\w+\\.)?apple", "://trailers.apple");
        br = new Browser();
        br.getHeaders().put("User-Agent", UserAgents.stringUserAgent());

        // make sure they don't have any stupid redirects here
        br.setFollowRedirects(true);
        br.getPage(parameter);
        br.setFollowRedirects(false);

        if (br.containsHTML(">Hmm, the page you’re looking for can’t be found")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        final String filmID = br.getRegex("var\\s*FilmId\\s*=\\s*'(\\d+)'").getMatch(0);
        if (filmID != null) {
            processFeed(filmID);
        } else {
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }

        if (StringUtils.isNotEmpty(title)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title.trim());
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    private void processFeed(String filmID) throws Exception {
        br2 = br.cloneBrowser();
        br2.getPage("//trailers.apple.com/trailers/feeds/data/" + filmID + ".json");
        final LinkedHashMap<String, Object> json;
        try {
            if (br2.containsHTML("404 - Page Not Found")) {
                return;
            }
            json = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br2.toString());
        } catch (Exception e) {
            return;
        }
        final Map<String, Object> page = (Map<String, Object>) json.get("page");
        if (page != null) {
            title = (String) page.get("movie_title");
            final List<Map<String, Object>> clips = (List<Map<String, Object>>) json.get("clips");
            for (final Map<String, Object> clip : clips) {
                final String clipTitle = (String) clip.get("title");
                final String[] sizes = new Regex(clip.toString(), "src=((https?://.*?apple[^<>]*?|/[^<>]*?)_h?\\d+p\\.mov)").getColumn(0);
                if (sizes != null) {
                    // temp arraylist
                    final ArrayList<DownloadLink> temp = new ArrayList<DownloadLink>();
                    for (final String size : sizes) {
                        /* correct url */
                        String url = size.replaceFirst("movies\\.", "www.");
                        if (dupe.add(url) == false) {
                            continue;
                        }
                        /* get format */
                        String format = new Regex(url, "_h?(\\d+)p").getMatch(0);
                        /* get filename */
                        String fname = title + "-" + clipTitle + " (" + p_q(format) + ")" + getFileNameExtensionFromString(url, ".mov");
                        if (fname == null || format == null) {
                            continue;
                        }
                        final DownloadLink dlLink = createDownloadlink(url.replace(".apple.com", ".appledecrypted.com"));
                        dlLink.setLinkID(getHost() + "://" + filmID + "/" + Hash.getMD5(clipTitle) + "/" + format);
                        dlLink.setFinalFileName(fname);
                        dlLink.setProperty("pSize", format);
                        dlLink.setProperty("Referer", br.getURL());
                        dlLink.setAvailable(true);
                        temp.add(dlLink);
                    }
                    decryptedLinks.addAll(analyseUserSettings(temp));
                }
            }
        }
    }

    private ArrayList<DownloadLink> analyseUserSettings(final ArrayList<DownloadLink> links) {
        if (this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.TrailersAppleCom.preferBest, jd.plugins.hoster.TrailersAppleCom.preferBest_default)) {
            int bestest = 0;
            DownloadLink bdlink = null;
            for (final DownloadLink dl : links) {
                if (dl.getStringProperty("pSize", null) != null) {
                    int p = Integer.parseInt(dl.getStringProperty("pSize"));
                    if (!isPqualityEnabled(p)) {
                        continue;
                    }
                    if (p > bestest) {
                        bestest = p;
                        bdlink = dl;
                    }
                }
            }
            if (bdlink != null) {
                final ArrayList<DownloadLink> b = new ArrayList<DownloadLink>();
                b.add(bdlink);
                return b;
            }
        }
        // we need code to still respect checkboxes when best isn't enabled
        final ArrayList<DownloadLink> results = new ArrayList<DownloadLink>();
        for (final DownloadLink dl : links) {
            if (dl.getStringProperty("pSize", null) != null) {
                int p = Integer.parseInt(dl.getStringProperty("pSize"));
                if (isPqualityEnabled(p)) {
                    results.add(dl);
                }
            }
        }
        return results.isEmpty() ? links : results;
    }

    private final boolean isPqualityEnabled(final int p) {
        if (p == 1080) {
            return this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.TrailersAppleCom.p1080, jd.plugins.hoster.TrailersAppleCom.p1080_default);
        } else if (p == 720) {
            return this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.TrailersAppleCom.p720, jd.plugins.hoster.TrailersAppleCom.p720_default);
        } else if (p == 480) {
            return this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.TrailersAppleCom.p480, jd.plugins.hoster.TrailersAppleCom.p480_default);
        } else if (p == 360) {
            return this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.TrailersAppleCom.p360, jd.plugins.hoster.TrailersAppleCom.p360_default);
        } else {
            return true;
        }
    }

    private String p_q(final String p) {
        final int dd = Integer.parseInt(p);
        if (dd >= 720) {
            return p + "p_HD";
        } else {
            return p + "p_SD";
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}