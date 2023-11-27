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
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.appwork.storage.TypeRef;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "trntbl.me" }, urls = { "http://(www\\.)?trntbl\\.me/[a-z0-9\\-]+" })
public class TrnTblMe extends PluginForDecrypt {
    public TrnTblMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    private final int ITEMS_PER_PAGE = 50;

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl();
        br.setFollowRedirects(true);
        final String user = new Regex(contenturl, "trntbl\\.me/(.+)").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(user);
        br.getHeaders().put("Accept", "*/*");
        final DecimalFormat df = new DecimalFormat("0000");
        int offset = 0;
        int request = 0;
        boolean cont = true;
        while (cont == true) {
            br.getPage("http://" + user + ".tumblr.com/api/read/json?callback=Request.JSONP.request_map.request_" + request + "&type=audio&start=" + offset + "&num=" + ITEMS_PER_PAGE + "&cache_bust=" + df.format(new Random().nextInt(1000)));
            final String json = new Regex(br.getRequest().getHtmlCode(), "(\\{.+)\\);$").getMatch(0);
            final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
            final List<Map<String, Object>> posts = (List<Map<String, Object>>) entries.get("posts");
            for (final Map<String, Object> post : posts) {
                final String playerhtml = (String) post.get("audio-player");
                String filename = null;
                String finallink = new Regex(playerhtml, "\\?audio_file=(http[^<>\"]*?)\"").getMatch(0);
                final ArrayList<DownloadLink> thisResults = new ArrayList<DownloadLink>();
                if (playerhtml != null) {
                    if (finallink != null) {
                        finallink = Encoding.htmlDecode(finallink.trim());
                        final String tumblrPostID = new Regex(finallink, "audio_file/[a-z0-9\\-]+/(\\d+)/").getMatch(0);
                        final String artist = (String) post.get("id3-artist");
                        final String album = (String) post.get("id3-album");
                        String title = (String) post.get("id3-title");
                        if (title == null) {
                            title = post.get("slug").toString();
                        }
                        if (title == null) {
                            title = tumblrPostID;
                        }
                        if (artist != null && album != null) {
                            filename = Encoding.htmlDecode(artist.trim()) + " - " + Encoding.htmlDecode(album.trim()) + " - " + Encoding.htmlDecode(title.trim());
                        } else if (artist != null) {
                            filename = Encoding.htmlDecode(artist.trim()) + " - " + Encoding.htmlDecode(title) + " - ";
                        } else {
                            filename = Encoding.htmlDecode(title.trim());
                        }
                        final DownloadLink dl;
                        if (tumblrPostID != null) {
                            final String postLink = "http://" + user + ".tumblrdecrypted.com/post/" + tumblrPostID;
                            dl = createDownloadlink(postLink);
                            dl.setProperty("audiodirectlink", finallink);
                        } else {
                            dl = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(finallink));
                        }
                        dl.setFinalFileName(filename + ".mp3");
                        dl.setAvailable(true);
                        thisResults.add(dl);
                    } else {
                        /* Look for external links */
                        // Maybe soundcloud link
                        finallink = new Regex(playerhtml, "<iframe src=\"(http[^<>\"]*?)\" frameborder=").getMatch(0);
                        if (finallink == null) {
                            // Maybe spotify link
                            finallink = new Regex(playerhtml, "class=\"spotify_audio_player\" src=\"(http[^<>\"]*?)\"").getMatch(0);
                            if (finallink == null) {
                                // Bandcamp
                                finallink = new Regex(playerhtml, "\"(http[^\"]+bandcamp\\.com/track/[^\"]+)").getMatch(0);
                            }
                        }
                        if (finallink != null) {
                            thisResults.add(this.createDownloadlink(Encoding.htmlDecode(finallink)));
                        } else {
                            /* Final fallback: Add all URLs we can find. */
                            final String[] urls = HTMLParser.getHttpLinks(playerhtml, br.getURL());
                            if (urls != null && urls.length > 0) {
                                for (final String url : urls) {
                                    thisResults.add(this.createDownloadlink(url));
                                }
                            }
                        }
                    }
                } else {
                    final String regularhtml = post.get("regular-body").toString();
                    final String[] urls = HTMLParser.getHttpLinks(regularhtml, br.getURL());
                    if (urls != null && urls.length > 0) {
                        for (final String url : urls) {
                            thisResults.add(this.createDownloadlink(url));
                        }
                    }
                }
                if (thisResults.isEmpty()) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                for (final DownloadLink result : thisResults) {
                    result._setFilePackage(fp);
                    distribute(result);
                    ret.add(result);
                }
            }
            logger.info("Crawled offset " + offset + " | Items found so far: " + ret.size());
            if (this.isAbort()) {
                logger.info("Stopping bcause: Aborted by user");
                break;
            } else if (posts.size() < ITEMS_PER_PAGE) {
                logger.info("Stopping because: Current page contains less items than: " + ITEMS_PER_PAGE);
                break;
            } else {
                /* Continue to next page */
                offset += ITEMS_PER_PAGE;
                request++;
            }
        }
        return ret;
    }
}
