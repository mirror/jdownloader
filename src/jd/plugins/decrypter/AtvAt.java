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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.ffmpeg.json.Stream;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "atv.at" }, urls = { "http://(www\\.)?atv\\.at/[a-z0-9\\-_]+/[a-z0-9\\-_]+/d\\d+/" }, flags = { 0 })
public class AtvAt extends PluginForDecrypt {

    public AtvAt(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * Important note: Via browser the videos are streamed via RTSP
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        final String fid = new Regex(parameter, "/(d\\d+)/$").getMatch(0);
        if (br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline (404 error): " + parameter);
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(fid);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (!br.containsHTML("class=\"jsb_ jsb_video/FlashPlayer\"")) {
            logger.info("There is no downloadable content: " + parameter);
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(fid);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (br.containsHTML("is_geo_ip_blocked\\&quot;:true")) {
            /*
             * We can get the direct links of geo blocked videos anyways - also, this variable only tells if a video is geo blocked at all -
             * this does not necessarily mean that it is blocked in the users'country!
             */
            logger.info("Video might not be available in your country: " + parameter);
        }
        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
        final String source = br.getRegex("<div class=\"jsb_ jsb_video/FlashPlayer\" data\\-jsb=\"(.*?)\">").getMatch(0);
        String name;
        final String[] allLinks = new Regex(source, "src\\&quot;:\\&quot;(http://[^<>\"]*?(index|playlist)\\.m3u8)\\&quot;}").getColumn(0);
        if (allLinks == null || allLinks.length == 0) {
            logger.info("Seems like the video source of the player is missing: " + parameter);
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(fid);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String episodeNr = br.getRegex("class=\"headline\">Folge (\\d+)</h4>").getMatch(0);
        if (episodeNr != null) {
            name = br.getRegex("class=\"title_bar\">[\t\n\r ]+<h1>([^<>\"]*?)</h1>").getMatch(0);
        } else {
            name = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        }
        if (name == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        name = Encoding.htmlDecode(name.trim());
        name = decodeUnicode(name);
        final DecimalFormat df = new DecimalFormat("000");
        final DecimalFormat episodeFormat = new DecimalFormat("00");

        int counter = 1;
        for (String singleLink : allLinks) {

            br.getPage(singleLink.replace("\\", ""));
            String quality = "360p";
            if (br.containsHTML("#EXT-X-STREAM-INF")) {
                for (String line : Regex.getLines(br.toString())) {
                    if (!line.startsWith("#")) {

                        DownloadLink link = createDownloadlink(br.getBaseURL() + line);

                        link.setContainerUrl(parameter);

                        try {
                            // try to get the video quality
                            HLSDownloader downloader = new HLSDownloader(link, br, link.getDownloadURL()) {
                                @Override
                                public LogSource initLogger(DownloadLink link) {
                                    return getLogger();
                                }
                            };
                            StreamInfo streamInfo = downloader.getProbe();
                            for (Stream s : streamInfo.getStreams()) {
                                if ("video".equals(s.getCodec_type())) {
                                    quality = s.getHeight() + "p";
                                    break;
                                }
                            }

                        } catch (Throwable e) {
                            getLogger().log(e);
                        }
                        StringBuilder finalName = new StringBuilder();
                        if (episodeNr != null) {
                            finalName.append(name + "_E" + episodeFormat.format(Integer.parseInt(episodeNr)));
                        } else {
                            finalName.append(name + "_part");
                        }
                        if (quality != null) {
                            finalName.append("_").append(quality);
                        }
                        quality = null;
                        String n = finalName.toString() + "_" + df.format(counter);

                        link.setFinalFileName(n + ".mp4");
                        decryptedLinks.add(link);
                        counter++;

                    }

                }
            }

        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(name);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}