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
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;

import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ero-video.net" }, urls = { "https?://(?:www\\.)?ero\\-video\\.net/movie/\\?mcd=[A-Za-z0-9]+" }) public class EroVideoNet extends PornEmbedParser {

    private static AtomicReference<String> DELIMITER = new AtomicReference<String>(null);

    public EroVideoNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        this.br.setCookiesExclusive(true);
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        if (filename == null) {
            filename = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
        } else {
            filename = filename.replace("- Movie - Free Porn Video sharing site ero-video.net", "").trim();
        }
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (decryptedLinks == null || decryptedLinks.isEmpty()) {
            final String externID = this.br.getRegex("<a href=\"(http[^<>\"]+)\"[^>]+>[\t\n\r ]*?<i[^<>]+></i>Original URL</a>").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(this.createDownloadlink(externID));
            }
            String delimiter = DELIMITER.get();
            if (delimiter == null) {
                synchronized (DELIMITER) {
                    delimiter = DELIMITER.get();
                    if (delimiter == null) {
                        final byte[] evplayer = new jd.utils.SWFDecompressor().decompress("http://ero-video.net/swf/evplayer.swf");
                        final String ret = new Regex(new String(evplayer, "UTF-8"), "delimiter(.*?)").getMatch(0);
                        if (StringUtils.isEmpty(ret)) {
                            delimiter = "";
                        } else {
                            delimiter = ret;
                        }
                        DELIMITER.set(delimiter);
                    }
                }
            }
            if (!StringUtils.isEmpty(delimiter)) {
                final String player = this.br.getRegex("evplayer\\(\"(.*?)\"").getMatch(0);
                if (player != null) {
                    final String mcd = new Regex(player, "mcd=(.*?)(&|$)").getMatch(0);
                    final String pt = new Regex(player, "pt=(.*?)(&|$)").getMatch(0);
                    if (mcd != null && pt != null) {
                        final String st = st(delimiter, mcd, pt);
                        br.getPage("http://ero-video.net/movie.xml?mcd=" + mcd + "&pt=" + pt + "&st=&st=" + st);
                        final String movieURLs[][] = br.getRegex("type=\"(.*?)\"\\s*size=\"(\\d+)\">(https?://.*?)<").getMatches();
                        if (movieURLs == null || movieURLs.length == 0) {
                            DELIMITER.set("");
                        } else {
                            for (final String movieURL[] : movieURLs) {
                                final DownloadLink link = createDownloadlink("directhttp://" + movieURL[2]);
                                link.setFinalFileName(filename + "_" + movieURL[0] + Plugin.getFileNameExtensionFromURL(movieURL[2]));
                                link.setContentUrl(parameter);
                                decryptedLinks.add(link);
                            }
                        }
                    }
                }
            }
        }
        return decryptedLinks;
    }

    private String st(String delimiter, String mcd, String pt) {
        return Hash.getMD5(delimiter + mcd + pt.substring(0, 8));
    }
}
