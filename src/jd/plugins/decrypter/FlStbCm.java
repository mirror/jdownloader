//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filestube.com" }, urls = { "http://(www\\.)?(filestube\\.(com|to)/([^<>/\"]+\\.html|[A-Za-z0-9]{10,})|video\\.filestube\\.com/(watch,[a-z0-9]+/.+\\.html|[a-z0-9]+))" }, flags = { 0 })
public class FlStbCm extends PluginForDecrypt {

    public FlStbCm(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    private static final String INVALIDLINKS = "http://(www\\.)?filestube\\.(com|to)/(source|advanced_search\\.html|search|look_for\\.html.+|sponsored_go\\.html|account|about\\.html|alerts/|api\\.html|contact\\.html|dmca\\.html|feedback|privacy\\.html|terms\\.html|trends/|last_added_files\\.html|add_contact\\.html|apidoc\\.html|submit\\.html|query\\.html|affiliation\\.html|affiliation_clickrates\\.html|cookies_policy\\.html)";
    private static final String NICE_HOST    = "filestube.to";
    private static final String VIDEOLINK    = "http://(www\\.)?video\\.filestube\\.com/(watch,[a-z0-9]+/.+\\.html|[a-z0-9]+)";

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final FilePackage fp = FilePackage.getInstance();
        String parameter = param.toString();
        if (!parameter.matches(VIDEOLINK)) parameter = parameter.replace("filestube.com/", "filestube.to/");
        if (parameter.matches(INVALIDLINKS)) {
            final DownloadLink offline = createDownloadlink("directhttp://http://filestubeoffline.to/" + new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setName(new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        // Allows us to get age restricted videos
        br.setCookie("http://" + NICE_HOST, "adultChecked", "1");
        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            final DownloadLink offline = createDownloadlink("directhttp://http://filestubeoffline.to/" + new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setName(new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        if (br.getURL().contains("filestube.com/query.html")) {
            final DownloadLink offline = createDownloadlink("directhttp://http://filestubeoffline.to/" + new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setName(new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (br.containsHTML(">403 Forbidden<") || br.containsHTML("class=\"err404\"")) {
            final DownloadLink offline = createDownloadlink("directhttp://http://filestubeoffline.to/" + new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setName(new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (br.containsHTML("Results <span>1")) {
            final DownloadLink offline = createDownloadlink("directhttp://http://filestubeoffline.to/" + new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setName(new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (parameter.contains("/go.html")) {
            if (br.containsHTML("\\- File no longer available<")) {
                final DownloadLink offline = createDownloadlink("directhttp://http://filestubeoffline.to/" + new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
                offline.setAvailable(false);
                offline.setName(new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            String finallink = br.getRegex("<noframes> <br /> <a href=\"(.*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("<iframe style=\".*?\" src=\"(.*?)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else if (parameter.matches(VIDEOLINK)) {
            if (br.containsHTML("(>Error 404 video not found<|>Sorry, the video you requested does not exist|>This video has been removed)")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String externID = null;
            // Check if there was a redirect to another site
            if (!br.getURL().contains("filestube.com/")) {
                externID = br.getURL();
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            }
            externID = br.getRegex("name=\"src\" value=\"http://(www\\.)?youtube\\.com/(v/|watch\\?v=)([^<>\"\\'/\\&]+)(\\&|\")").getMatch(2);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://www.youtube.com/watch?v=" + externID));
                return decryptedLinks;
            }
            externID = br.getRegex("(https?://(www\\.)?dailymotion\\.com/(video/[a-z0-9\\-_]+|swf(/video)?/[a-zA-Z0-9]+))").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            }
            externID = br.getRegex("xvideos\\.com/embedframe/(\\d+)\"").getMatch(0);
            if (externID == null) externID = br.getRegex("\"http://(www\\.)?xvideos\\.com/sitevideos/flv_player_site.*?value=\"id_video=(\\d+)\"").getMatch(1);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://www.xvideos.com/video" + externID));
                return decryptedLinks;
            }
            externID = br.getRegex("metacafe\\.com/fplayer/(\\d+)/").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://www.metacafe.com/watch/" + externID + "/" + System.currentTimeMillis()));
                return decryptedLinks;
            }
            externID = br.getRegex("emb\\.slutload\\.com/([A-Za-z0-9]+)\"").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://slutload.com/watch/" + externID));
                return decryptedLinks;
            }
            externID = br.getRegex("123video\\.nl/123videoPlayer\\.swf\\?mediaSrc=(\\d+)\"").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://www.123video.nl/playvideos.asp?MovieID=" + externID));
                return decryptedLinks;
            }
            externID = br.getRegex("veoh\\.com/static/swf/webplayer/WebPlayer\\.swf\\?version=v[0-9\\.]+\\&amp;permalinkId=v([^<>\"/]*?)\\&").getMatch(0);
            if (externID == null) externID = br.getRegex("<embed src=\"http://(www\\.)?veoh\\.com/videodetails2\\.swf\\?permalinkId=v([^<>\"/]*?)\\&").getMatch(1);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://www.veoh.com/watch/v" + externID));
                return decryptedLinks;
            }
            externID = br.getRegex("redtube\\.com/player/\"><param name=\"FlashVars\" value=\"id=(\\d+)\\&").getMatch(0);
            if (externID == null) externID = br.getRegex("embed\\.redtube\\.com/player/\\?id=(\\d+)\\&").getMatch(0);
            if (externID != null) {
                DownloadLink dl = createDownloadlink("http://www.redtube.com/" + externID);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            externID = br.getRegex("pornhub\\.com/embed/(\\d+)").getMatch(0);
            if (externID == null) {
                externID = br.getRegex("pornhub\\.com/view_video\\.php\\?viewkey=(\\d+)").getMatch(0);
                if (externID == null) {
                    externID = br.getRegex("pornhub\\.com/embed_player\\.php\\?id=(\\d+)\"").getMatch(0);
                }
            }
            if (externID != null) {
                DownloadLink dl = createDownloadlink("http://www.pornhub.com/view_video.php?viewkey=" + externID);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            // Some errorhandling
            if (br.containsHTML(">Channel: <a href=\"/\\d+,\\d+,channel\\.html\"> pornhub</a>")) {
                logger.info("filestube: Pornhub link is broken: " + parameter);
                return decryptedLinks;
            }
            externID = br.getRegex("player\\.tnaflix\\.com/video/(\\d+)\"").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://www.tnaflix.com/cum-videos/" + System.currentTimeMillis() + "/video" + externID));
                return decryptedLinks;
            }
            // 2nd handling for tnaflix
            externID = br.getRegex("tnaflix\\.com/embedding_player/player_[^<>\"]+\\.swf.*?config=(embedding_feed\\.php\\?viewkey=[a-z0-9]+)").getMatch(0);
            if (externID != null) {
                br.getPage("http://www.tnaflix.com/embedding_player/" + externID);
                externID = br.getRegex("start_thumb>http://static\\.tnaflix\\.com/thumbs/[a-z0-9\\-_]+/[a-z0-9]+_(\\d+)l\\.jpg<").getMatch(0);
                if (externID != null) {
                    decryptedLinks.add(createDownloadlink("http://www.tnaflix.com/cum-videos/" + System.currentTimeMillis() + "/video" + externID));
                    return decryptedLinks;
                }
            }
            externID = br.getRegex("<embed src=\\'(http://(www\\.)?dalealplay\\.com/[^<>\"]*?)\\'").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            }
            externID = br.getRegex("webdata\\.vidz\\.com/demo/swf/FlashPlayerV2\\.swf\".*?flashvars=\"id_scene=(\\d+)").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://www.vidz.com/video/" + System.currentTimeMillis() + "/vidz_porn_videos/?s=" + externID));
                return decryptedLinks;
            }
            externID = br.getRegex("id=\"embedContent\" ><a href=\"(http://xhamster[^<>\"]*?)\"").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            }
            externID = br.getRegex("\"http://(www\\.)?dailymotion\\.com/swf/(\\d+)\"").getMatch(0);
            if (externID != null) {
                logger.info("Found unsupported dailymotion videolink, stopping...." + parameter);
                return decryptedLinks;
            }
            // Filename needed for all ids below here
            String filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (filename == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            filename = Encoding.htmlDecode(filename.trim());
            externID = br.getRegex("mediaservices\\.myspace\\.com/services/media/embed\\.aspx/m=(\\d+)").getMatch(0);
            if (externID != null) {
                br.getPage("http://mediaservices.myspace.com/services/rss.ashx?videoID=" + externID + "&type=video&el=");
                final String finallink = br.getRegex("<media:player url=\"(http://[^<>\"]*?)\"").getMatch(0);
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
                dl.setFinalFileName(filename + ".flv");
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            externID = br.getRegex("shufuni\\.com/Flash/.*?flashvars=\"VideoCode=(.*?)\"").getMatch(0);
            if (externID != null) {
                final DownloadLink dl = createDownloadlink("http://www.shufuni.com/handlers/FLVStreamingv2.ashx?videoCode=" + externID);
                dl.setFinalFileName(Encoding.htmlDecode(filename.trim()));
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            externID = br.getRegex("\"(http://video\\.fc2\\.com/flv2\\.swf\\?i=[A-Za-z0-9\\-]+)\\&").getMatch(0);
            if (externID != null) {
                final DownloadLink dl = createDownloadlink(externID);
                dl.setFinalFileName(Encoding.htmlDecode(filename.trim()));
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            externID = br.getRegex("ebaumsworld\\.com/player\\.swf\" allowScriptAccess=\"always\" flashvars=\"id1=(\\d+)\"").getMatch(0);
            if (externID != null) {
                br.getPage("http://www.ebaumsworld.com/video/player/" + externID + "?env=id1");
                externID = br.getRegex("<file>(http://[^<>\"]*?)</file>").getMatch(0);
                if (externID != null) {
                    final DownloadLink dl = createDownloadlink("directhttp://" + externID);
                    dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
                    decryptedLinks.add(dl);
                    return decryptedLinks;
                }
            }
            if (br.containsHTML("freeadultmedia\\.com/hosted/famplayer\\.swf\"")) {
                final String file = br.getRegex("<param name=\"FlashVars\" value=\"file=(.*?)\\&").getMatch(0);
                if (file == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                br.getPage("http://www.freeadultmedia.com/famconfig.xml");
                final String server = br.getRegex("<server>(.*?)</server>").getMatch(0);
                if (server == null || file == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final DownloadLink dl = createDownloadlink("directhttp://" + server + file);
                dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
                decryptedLinks.add(dl);
                return decryptedLinks;

            }
            if (br.containsHTML("api\\.kewego\\.com") || br.containsHTML("kewego\\.es/p/")) {
                final DownloadLink offline = createDownloadlink("directhttp://http://filestubeoffline.to/" + new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
                offline.setAvailable(false);
                offline.setName(new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            if (br.containsHTML("dailymotion\\.com/swf/")) {
                final DownloadLink offline = createDownloadlink("directhttp://http://filestubeoffline.to/" + new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
                offline.setAvailable(false);
                offline.setName(new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            if (br.containsHTML("megaporn\\.com/")) {
                final DownloadLink offline = createDownloadlink("directhttp://http://filestubeoffline.to/" + new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
                offline.setAvailable(false);
                offline.setName(new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        } else {
            br.setFollowRedirects(true);
            if (br.getURL().equals("http://www." + NICE_HOST + "/")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            // Invalid/offline link
            if (br.containsHTML("(> ?File no longer available ?<|>Error 404 \\- Requested)")) {
                final DownloadLink offline = createDownloadlink("directhttp://http://filestubeoffline.to/" + new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
                offline.setAvailable(false);
                offline.setName(new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            // Offline link
            if (br.containsHTML("No results containing all your search terms were found|Searches related to <span")) {
                final DownloadLink offline = createDownloadlink("directhttp://http://filestubeoffline.to/" + new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
                offline.setAvailable(false);
                offline.setName(new Regex(parameter, "filestube\\.to/(.+)").getMatch(0));
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            String fpName = br.getRegex("<title>(.*?)\\- Free Download").getMatch(0);
            // Hmm this plugin should always have a name with that mass of
            // alternative ways to get the name
            if (fpName == null) {
                fpName = br.getRegex("<h1 style=\"float:left;width:450px;font\\-size:19px;margin:0 0 15px;padding:0;\">([^<>\"]*?)</h1>").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("&quot;\\](.*?)\\[/url\\]\"").getMatch(0);
                }
            }
            final String pornLink = br.getRegex("class=\"gobut\" href=\"(http[^<>\"]*?)\"").getMatch(0);
            String pagePiece = br.getRegex(Pattern.compile("id=\"copy_paste_links\" style=\".*?\">(.*?)</pre>", Pattern.DOTALL)).getMatch(0);
            // Find IDs for alternative links
            String[][] alternativeLinks = br.getRegex("alternate_files\\.push\\(\\{key: \\'([a-z0-9]+)\\',token: \\'([a-z0-9]+)\\'\\}\\)").getMatches();
            if (pagePiece == null && pornLink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (pagePiece != null) {
                String temp[] = pagePiece.split("\r\n");
                if (temp == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                if (temp == null || temp.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (String data : temp)
                    decryptedLinks.add(createDownloadlink(data));
                // Disabled because server returns 503 error for alternative
                // links,
                // maybe this is completely broken/not available anymore
                final boolean enableAlternatives = false;
                if (enableAlternatives) {
                    if (alternativeLinks != null && alternativeLinks.length != 0) {
                        Browser br2 = br.cloneBrowser();
                        for (String alternativeLinkInfo[] : alternativeLinks) {
                            br2.getPage("http://149.13.65.144:8889/get/" + alternativeLinkInfo[0] + "/" + alternativeLinkInfo[1] + "?callback=jsonp" + System.currentTimeMillis());
                            String alts[] = br2.getRegex("\\'t\\':\\'(.*?)\\'").getColumn(0);
                            if (alts != null && alts.length != 0) {
                                Browser br3 = br.cloneBrowser();
                                for (String link : alts) {
                                    br3.getPage("http://www." + NICE_HOST + "/" + link + "/go.html");
                                    String finallink = br3.getRegex("<noframes> <br /> <a href=\"(.*?)\"").getMatch(0);
                                    if (finallink == null) finallink = br3.getRegex("<iframe style=\".*?\" src=\"(.*?)\"").getMatch(0);
                                    if (finallink != null) decryptedLinks.add(createDownloadlink(finallink));
                                }
                            }
                        }
                    }
                }
            }
            if (pornLink != null) {
                decryptedLinks.add(createDownloadlink(pornLink));
            }
            if (fpName != null) {
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}