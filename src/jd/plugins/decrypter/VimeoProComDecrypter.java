//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vimeopro.com" }, urls = { "https?://vimeopro.com/[^/]+/[^/]+(/video/\\d+)?" })
public class VimeoProComDecrypter extends PluginForDecrypt {
    public VimeoProComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String pattern = "/[^/]+/[^/]+/video/(\\d+)";

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        final String videoID = new Regex(param.getCryptedUrl(), pattern).getMatch(0);
        String playerURL = br.getRegex("<iframe\\s*src\\s*=\\s*\"((?:https:)?//player\\.vimeo\\.com/video/\\d+[^\"]*)\"").getMatch(0);
        if (playerURL == null && videoID != null) {
            playerURL = br.getRegex("((?:https:)?//player\\.vimeo\\.com/video/" + Pattern.quote(videoID) + "[^\"]*)\"").getMatch(0);
            playerURL = Encoding.htmlOnlyDecode(playerURL);
        }
        if (playerURL == null) {
            final String otherIframe = br.getRegex("<iframe\\s*src\\s*=\\s*\"([^\"]+)\"").getMatch(0);
            if (otherIframe != null) {
                /**
                 * 2022-06-23 e.g. https://vimeopro.com/user111222222/online-exercise-classes </br> 2nd example:
                 * https://vimeopro.com/cwdc/bla-training/video/69901718
                 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("\\{VIMEO_URL\\}/log_in")) {
                if (videoID == null) {
                    /* E.g. age restricted */
                    throw new AccountRequiredException();
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (videoID != null) {
            final String portfolio_id = br.getRegex("portfolio_id\\s*=\\s*(\\d+)").getMatch(0);
            String url = br.getURL(playerURL).toString();
            if (!StringUtils.containsIgnoreCase(url, "portfolio_id")) {
                if (portfolio_id == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    url = url + "&portfolio_id=" + portfolio_id;
                }
            }
            url = url + "#forced_referer=" + HexFormatter.byteArrayToHex(param.getCryptedUrl().getBytes("UTF-8"));
            decryptedLinks.add(createDownloadlink(url));
        } else {
            final String portfolio_id = new Regex(playerURL, "portfolio_id\\s*=\\s*(\\d+)").getMatch(0);
            if (portfolio_id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Set<String> videoIDs = new HashSet<String>();
            int page = 0;
            boolean nextPage = true;
            while (nextPage) {
                nextPage = false;
                page++;
                if (page > 1) {
                    br.getPage(param.getCryptedUrl() + "/page/" + page);
                }
                final String videos[] = br.getRegex(pattern).getColumn(0);
                nextPage = videoIDs.addAll(Arrays.asList(videos));
            }
            for (final String video : videoIDs) {
                final String url = br.getURL("https://player.vimeo.com/video/" + video + "?portfolio_id=" + portfolio_id).toString() + "#forced_referer=" + HexFormatter.byteArrayToHex(param.getCryptedUrl().getBytes("UTF-8"));
                decryptedLinks.add(createDownloadlink(url));
            }
        }
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}