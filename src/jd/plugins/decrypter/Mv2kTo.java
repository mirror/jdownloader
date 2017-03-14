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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "movie4k.to" }, urls = { "https?://(www\\.)?movie4k\\.(?:to|tv)/{1,2}(?!movies\\-(all|genre)|tvshows\\-season)(tvshows\\-\\d+\\-[^<>\"/]*?\\.html|[^<>\"/]*\\-\\d+(?:.*?\\.html)?|\\d+\\-[^<>\"/]*?)(\\.html)?" })
public class Mv2kTo extends PluginForDecrypt {

    public Mv2kTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS  = "https?://(www\\.)?movie4k\\.to//?[a-z0-9\\-_]+\\-all\\-\\d+\\.html";
    private static final String INVALIDLINKS2 = "https?://(www\\.)?movie4k\\.to//?tvshows\\-episode[a-z0-9\\-]+\\.html";

    /**
     * Description of regex array: 1= nowvideo.co, streamcloud.com 2=flashx.tv, vidbux.com, xvidstage.com, vidstream.in, hostingbulk.com,
     * uploadc.com, allmyvideos.net, firedrive.com, and many others 4=stream2k.com 5=flashx.tv, yesload.net
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final String old_domain = new Regex(param.toString(), "https?://(?:www\\.)?([^/]+/)").getMatch(0);
        final String parameter = param.toString().replace(old_domain, "movie4k.to/");
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String initalMirror = parameter.substring(parameter.lastIndexOf("/") + 1);
        br.setFollowRedirects(true);
        if (parameter.matches(INVALIDLINKS) || parameter.matches(INVALIDLINKS2) || parameter.contains("/index.php")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        try {
            br.getPage(parameter);
            final String continuelink = br.getRegex("<SCRIPT>window\\.location='([^<>\"]*?)';</SCRIPT>").getMatch(0);
            if (continuelink != null) {
                br.getPage(continuelink);
            }
            if (br.getHttpConnection().getResponseCode() == 404 || this.br.getURL().length() < 30) {
                logger.info("Invalid URL, or the URL doesn't exist any longer: " + parameter);
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            String fpName = br.getRegex("<title>Watch ([^<>\"]*?) online - Watch Movies Online, Full Movies, Download</title>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<title>(.*?) online").getMatch(0);
            }
            Browser br2 = br.cloneBrowser();

            int mirror = 1, part = 1, m = 0;
            String mirrors[] = br.getRegex("<OPTION\\s+(?:selected\\s+)?value=\"([^\"]+)\"").getColumn(0);
            if (mirrors != null && mirrors.length > 1) {
                mirror = mirrors.length;
            }
            String parts[] = br.getRegex("<a href=\"(movie\\.php\\?id=\\d+\\&part=\\d)\">").getColumn(0);
            if (parts != null && parts.length > 1) {
                part = parts.length;
            }

            for (int i = 0; i <= mirror; i++) {
                m++;
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim() + (mirror > 1 ? "@Mirror " + i : "")));
                secondary: for (int j = 1; j <= part; j++) {
                    final String[][] regexes = { { "width=\"\\d+\" height=\"\\d+\" frameborder=\"0\"( scrolling=\"no\")? src=\"(http://[^<>\"]*?)\"", "1" }, { "<a target=\"_blank\" href=\"((?!http://get\\.adobe\\.com/flashplayer/)(?:https?://)?[^<>\"]*?)\"", "0" }, { "<a href=\"((?!http://get\\.adobe\\.com/flashplayer/)(?:https?://)?[^<>\"]+)\" target=\"_blank\"", "0" }, { "<IFRAME SRC=\"(https?://[^<>\"]*?)\"", "0" }, { "<iframe width=\\d+% height=\\d+px frameborder=\"0\" scrolling=\"no\" src=\"(https?://embed\\.stream2k\\.com/[^<>\"]*?)\"", "0" }, { "\"(https?://flashx\\.tv/player/embed_player\\.php\\?vid=\\d+)", "0" }, { "\\'(https?://(www\\.)?novamov\\.com/embed\\.php\\?v=[^<>\"/]*?)\\'", "0" }, { "\"(https?://(www\\.)?video\\.google\\.com/googleplayer\\.swf\\?autoplay=1\\&fs=true\\&fs=true\\&docId=\\d+)", "0" }, { "(https?://embed\\.yesload\\.net/[\\w\\?]+)", "0" },
                            { "\"(https?://(www\\.)?videoweed\\.es/embed\\.php\\?v=[a-z0-9]+)\"", "0" }, { "<param name=\"movie\" value=\"(https?://(?:www\\.)?userporn\\.com/[^<>\"]*?)\"></param>", "0" } };
                    for (String[] regex : regexes) {
                        String finallink = br.getRegex(Pattern.compile(regex[0], Pattern.CASE_INSENSITIVE)).getMatch(Integer.parseInt(regex[1]));
                        if (finallink != null) {
                            if (finallink.contains("facebook.com/")) {
                                continue;
                            } else if (finallink.matches("https?://embed\\.stream2k\\.com/[^<>\"]+")) {
                                br2.getPage(finallink);
                                finallink = br2.getRegex("file: '(https?://[^<>\"]*?)',").getMatch(0);
                                if (finallink == null) {
                                    finallink = br2.getRegex("'(https?://server\\d+\\.stream2k\\.com/dl\\d+/[^<>\"/]*?)'").getMatch(0);
                                }
                                if (finallink != null) {
                                    finallink = "directhttp://" + finallink;
                                }
                            } else if (finallink.matches("https?://flashx\\.tv/player/embed_player\\.php\\?vid=\\d+")) {
                                br2.setFollowRedirects(true);
                                br2.getPage(finallink);
                                if (br2.containsHTML(">Video not found or deleted<")) {
                                    logger.info("Video not found or deleted");
                                    return decryptedLinks;
                                }
                                finallink = br2.getRegex("\"(https?://flashx\\.tv/video/[A-Z0-9]+/)").getMatch(0);
                            }
                            if (finallink != null) {
                                DownloadLink dl = createDownloadlink(finallink);
                                dl.setName(fpName + (mirror > 1 && part == 1 ? "__Mirror_" + m : "") + (part > 1 ? "__Part_" + j : ""));
                                dl.setProperty("MOVIE2K", true);
                                fp.add(dl);
                                if (!finallink.startsWith("directhttp://")) {
                                    distribute(dl);
                                }
                                decryptedLinks.add(dl);
                                continue secondary;
                            }
                        }
                    }
                    if (j > 0 && j < parts.length) {
                        String nextPart = parts[j];
                        if (!nextPart.startsWith("/")) {
                            nextPart = "/" + nextPart;
                        }
                        br.getPage(nextPart);
                        br2 = br.cloneBrowser();
                    }
                    // No wait = stream2k links may fail
                    this.sleep(2 * 1000l, param);
                }
                if (mirrors.length == 0) {
                    break;
                }
                if (i < mirrors.length) {
                    String next = mirrors[i];
                    if (initalMirror.equalsIgnoreCase(next)) {
                        i++;
                    }
                    if (i < mirrors.length) {
                        next = mirrors[i];
                        br.getPage(next);
                        br2 = br.cloneBrowser();
                        String mirrorParts[] = br.getRegex("<a href=\"(movie\\.php\\?id=\\d+\\&part=\\d)\">").getColumn(0);
                        if (mirrorParts != null && mirrorParts.length > 1) {
                            part = mirrorParts.length;
                        }
                        if (mirrorParts != null && mirrorParts.length > 0) {
                            System.arraycopy(mirrorParts, 0, parts, 0, parts.length);
                        }
                    }
                }
            }
        } catch (final BrowserException e) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}