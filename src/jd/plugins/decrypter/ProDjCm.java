//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

// "old style" , "new style", "redirect url shorting service" 
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "promodj.com" }, urls = { "https?://(([\\w\\-]+\\.)?(djkolya\\.net|pdj\\.ru|promodeejay\\.(net|ru)|promodj\\.(ru|com))/[\\w\\-/]+|(www\\.)?pdj\\.cc/\\w+)" }, flags = { 0 })
public class ProDjCm extends PluginForDecrypt {

    // DEV NOTES - by raztoki
    // other: Because they have so many domains, Please becareful with the
    // regex, \\w can not be used twice either side of (sub.)?domains as it
    // effectively lets all site links match.
    // other: As of march 12 they redirect to promodj.com but it's too hard to
    // rename prior to processing, as redirects do not necessarily carry the
    // same parameters.

    private static final String HOSTS = "(djkolya\\.net|pdj\\.(cc|ru)|promodeejay\\.(net|ru)|promodj\\.(ru|com))";

    public ProDjCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* NOTE: no override to keep compatible to old stable */
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookiesExclusive(true);
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.containsHTML("(<title>404 \\&ndash; Инфинити</title>|<h1>Page not found :\\(</h1>)")) {
            logger.warning("Invalid URL: " + parameter);
            return decryptedLinks;
        }
        if (parameter.matches(".*")) {
            // this is needed because /groups/ can contain all sorts of link
            // types. This is instead of returning back to the plugin. Only
            // downside it reduces the threading ability, but positively
            // controls connections all within one instance.
            passItOn(decryptedLinks, parameter, null);
        }
        return decryptedLinks;
    }

    private void passItOn(ArrayList<DownloadLink> ret, String parameter, String grabThis) throws IOException {
        String fpName = null;
        // domain shorting services first, then change parameter as it saves
        // reloading pages, this could be anything, just return to decrypter for
        // second round.
        if (parameter.matches("https?://pdj\\.cc/\\w+")) {
            ret.add(createDownloadlink(br.getURL()));
        }
        // downloads urls sometimes onsite or offsite
        else if (parameter.matches("https?://([\\w\\-]+\\.)?" + HOSTS + "/download/\\d+/.+")) {
            ret.add(createDownloadlink("directhttp://" + parameter));
        }
        // Single photos && not album type && not album type
        else if (parameter.matches(".*/foto/(all#foto\\d+|\\d+(/\\d+(\\.html)?)?(#foto|full)\\d+)") && !parameter.matches(".*foto#(list|biglist|middlelist)")) {
            parseSingleFoto(ret, parameter, null);
        }
        // Entire photo albums
        else if (parameter.matches(".*(foto#(list|biglist|middlelist)|/foto/(all|\\d+)/?)")) {
            // set fpName
            String fp1 = br.getRegex("\\&ndash; (.+)</title>").getMatch(0);
            if (fp1 == null) fp1 = br.getRegex("").getMatch(0);
            String fp2 = br.getRegex("<meta name=\"title\" content=\"([^\"]+)").getMatch(0);
            if (fp2 == null) fp2 = br.getRegex("<title>(.+) \\&ndash;").getMatch(0);
            if (fp1 != null && fp2 != null)
                fpName = fp1 + " - " + fp2;
            else if (fp2 != null) fpName = fp2;
            // grab the 'link' for each image
            String[] albumLinks = br.getRegex("link: '(https?://([\\w\\-]+)?" + HOSTS + "(/[\\w\\-]+)?/foto/[^']+)").getColumn(0);
            if (albumLinks == null) {
                logger.warning("albumLinks can not be found, Please report this issue to JDownloader Deveolopment Team! " + parameter);
                return;
            }
            if (albumLinks != null && albumLinks.length != 0) {
                for (String link : albumLinks) {
                    parseSingleFoto(ret, parameter, link);
                }
            }
        }
        // grab all the provided download stuff here
        else if (grabThis != null && grabThis.matches(".*/(acapellas|mixes|podcasts|radioshows|realtones|remixes|samples|tracks|videos)/\\d+")) {
            if (br.getURL() != grabThis) br.getPage(grabThis);
            parseDownload(ret, parameter);
        }
        // repeated for grabThis
        else if (grabThis == null && parameter.matches(".*/(acapellas|mixes|podcasts|radioshows|realtones|remixes|samples|tracks|videos)/\\d+")) {
            if (br.getURL() != parameter) br.getPage(parameter);
            parseDownload(ret, parameter);
        }
        // find groups data and export it.
        else if (parameter.matches(".*/groups/\\d+")) {
            HashSet<String> filter = new HashSet<String>();
            String frame = br.getRegex("(<div class=\"dj_bblock\">.*</div>[\r\n ]+</div>)").getMatch(0);
            String fp1 = br.getRegex("\\&ndash; (.*?)</title>").getMatch(0);
            String fp2 = new Regex(frame, ">([^<]+)</span>[\r\n ]+</h2>").getMatch(0);
            if (fp1 != null || fp2 != null)
                fpName = fp1 + " - " + fp2;
            else if (fp1 == null) fpName = fp2;

            String[] posts = new Regex(frame, this.getSupportedLinks()).getColumn(0);
            if (frame == null || posts == null) {
                logger.warning("/groups/ issue, Please report this issue to JDownloader Deveolopment Team!" + parameter);
                return;
            }
            if (posts != null && posts.length != 0) {
                for (String link : posts) {
                    if (filter.add(link) == false) continue;
                    if (!link.matches("https?://")) link = new Regex(br.getURL(), "(https?://)").getMatch(0) + link;
                    passItOn(ret, parameter, link);
                }
            }
        } else {
            final String filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            String dllink = br.getRegex("\"(http://promodj\\.com/prelisten[^<>\"]*?)\"").getMatch(0);
            if (dllink == null && filename != null) {
                logger.info("No downloadable content available for link: " + parameter);
                return;
            }
            if (dllink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return;
            }
            br.setFollowRedirects(false);
            br.getPage(dllink + "?hq=1");
            dllink = br.getRedirectLocation();
            if (dllink == null) dllink = br.getRegex("#EXTINF:\\-1,[^<>\"/]+(http[^<>\"]*?\\.mp3)").getMatch(0);
            if (dllink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + dllink);
            if (filename != null) dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp3");
            ret.add(dl);

        }
        if (fpName != null) {
            fpName = fpName.replaceAll("\\&quot\\;", "'");
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
        }
    }

    private void parseDownload(ArrayList<DownloadLink> ret, String parameter) {
        String dllink = br.getRegex("<a class=\"bigload1\" promenade=\"\\d+\" href=\"(https?://" + HOSTS + "/download/\\d+/[^\"<>]+)").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("<a id=\"download_flasher\" href=\"(https?://" + HOSTS + "/download/\\d+/[^\"<>]+)").getMatch(0);
            if (dllink == null) {
                logger.warning("parseDownload issue, with finding dllink. Please report this issue to JDownloader Development Team! " + parameter);
                return;
            }
        }
        ret.add(createDownloadlink("directhttp://" + dllink));
    }

    private void parseSingleFoto(ArrayList<DownloadLink> ret, String parameter, String grabThis) throws IOException {
        String fid = null;
        if (grabThis == null) {
            // place this first, then use null for each possible combination
            // album link but with tag to photo requested.
            fid = new Regex(parameter, "#(foto|full)(\\d+)").getMatch(1);
            // standard url structure /foto/albumid/fid
            if (fid == null) fid = new Regex(grabThis, "/foto/\\d+/(\\d+)").getMatch(0);
        }
        // repeated here for grabThis
        else {
            if (br.getURL() != grabThis) br.getPage(grabThis);
            fid = new Regex(grabThis, "#(foto|full)(\\d+)").getMatch(1);
            if (fid == null) fid = new Regex(grabThis, "/foto/\\d+/(\\d+)").getMatch(0);
        }
        // find the correct image within the image array.
        String ImgsInArray = br.getRegex("(\\{[\r\n\t ]+fotoID: ?" + fid + "[^\\}]+)").getMatch(0);
        // these two images are identical! Just with different filename. I
        // assume 'best image' is always available.
        String[][] unformattedSource = new Regex(ImgsInArray, "bigURL: ?'(http://[^\\']+(\\.[a-z]+))").getMatches();
        if (unformattedSource == null) {
            unformattedSource = new Regex(ImgsInArray, "originalURL: ?'(http://[^\\']+(\\.[a-z]+))").getMatches();
        }
        if (fid == null || ImgsInArray == null || unformattedSource == null) {
            logger.warning("parseSingleFoto issue, Please report this issue to JDownloader Development Team! " + parameter);
            return;
        }
        String fileName = null;
        String source = unformattedSource[0][0];
        String extension = unformattedSource[0][1];
        String title = new Regex(ImgsInArray, "title: '(.+)',").getMatch(0);
        DownloadLink link = createDownloadlink("directhttp://" + source);
        if (title != null) fileName = fid + " - " + title + extension;
        // not all fotos have a title/name
        if (title == null || title.equals("")) fileName = fid + extension;
        link.setFinalFileName(Encoding.htmlDecode(fileName.trim()));
        ret.add(link);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}