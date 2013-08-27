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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "trntbl.me" }, urls = { "http://(www\\.)?trntbl\\.me/[a-z0-9]+" }, flags = { 0 })
public class TrnTblMe extends PluginForDecrypt {

    public TrnTblMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final int TRACKSPERPAGE = 50;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        final String user = new Regex(parameter, "trntbl\\.me/(.+)").getMatch(0);
        br.getHeaders().put("Accept", "*/*");

        final DecimalFormat df = new DecimalFormat("0000");
        int offset = 0;
        int request = 0;
        boolean cont = true;
        while (cont == true) {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted for link: " + parameter);
                    return decryptedLinks;
                }
            } catch (final Throwable e) {
            }

            br.getPage("http://" + user + ".tumblr.com/api/read/json?callback=Request.JSONP.request_map.request_" + request + "&type=audio&start=" + offset + "&num=" + TRACKSPERPAGE + "&cache_bust=" + df.format(new Random().nextInt(1000)));
            br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
            final String listString = br.getRegex("type\":\"audio\",\"posts\":\\[(.*?)\\]\\}\\);").getMatch(0);
            final String[] info = listString.split("\\},");
            if (info == null || info.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleInfo : info) {
                String filename = null;
                String finallink = new Regex(singleInfo, "\\?audio_file=(http[^<>\"]*?)\"").getMatch(0);
                if (finallink == null) {
                    // Maybe no tumblr link
                    // Maybe soundcloud link
                    finallink = new Regex(singleInfo, "<iframe src=\"(http[^<>\"]*?)\" frameborder=").getMatch(0);
                    if (finallink != null) {
                        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(finallink.trim())));
                        continue;
                    }
                    // Maybe spotify link
                    finallink = new Regex(singleInfo, "class=\"spotify_audio_player\" src=\"(http[^<>\"]*?)\"").getMatch(0);
                    if (finallink != null) {
                        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(finallink.trim())));
                        continue;
                    }
                }
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                finallink = Encoding.htmlDecode(finallink.trim());
                final String postID = new Regex(finallink, "audio_file/[a-z0-9]+/(\\d+)/").getMatch(0);
                final String postLink = "http://" + user + ".tumblrdecrypted.com/post/" + postID;
                final String artist = new Regex(singleInfo, "\"id3\\-artist\":\"([^<>\"]*?)\"").getMatch(0);
                final String album = new Regex(singleInfo, "\"id3\\-album\":\"([^<>\"]*?)\"").getMatch(0);
                String title = new Regex(singleInfo, "\"id3\\-title\":\"([^<>\"]*?)\"").getMatch(0);
                if (title == null) title = new Regex(singleInfo, "\"slug\":\"([^<>\"]*?)\"").getMatch(0);
                if (title == null) title = postID;
                if (artist != null && album != null) {
                    filename = Encoding.htmlDecode(artist.trim()) + " - " + Encoding.htmlDecode(album.trim()) + " - " + Encoding.htmlDecode(title.trim());
                } else if (artist != null) {
                    filename = Encoding.htmlDecode(artist.trim()) + " - " + Encoding.htmlDecode(title) + " - ";
                } else {
                    filename = Encoding.htmlDecode(title.trim());
                }

                final DownloadLink dl = createDownloadlink(postLink);
                dl.setProperty("audiodirectlink", finallink);
                dl.setFinalFileName(filename + ".mp3");
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
            if (info.length == TRACKSPERPAGE) {
                offset += TRACKSPERPAGE;
                request++;
            } else {
                cont = false;
                break;
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(user);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }
}
