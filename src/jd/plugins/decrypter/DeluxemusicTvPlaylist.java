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
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DeluxemusicTvPlaylist extends PluginForDecrypt {
    public DeluxemusicTvPlaylist(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "deluxemusic.de", "deluxemusic.tv" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/.+");
        }
        return ret.toArray(new String[0]);
    }

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        this.br.setFollowRedirects(true);
        if (param.getCryptedUrl().matches(".+deluxemusic\\.(?:tv|de)/.+")) {
            return findVideos(param);
        } else {
            return crawlCategory(param);
        }
    }

    private ArrayList<DownloadLink> findVideos(final CryptedLink param) throws IOException, PluginException {
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String[] embeddedVideoIDs = br.getRegex("dataId\\s*:\\s*\"([a-f0-9\\-]+)\"").getColumn(0);
        if (embeddedVideoIDs == null || embeddedVideoIDs.length == 0) {
            /* 2023-09-04 */
            embeddedVideoIDs = br.getRegex("id=\"player3q\" data-id=\"([a-f0-9\\-]+)").getColumn(0);
        }
        if (embeddedVideoIDs.length == 0) {
            logger.info("Failed to find any downloadable content");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        for (final String embeddedVideoID : embeddedVideoIDs) {
            final DownloadLink link = this.createDownloadlink("https://playout.3qsdn.com/config/" + embeddedVideoID + "?key=0&timestamp=0");
            link.setReferrerUrl(br.getURL());
            ret.add(link);
        }
        return ret;
    }

    /**
     * @throws PluginException
     */
    @Deprecated
    private ArrayList<DownloadLink> crawlCategory(final CryptedLink param) throws IOException, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }
}
