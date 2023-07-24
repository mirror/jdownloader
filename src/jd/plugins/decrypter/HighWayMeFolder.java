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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "high-way.me" }, urls = { "https?://((?:torrent|usenet)(archiv)?)\\.(?:high-way\\.me|dwld\\.link)/dl(?:u|t)/[a-z0-9]+(?:/$|/.+)" })
public class HighWayMeFolder extends GenericHTTPDirectoryIndexCrawler {
    public HighWayMeFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> crawledItems = super.decryptIt(param, progress);
        if (param.getCryptedUrl().matches("(?i)^https?://(?:torrentarchiv|torrent)\\.high-way\\.me/dlt/[a-z0-9]+/$")) {
            /*
             * Root of the torrent -> Can sometimes contain one single .zip file which contains all items again -> Filter that to avoid
             * downloading everything twice!
             */
            final List<DownloadLink> remove = new ArrayList<DownloadLink>();
            for (final DownloadLink link : crawledItems) {
                if (link.getName() != null && link.getName().matches("([a-f0-9]{40}|[a-f0-9]{64})\\.zip")) {
                    remove.add(link);
                }
            }
            if (remove.size() > 0) {
                logger.info("Found- and removed complete zip");
                crawledItems.removeAll(remove);
            } else {
                logger.info("Failed to find a complete zip file although we're in the torrent root folder -> Probably torrent is very big and thus no extra .zip file is provided");
            }
        }
        /*
         * Workaround! We want directURLs to be handled by our high-way.me host plugin, not directhttp a it's usually expected to happen
         * with results of the parent plugin "GenericHTTPDirectoryIndexCrawler".
         */
        for (final DownloadLink link : crawledItems) {
            if (link.getPluginPatternMatcher().startsWith("directhttp://")) {
                link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceFirst("directhttp://", ""));
                link.setHost(this.getHost());
            }
        }
        return crawledItems;
    }

    @Override
    protected String getCurrentDirectoryPath(final Browser br) {
        String path = br.getRegex("(?i)<(?:title|h1)>Index of (/[^<]+)</(?:title|h1)>").getMatch(0);
        if (path == null) {
            return null;
        } else {
            if (Encoding.isUrlCoded(path)) {
                path = Encoding.htmlDecode(path);
            }
            /* Remove internal base path as it's not required for the user. */
            final String removeThis = new Regex(path, "(?i)^(/torrent/([a-f0-9]{40}|[a-f0-9]{64}))/").getMatch(0);
            if (removeThis != null) {
                path = path.replaceFirst(removeThis, "");
            }
            return path;
        }
    }

    @Override
    protected String getCurrentDirectoryPath(final String url) throws UnsupportedEncodingException {
        final String path = new Regex(url, "(?i)^https?://[^/]+/dl(?:u|t)/[a-z0-9]+/(.+)").getMatch(0);
        if (path != null) {
            return URLDecoder.decode(path, "UTF-8");
        } else {
            /* Root */
            return "/";
        }
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}
