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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.HexFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vimeo.com" }, urls = { "https?://vimeopro.com/[^/]+/[^/]+(/video/\\d+)?" })
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
        if (videoID != null) {
            final String iframe = br.getRegex("iframe\\s*src\\s*=\\s*\"((?:https:)?//player.vimeo.com/video/" + videoID + ".*?)\"").getMatch(0);
            if (iframe == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url = br.getURL(iframe).toString() + "#forced_referer=" + HexFormatter.byteArrayToHex(param.getCryptedUrl().getBytes("UTF-8"));
            decryptedLinks.add(createDownloadlink(url));
        } else {
            final String iframe = br.getRegex("iframe\\s*src\\s*=\\s*\"((?:https:)?//player.vimeo.com/video/.*?)\"").getMatch(0);
            final String portfolio_id = new Regex(iframe, "portfolio_id=(\\d+)").getMatch(0);
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

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}