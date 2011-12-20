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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flickr.com" }, urls = { "http://(www\\.)?flickr\\.com/photos/[a-z0-9_\\-]+(/(\\d+|page\\d+|sets/\\d+))?" }, flags = { 0 })
public class FlickrCom extends PluginForDecrypt {

    public FlickrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (new Regex(parameter, "http://(www\\.)?flickr\\.com/photos/[a-z0-9_\\-]+/\\d+").matches()) {
            String filename = getFilename();
            if (br.containsHTML("(photo\\-div video\\-div|class=\"video\\-wrapper\")")) {
                final String lq = createGuid();
                final String secret = br.getRegex("photo_secret=(.*?)\\&").getMatch(0);
                final String nodeID = br.getRegex("data\\-comment\\-id=\"(\\d+\\-\\d+)\\-").getMatch(0);
                if (secret == null || nodeID == null || filename == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                br.getPage("http://www.flickr.com/video_playlist.gne?node_id=" + nodeID + "&tech=flash&mode=playlist&lq=" + lq + "&bitrate=700&secret=" + secret + "&rd=video.yahoo.com&noad=1");
                final Regex parts = br.getRegex("<STREAM APP=\"(http://.*?)\" FULLPATH=\"(/.*?)\"");
                final String part1 = parts.getMatch(0);
                final String part2 = parts.getMatch(1);
                if (part1 == null || part2 == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                filename += ".flv";
                DownloadLink finalDownloadlink = createDownloadlink(part1 + part2.replace("&amp;", "&"));
                finalDownloadlink.setFinalFileName(filename);
                decryptedLinks.add(finalDownloadlink);
            } else {
                DownloadLink finalDownloadlink = decryptSingleLink(parameter);
                if (finalDownloadlink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(finalDownloadlink);
            }
        } else {
            /** Handling for albums/sets */
            String[] links = br.getRegex("data\\-track=\"photo\\-click\" href=\"(/photos/[a-z0-9_\\-]+/\\d+)").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String singleLink : links) {
                DownloadLink finalDownloadlink = decryptSingleLink("http://www.flickr.com" + singleLink);
                if (finalDownloadlink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(finalDownloadlink);
            }
        }

        return decryptedLinks;
    }

    private String createGuid() {
        String a = "";
        final String b = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._";
        int c = 0;
        while (c < 22) {
            final int index = (int) Math.floor(Math.random() * b.length());
            a = a + b.substring(index, index + 1);
            c++;
        }
        return a;
    }

    /** Get single links and set nice filenames */
    private DownloadLink decryptSingleLink(String parameter) throws IOException {
        br.getPage(parameter + "/sizes/l/in/photostream/");
        String filename = getFilename();
        final Regex regex1 = br.getRegex("<div class=\"spaceball\" style=\"height:\\d+px; width: \\d+px;\"></div>[\t\n\r ]+<img src=\"(http://.*?)\"");
        final Regex regex2 = br.getRegex("\"(http://farm\\d+\\.static\\.flickr\\.com/\\d+/.*?)\"");
        String finallink = regex1.getMatch(0);
        if (finallink == null) {
            finallink = regex2.getMatch(0);
            if (finallink == null) {
                // Didn't work, try again...
                br.getPage(parameter + "/sizes/l/in/photostream/");
                finallink = regex1.getMatch(0);
                if (finallink == null) {
                    finallink = regex2.getMatch(0);
                }
            }
        }
        DownloadLink fina = null;
        if (finallink != null) {
            fina = createDownloadlink(finallink);
            final String ext = finallink.substring(finallink.lastIndexOf("."));
            if (ext != null && filename != null) filename = Encoding.htmlDecode(filename.trim()) + ext;
            fina.setFinalFileName(filename);
        }
        return fina;
    }

    private String getFilename() {
        String filename = br.getRegex("<meta name=\"title\" content=\"(.*?)\">").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"photo\\-title\">(.*?)</h1").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) \\| Flickr \\- (F|Ph)otosharing\\!</title>").getMatch(0);
            }
        }
        return filename;
    }
}
