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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "watchcartoonsonline.la" }, urls = { "https?://(www[0-9]*\\.)?watchcartoonsonline\\.la/.+" })
public class WatchCartoonsOnline extends antiDDoSForDecrypt {
    public WatchCartoonsOnline(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("property=\"og:title\" content=\"Watch ([^\"]+) (?:full episodes cartoon|Full Free Online|full online)").getMatch(0);
        String[] links = null;
        links = br.getRegex("'sources'\\s*:\\s*\\[([^\\]]+)\\]").getColumn(0);
        if (links != null && links.length > 0) {
            links = HTMLParser.getHttpLinks(StringUtils.join(links, "\r\n"), null);
        } else {
            links = br.getRegex("([^\"]+watchcartoonsonline\\.la/watch/[^\"]+)").getColumn(0);
        }
        if (links != null && links.length > 0) {
            for (final String link : links) {
                String dlURL = null;
                if (isAbort()) {
                    break;
                } else {
                    long size = -1;
                    String headerName = null;
                    if (StringUtils.containsIgnoreCase(link, "get_video.php")) {
                        final Browser brc = br.cloneBrowser();
                        brc.setFollowRedirects(false);
                        String nextURL = Encoding.htmlOnlyDecode(link);
                        while (!isAbort()) {
                            URLConnectionAdapter con = null;
                            try {
                                con = brc.openGetConnection(nextURL);
                                if (con.getRequest().getLocation() != null) {
                                    brc.followConnection();
                                    nextURL = brc.getRedirectLocation();
                                } else if (getCrawler().getDeepInspector().looksLikeDownloadableContent(con)) {
                                    dlURL = con.getURL().toString();
                                    if (con.getLongContentLength() > 0) {
                                        size = con.getLongContentLength();
                                    }
                                    headerName = getFileNameFromDispositionHeader(con);
                                    break;
                                } else {
                                    nextURL = con.getURL().toString();
                                    break;
                                }
                            } catch (IOException e) {
                                logger.log(e);
                            } finally {
                                if (con != null) {
                                    con.disconnect();
                                }
                            }
                        }
                    } else {
                        dlURL = Encoding.htmlOnlyDecode(link);
                    }
                    if (dlURL != null) {
                        final DownloadLink downloadLink = createDownloadlink(dlURL);
                        if (headerName != null) {
                            downloadLink.setFinalFileName(headerName);
                        } else if (fpName != null) {
                            downloadLink.setName(fpName);
                        }
                        if (size > 0) {
                            downloadLink.setDownloadSize(size);
                            downloadLink.setAvailable(true);
                        }
                        downloadLink.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
                        decryptedLinks.add(downloadLink);
                    }
                }
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}