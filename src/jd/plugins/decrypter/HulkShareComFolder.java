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

import java.math.BigDecimal;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hulkshare.com" }, urls = { "http://(www\\.)?(hulkshare\\.com|hu\\.lk)/[A-Za-z0-9_\\-]+(/[^<>\"/]+)?" }, flags = { 0 })
public class HulkShareComFolder extends PluginForDecrypt {

    public HulkShareComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String HULKSHAREDOWNLOADLINK = "http://(www\\.)?(hulkshare\\.com|hu\\/lk)/([a-z0-9]{12})";
    private static final String TYPE_SECONDSINGLELINK = "http://(www\\.)?(hulkshare\\.com|hu\\.lk)/[a-z0-9]+/[^<>\"/]+";
    private static final String TYPE_PLAYLIST         = "http://(www\\.)?(hulkshare\\.com|hu\\.lk)/playlist/\\d+(.+)?";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replaceFirst("hu\\.lk/", "hulkshare\\.com/");
        final String fuid = getUID(parameter);
        if (parameter.matches("https?://(www\\.)?(hulkshare\\.com|hu\\/lk)/(dl/|static|browse|images|terms|contact|audible|search|people|upload|featured|mobile|group|explore).*?")) {
            logger.info("Invalid link: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        br.setCookie("http://hulkshare.com/", "lang", "english");
        try {
            // They can have huge pages, allow double of the normal load limit
            br.setLoadLimit(4194304);
        } catch (final Throwable e) {
            // Not available in old 0.9.581 Stable
        }
        br.getPage(parameter);
        if (br.getHttpConnection().getContentType().equals("text/javascript") || br.getHttpConnection().getContentType().equals("text/css")) {
            logger.info("Invalid link: " + parameter);
            return decryptedLinks;
        }
        String argh = br.getRedirectLocation();
        if (br.containsHTML("class=\"bigDownloadBtn") || br.containsHTML(">The owner of this file doesn\\'t allow downloading") || argh != null) {
            logger.info("Link offline: " + parameter);
            decryptedLinks.add(createDownloadlink(parameter.replace("hulkshare.com/", "hulksharedecrypted.com/")));
            return decryptedLinks;
        }
        if (br.containsHTML("You have reached the download\\-limit")) {
            logger.info("Link offline: " + parameter);
            final DownloadLink dl = createDownloadlink(parameter.replace("hulkshare.com/", "hulksharedecrypted.com/"));
            dl.setAvailable(false);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (br.containsHTML(">Page not found") || br.containsHTML(">This file has been subject to a DMCA notice") || br.containsHTML("<h2>Error</h2>") || br.containsHTML(">We\\'re sorry but this page is not accessible") || br.containsHTML(">Error<")) {
            logger.info("Link offline: " + parameter);
            final DownloadLink dl = createDownloadlink(parameter.replace("hulkshare.com/", "hulksharedecrypted.com/"));
            dl.setProperty("fileoffline", true);
            dl.setAvailable(false);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        // Mainpage
        if (br.containsHTML("<title>Online Music, Free Internet Radio, Discover Artists \\- Hulkshare")) {
            logger.info("Link offline: " + parameter);
            final DownloadLink dl = createDownloadlink(parameter.replace("hulkshare.com/", "hulksharedecrypted.com/"));
            dl.setProperty("fileoffline", true);
            dl.setAvailable(false);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (br.containsHTML("class=\"nhsUploadLink signupPopLink\">Sign up for Hulkshare<")) {
            logger.info("Link doesn't contain any downloadable content: " + parameter);
            final DownloadLink dl = createDownloadlink(parameter.replace("hulkshare.com/", "hulksharedecrypted.com/"));
            dl.setProperty("fileoffline", true);
            dl.setAvailable(false);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (parameter.matches(TYPE_PLAYLIST)) {
            String fpName = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (fpName == null) {
                fpName = "hulkshare.com playlist - " + new Regex(parameter, "(\\d+)$").getMatch(0);
            }
            final String pllist = br.getRegex("class=\"newPlayer\" id=\"hsPlayer[A-Za-z0-9\\-_]+\" pid=\"\\d+\" rel=\"([^<>\"]*?)\"").getMatch(0);
            if (pllist == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String[] playlistids = pllist.split(",");
            for (final String pllistid : playlistids) {
                decryptedLinks.add(createDownloadlink("http://hulksharedecrypted.com/playlistsong/" + pllistid));
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
            return decryptedLinks;
        } else if (parameter.matches(TYPE_SECONDSINGLELINK)) {
            final String longLink = br.getRegex("longLink = \\'(http://(www\\.)?hulkshare\\.com/[a-z0-9]{12})\\'").getMatch(0);
            if (longLink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(longLink.replace("hulkshare.com/", "hulksharedecrypted.com/")));
            return decryptedLinks;
        }
        final String uid = br.getRegex("\\?uid=(\\d+)").getMatch(0);
        if (uid == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final int count_tracks = Integer.parseInt(br.getRegex(">All Music <i>(\\d+)</i>").getMatch(0));
        if (count_tracks == 0) {
            final DownloadLink dl = createDownloadlink("directhttp://" + parameter);
            dl.setProperty("offline", true);
            dl.setAvailable(false);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        final int entries_per_page = 25;
        final BigDecimal bd = new BigDecimal((double) count_tracks / entries_per_page);
        final int max_loads = bd.setScale(0, BigDecimal.ROUND_UP).intValue();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(new Regex(parameter, "hulkshare\\.com/(.+)").getMatch(0));

        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        for (int i = 1; i <= max_loads; i++) {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted for link: " + parameter);
                    return decryptedLinks;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            logger.info("Decrypting page " + i + " of " + max_loads);
            String linktext;
            if (i == 1) {
                linktext = br.getRegex("<ul class=\"nhsBrowseItems\">(.*?</li>)[\t\n\r ]+</ul>").getMatch(0);
            } else {
                br.postPage("http://www.hulkshare.com/userPublic.php", "ajax_pagination=1&uid=" + uid + "&page=" + i + "&fav=0&isvid=0&fld_id=0&per_page=" + entries_per_page + "&up=0&is_following=1&type=music&last_create_from_previous_list=");
                br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                linktext = br.getRegex("\"html\":\"(.+)\\}$").getMatch(0);
            }
            if (linktext == null) {
                // do not return null; as this could be a false positive - probably we simply decrypted everything possible
                break;
            }
            final String[] linkinfo = linktext.split("class=\"nhsBrowseBlock\"");
            if (linkinfo == null || linkinfo.length == 0) {
                // do not return null; as this could be a false positive - probably we simply decrypted everything possible
                break;
            }
            int added_links_count = 0;
            for (String slinkinfo : linkinfo) {
                final String fcode = new Regex(slinkinfo, "id=\"filecode\\-([a-z0-9]{12})\"").getMatch(0);
                if (fcode != null) {
                    if (fcode.equals(fuid)) {
                        continue;
                    }
                    final Regex more_info = new Regex(slinkinfo, "class=\"nhsTrackTitle nhsClear\" href=\"(http://[^<>\"]*?)\">([^<>\"]*?)</a>");
                    final String trackname = more_info.getMatch(1);
                    final DownloadLink fina = createDownloadlink("http://hulksharedecrypted.com/" + fcode);
                    if (trackname != null) {
                        fina.setName(Encoding.htmlDecode(trackname.trim()) + ".mp3");
                        fina.setAvailable(true);
                    }
                    fina._setFilePackage(fp);
                    fina.setProperty("LINKDUPEID", "hulksharecom_" + fcode);
                    try {
                        distribute(fina);
                    } catch (final Throwable e) {
                        // Not available in old 0.9.581 Stable
                    }
                    decryptedLinks.add(fina);
                    added_links_count++;
                }
            }
            if (added_links_count != entries_per_page) {
                logger.info("We got less links than entries_per_page --> Probably we're done - stopping");
                break;
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Possible Plugin Defect, please confirm in your browser. If there are files present within '" + parameter + "' please report this issue to JDownloader Development Team!");
            return null;
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private String getUID(String s) {
        if (s == null) {
            return null;
        }
        return new Regex(s, HULKSHAREDOWNLOADLINK).getMatch(2);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}