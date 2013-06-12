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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

// Please do not mess with the following Regex!
// Do not use lazy regex. Make regex to support the features you need. Lazy regex will pick up false positives in other areas of the plugin.
// "old style" , "new style", "redirect url shorting service" 
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "promodj.com" }, urls = { "https?://((www\\.)?((([\\w\\-\\.]+\\.(djkolya\\.net|pdj\\.ru|promodeejay\\.(net|ru)|promodj\\.(ru|com)))|(djkolya\\.net|pdj\\.ru|promodeejay\\.(net|ru)|promodj\\.(ru|com))(/[\\w\\-\\.]+)?)/(?!top100|podsafe)(foto/(all|\\d+)/?(#(foto|full|list|biglist|middlelist)\\d+)?(\\d+(\\.html)?(#(foto|full|list|biglist|middlelist)\\d+)?)?|(acapellas|groups|mixes|prelisten|podcasts|promos|radioshows|realtones|remixes|samples|tracks|videos)/\\d+|prelisten_m3u/\\d+/[\\w]+\\.m3u|(download|source)/\\d+/[^\r\n\"'<>]+))|pdj\\.cc/\\w+)" }, flags = { 0 })
public class ProDjCm extends PluginForDecrypt {

    // DEV NOTES
    // other: Because they have so many domains, Please becareful with the regex, \\w can not be used twice either side of (sub.)?domains as
    // it effectively lets all site links match.
    // other: As of march 12 they redirect to promodj.com but it's too hard to rename prior to processing, as redirects do not necessarily
    // carry the same parameters.

    private static final String HOSTS = "(djkolya\\.net|pdj\\.(cc|ru)|promodeejay\\.(net|ru)|promodj\\.(ru|com))";

    /**
     * @author raztoki
     * */
    public ProDjCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* NOTE: no override to keep compatible to old stable */
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        HashSet<String> filter = new HashSet<String>();

        String parameter = param.toString();

        br.setCookiesExclusive(true);
        // this is needed! do not disable
        br.setFollowRedirects(true);

        // these types here need to be done before first page grab!! as they could be files prelisten links are direct links!
        if (parameter.matches(".+/prelisten/\\d+")) {
            handlePrelisten(decryptedLinks, filter, parameter);
        } else if (parameter.matches(".+/(download|source)/\\d+/.+")) {
            // downloads urls sometimes onsite or offsite
            decryptedLinks.add(createDownloadlink("directhttp://" + parameter));
        } else {
            // back to normal!
            br.getPage(parameter);
            if (br.containsHTML("(<title>404 &ndash;.*?</title>|<h1>Page not found :\\(</h1>)")) {
                if (parameter.contains("/download/")) {
                    DownloadLink link = createDownloadlink("directhttp://" + parameter);
                    link.setAvailable(false);
                    decryptedLinks.add(link);
                    logger.warning("Offline URL : " + parameter);
                } else {
                    logger.warning("Invalid URL: " + parameter);
                }
                return decryptedLinks;
            }

            // this is needed because /groups/ can contain all sorts of link types. This is instead of returning back to the plugin. Only
            // downside it reduces the threading ability, but positively controls connections all within one instance.
            passItOn(decryptedLinks, filter, parameter);
        }

        return decryptedLinks;
    }

    private void passItOn(ArrayList<DownloadLink> ret, HashSet<String> filter, String grabThis) throws IOException {
        String fpName = null;
        if (grabThis.matches("https?://pdj\\.cc/\\w+")) {
            // domain shorting services
            ret.add(createDownloadlink(br.getURL()));
        } else if (grabThis.matches(".+/prelisten/\\d+")) {
            handlePrelisten(ret, filter, grabThis);
        } else if (grabThis.matches(".+/(download|source)/\\d+/.+")) {
            // downloads urls sometimes onsite or offsite
            ret.add(createDownloadlink("directhttp://" + grabThis));
        } else if (grabThis.matches(".*/foto/(all#foto\\d+|\\d+(/\\d+(\\.html)?)?(#foto|full)\\d+)") && !grabThis.matches(".*foto#(list|biglist|middlelist)")) {
            // Single photos && not album type && not album type
            parseFoto(ret, filter, false, grabThis);
        } else if (grabThis.matches(".*(foto#(list|biglist|middlelist)|/foto/(all|\\d+)/?)")) {
            // Entire photo album
            // set fpName
            String fp1 = br.getRegex("&ndash; (.+)</title>").getMatch(0);
            // if (fp1 == null) fp1 = br.getRegex("").getMatch(0);
            String fp2 = br.getRegex("<meta name=\"title\" content=\"([^\"]+)").getMatch(0);
            if (fp2 == null) fp2 = br.getRegex("<title>(.+) &ndash;").getMatch(0);
            if (fp1 != null && fp2 != null)
                fpName = fp1 + " - " + fp2;
            else if (fp2 != null) fpName = fp2;
            parseFoto(ret, filter, true, grabThis);
        } else if (grabThis != null && grabThis.matches(".*/(acapellas|mixes|podcasts|promos|radioshows|realtones|remixes|samples|tracks|videos)/\\d+")) {
            // grab all the provided download stuff here
            parseDownload(ret, filter, grabThis);
        } else if (grabThis.matches(".*/groups/\\d+")) {
            // find groups data and export it.

            // prevent itself from itself!
            filter.add(grabThis);

            String frame = br.getRegex("(<div class=\"dj_bblock\">.*<h2>.*</div>[\r\n ]+</div>[\r\n ]+</div>)").getMatch(0);
            String fp1 = br.getRegex("&ndash; (.*?)</title>").getMatch(0);
            String fp2 = new Regex(frame, ">([^<]+)</span>[\r\n ]+</h2>").getMatch(0);
            if (fp1 != null || fp2 != null)
                fpName = fp1 + " - " + fp2;
            else if (fp1 == null) fpName = fp2;

            String[] posts = new Regex(frame, this.getSupportedLinks()).getColumn(0);
            if (frame == null || posts == null) {
                logger.warning("/groups/ issue, Please report this issue to JDownloader Deveolopment Team!" + grabThis);
                return;
            }
            if (posts != null && posts.length != 0) {
                for (String link : posts) {
                    if (filter.add(link) == false) continue;
                    if (!link.matches("https?://")) link = new Regex(br.getURL(), "(https?://)").getMatch(0) + link;
                    if (!link.matches(".+/(download|source|prelisten_m3u|prelisten)/\\d+/.+")) br.getPage(link);
                    passItOn(ret, filter, link);
                }
            }
        } else if (grabThis.matches(".+/prelisten_m3u/.+")) {
            // worth supporting
            br.setFollowRedirects(false);
            if (!grabThis.contains(br.getURL())) br.getPage(grabThis);
            String dllink = br.getRedirectLocation();
            br.setFollowRedirects(true);
            if (br.containsHTML("<title>404</title>")) return;
            if (dllink == null) dllink = br.getRegex("#EXTINF:\\-1,[^<>\"/]+(http[^<>\"]*?\\.mp3)").getMatch(0);
            if (dllink == null) {
                logger.warning("Decrypter broken for link: " + grabThis);
                return;
            }
            if (filter.add(dllink) == false) return;
            final DownloadLink dl = createDownloadlink("directhttp://" + dllink);
            ret.add(dl);
        }

        if (fpName != null) {
            fpName = fpName.replaceAll("\\&quot\\;", "'");
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.setProperty("ALLOW_MERGE", true);
            fp.addLinks(ret);
        }
    }

    private void parseDownload(ArrayList<DownloadLink> ret, HashSet<String> filter, String grabThis) {
        ArrayList<String[]> customHeaders = new ArrayList<String[]>();
        ArrayList<String> linksFound = new ArrayList<String>();

        String dllink = br.getRegex("<a class=\"bigload1\" promenade=\"\\d+\" href=\"(https?://" + HOSTS + "/download/\\d+/[^\"<>]+)").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("<a id=\"download_flasher\" href=\"(https?://" + HOSTS + "/download/\\d+/[^\"<>]+)").getMatch(0);
        }
        // give the ability to return multiple formats audio sections...
        if (grabThis.matches(".*/(acapellas|mixes|podcasts|promos|radioshows|realtones|remixes|samples|tracks)/\\d+")) {
            if (dllink != null) linksFound.add(dllink);
            if (dllink == null || dllink.endsWith(".mp3")) {
                // lets look for wav!
                dllink = br.getRegex("href=\"(https?://" + HOSTS + "/(source|download)/[^\"]+\\.wav)\"").getMatch(0);
                if (dllink != null) linksFound.add(dllink);
            }
        } else if (dllink == null && grabThis.contains("/videos/")) {
            // this type seems to have advertised links escaped but not always /download/able! Need to switch to alternative method.
            String holder = br.getRegex("swf.addVariable\\('jsonText', '(.*?)\\);").getMatch(0);
            if (holder == null) {
                logger.warning("parseDownload issue, with finding dllink. Please report this issue to JDownloader Development Team! " + grabThis);
                return;
            } else {
                holder = Encoding.urlDecode(holder, false).replaceAll("\\\\/", "/");
                dllink = new Regex(holder, "\"play\":\\{\"@url\":\"(https?://[^\"]+)").getMatch(0);
                if (dllink != null) {
                    // lets add current dllink to the HashSet because the finallink is actually dynamically created each time you request.
                    if (filter.add(dllink) == true) {
                        linksFound.add(dllink);
                        try {
                            // like apple trailers they have the final url inside so called video...
                            Browser br2 = br.cloneBrowser();
                            URLConnectionAdapter con = br2.openGetConnection(dllink);
                            long test = con.getContentLength();
                            if (con.getContentType().contains("video/") && test < 51200) {
                                br2.followConnection();
                                dllink = br2.getRegex("URL=(http[^\r\n\t ]+)").getMatch(0);
                            }
                            con.disconnect();
                        } catch (Exception e) {
                            dllink = null;
                        }

                        // the following is not really needed.. though might be good to send it anyway.
                        customHeaders.add(new String[] { "Referer", br.getURL() });
                        customHeaders.add(new String[] { "Accept", "*/*" });
                        customHeaders.add(new String[] { "Accept-Encoding", "gzip, deflate" });
                        customHeaders.add(new String[] { "Accept-Charset", null });
                        customHeaders.add(new String[] { "Cache-Control", null });
                        customHeaders.add(new String[] { "Pragma", null });
                    }
                }
            }
        } else if (dllink == null && grabThis.contains("/promos/")) {
            String holder = br.getRegex("(\\{\"seekAny\".*\"\\}\\);)").getMatch(0);
            if (holder != null) {
                if (holder.contains("\"downloadable\":true")) {
                    dllink = new Regex(holder, "downloadURL\":\"(https?:\\\\/\\\\/" + HOSTS + "\\\\/download\\\\/\\d+\\\\/[^\"<>]+)").getMatch(0);
                    if (dllink != null) {
                        dllink = dllink.replaceAll("\\\\/", "/");
                    }
                } else {
                    // lets return prelisten and amend _m3u
                    String prelisten = new Regex(holder, "URL\":\"(https?[^\"]+)").getMatch(0);
                    if (prelisten != null) {
                        dllink = prelisten.replaceAll("\\\\/", "/").replace("/prelisten/", "/prelisten_m3u/");
                    }
                }
            } else {
                logger.warning("parseDownload issue, with finding dllink. Please report this issue to JDownloader Development Team! " + grabThis);
                return;
            }
        }
        // easier doing this here once than multiple times.
        if (linksFound.isEmpty()) linksFound.add(dllink);

        for (String link : linksFound) {
            if (filter.add(link) == true) {
                DownloadLink dl = createDownloadlink(link);
                if (customHeaders.size() != 0) dl.setProperty("customHeader", customHeaders);
                ret.add(dl);
            }
        }
    }

    private void parseFoto(ArrayList<DownloadLink> ret, HashSet<String> filter, boolean album, String grabThis) throws IOException {
        String fuid = null;
        ArrayList<String> imgsArray = new ArrayList<String>();

        if (!album) {
            // place this first, then use null for each possible combination album link but with tag to photo requested.
            fuid = new Regex(grabThis, "#(foto|full)(\\d+)").getMatch(1);
            if (fuid == null) fuid = new Regex(grabThis, "/foto/\\d+/(\\d+)").getMatch(0);
            String single = br.getRegex("(\\{[\r\n\t ]+fotoID: ?" + fuid + "[^\\}]+)").getMatch(0);
            imgsArray.add(single);
        } else {
            // all images are now found within first page grab within an array!
            String[] imgArray = br.getRegex("(\\{[\r\n\t ]+fotoID: \\d+[^\\}]+)").getColumn(0);
            if (imgArray != null && imgArray.length != 0) {
                imgsArray.addAll(Arrays.asList(imgArray));
            }
        }

        for (String result : imgsArray) {
            if (fuid == null) fuid = new Regex(result, "link: '.+/foto/\\d+/(\\d+)").getMatch(0);
            // Original is best!
            String[] bestImg = new Regex(result, "originalURL: ?'(http://[^\\']+(\\.[a-z]+))").getRow(0);
            if (bestImg == null) {
                bestImg = new Regex(result, "bigURL: ?'(http://[^\\']+(\\.[a-z]+))").getRow(0);
            }
            String fileName = null;
            String title = new Regex(result, "title: '(.+)',").getMatch(0);
            if (filter.add(bestImg[0]) == false) continue;
            DownloadLink link = createDownloadlink("directhttp://" + bestImg[0].replace("/labeled/", "/"));
            if (title != null) fileName = fuid + " - " + title + bestImg[1];
            // not all fotos have a title/name
            if (title == null || title.equals("")) fileName = fuid + bestImg[1];
            link.setFinalFileName(Encoding.htmlDecode(fileName.trim()));
            if (album) link.setAvailable(true);
            ret.add(link);
            fuid = null;
        }
    }

    private void handlePrelisten(ArrayList<DownloadLink> ret, HashSet<String> filter, String grabThis) {
        // dl wont start unless you have trailing /
        grabThis += "/";
        DownloadLink link = createDownloadlink("directhttp://" + grabThis);
        try {
            Browser br2 = br.cloneBrowser();
            URLConnectionAdapter con = br2.openGetConnection(grabThis);
            if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                link.setAvailable(false);
            } else {
                link.setFinalFileName(Plugin.getFileNameFromHeader(con));
                link.setDownloadSize(con.getLongContentLength());
                link.setAvailable(true);
                // links seem to generate each time you hit downloadlink! I will assume short time to live!
                // link.setUrlDownload(br2.getURL());
            }
            con.disconnect();
        } catch (Exception e) {
            link.setAvailable(false);
        }
        ret.add(link);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}