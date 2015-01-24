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
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ndr.de" }, urls = { "https?://(www\\.)?ndr\\.de/fernsehen/sendungen/[A-Za-z0-9\\-_]+/[^<>\"/]+\\.html" }, flags = { 0 })
public class NdrDeDecrypter extends PluginForDecrypt {

    public NdrDeDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String domain      = "ndr.de";
    private final String[]      qualities   = { "hq", "hi", "lo" };

    private static final String Q_SUBTITLES = "Q_SUBTITLES";
    private static final String Q_BEST      = "Q_BEST";
    private static final String Q_LOW       = "Q_LOW";
    private static final String Q_HIGH      = "Q_HIGH";
    private static final String Q_VERYHIGH  = "Q_VERYHIGH";

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        LinkedHashMap<String, DownloadLink> foundqualities = new LinkedHashMap<String, DownloadLink>();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("https://", "http://");
        final String url_id = new Regex(parameter, "([a-z0-9]+)\\.html$").getMatch(0);
        br.setFollowRedirects(true);
        /* API - well, basically returns same html code as if we simply access the normal link... */
        br.getPage("http://www.ndr.de/epg/" + url_id + ".json");
        if (br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String title = br.getRegex("name=\"title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (title == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String availablequalitiestext = br.getRegex("\\.,([a-z,]+),\\.mp4\\.csmil/master\\.m3u8\\'").getMatch(0);
        final Regex afnreg = br.getRegex("afn: \"TV-(\\d{4})(\\d{4})([0-9\\-]+)\"");
        final String v_year = afnreg.getMatch(0);
        final String v_id = afnreg.getMatch(1);
        final String v_rest = afnreg.getMatch(2);
        if (availablequalitiestext == null || v_id == null || v_year == null || v_id == null || v_rest == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String subtitle_url = br.getRegex("\"(/media/ut\\d+\\.xml)\"").getMatch(0);
        if (subtitle_url != null) {
            subtitle_url = "https://www.ndr.de" + subtitle_url;
        }
        final String linkdupeid = v_rest + "_%S_%S";
        final String[] availablequalities = availablequalitiestext.split(",");
        for (final String quality : availablequalities) {
            final String finalfilename = title + "_" + getNiceQuality(quality) + ".mp4";
            final String directlink = "http://media.ndr.de/progressive/" + v_year + "/" + v_id + "/TV-" + v_year + v_id + v_rest + "." + quality + ".mp4";
            final DownloadLink dl = createDownloadlink("http://ndrdecrypted.de/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
            try {
                dl.setContentUrl(parameter);
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
                dl.setBrowserUrl(parameter);
            }
            dl.setProperty("directlink", directlink);
            dl.setProperty("quality", quality);
            dl.setProperty("streamingType", "video");
            dl.setProperty("decryptedfilename", finalfilename);
            dl.setProperty("mainlink", parameter);
            try {
                dl.setLinkID(String.format(linkdupeid, "video", quality));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
                dl.setLinkID(String.format(linkdupeid, "video", quality));
            }
            dl.setFinalFileName(finalfilename);
            dl.setAvailable(true);
            foundqualities.put(quality, dl);
        }
        /** Decrypt qualities, selected by the user */
        final ArrayList<String> selectedQualities = new ArrayList<String>();
        final SubConfiguration cfg = SubConfiguration.getConfig(domain);
        final boolean qsubtitles = cfg.getBooleanProperty(Q_SUBTITLES, false);
        if (cfg.getBooleanProperty(Q_BEST, false)) {
            for (final String quality : qualities) {
                if (foundqualities.get(quality) != null) {
                    selectedQualities.add(quality);
                    break;
                }
            }
        } else {
            /** User selected nothing -> Decrypt everything */
            boolean qveryhigh = cfg.getBooleanProperty(Q_VERYHIGH, false);
            boolean qhigh = cfg.getBooleanProperty(Q_HIGH, false);
            boolean qlow = cfg.getBooleanProperty(Q_LOW, false);
            if (qveryhigh == false && qhigh == false && qlow == false) {
                qveryhigh = true;
                qhigh = true;
                qlow = true;
            }

            if (qveryhigh) {
                selectedQualities.add("hq");
            }
            if (qhigh) {
                selectedQualities.add("hi");
            }
            if (qlow) {
                selectedQualities.add("lo");
            }
        }
        /* Add chosen downloadlinks, add subtitle if desired. */
        for (final String selectedQualityValue : selectedQualities) {
            final DownloadLink dl = foundqualities.get(selectedQualityValue);
            if (dl != null) {
                decryptedLinks.add(dl);
                if (subtitle_url != null && qsubtitles) {
                    final String quality = dl.getStringProperty("quality", null);
                    final String finalfilename = title + "_" + getNiceQuality(quality) + ".xml";
                    final DownloadLink dlsubtitle = createDownloadlink("http://ndrdecrypted.de/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
                    try {
                        dlsubtitle.setContentUrl(parameter);
                    } catch (final Throwable e) {
                        /* Not available in old 0.9.581 Stable */
                        dlsubtitle.setBrowserUrl(parameter);
                    }
                    dlsubtitle.setProperty("directlink", subtitle_url);
                    dlsubtitle.setProperty("quality", quality);
                    dlsubtitle.setProperty("streamingType", "subtitle");
                    dlsubtitle.setProperty("decryptedfilename", finalfilename);
                    dlsubtitle.setProperty("mainlink", parameter);
                    try {
                        dl.setLinkID(String.format(linkdupeid, "subtitle", quality));
                    } catch (final Throwable e) {
                        /* Not available in old 0.9.581 Stable */
                        dl.setLinkID(String.format(linkdupeid, "subtitle", quality));
                    }
                    dlsubtitle.setFinalFileName(finalfilename);
                    dlsubtitle.setAvailable(true);
                    decryptedLinks.add(dlsubtitle);
                }
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info(domain + ": None of the selected qualities were found, decrypting done...");
            return decryptedLinks;
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(title.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private String getNiceQuality(final String qual) {
        String nicequal;
        if (qual.equals("hq")) {
            nicequal = "VERYHIGH";
        } else if (qual.equals("hi")) {
            nicequal = "HIGH";
        } else {
            nicequal = "LOW";
        }
        return nicequal;
    }
}
