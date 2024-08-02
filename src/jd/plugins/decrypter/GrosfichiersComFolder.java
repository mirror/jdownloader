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
import java.util.List;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GrosfichiersComFolder extends PluginForDecrypt {
    public GrosfichiersComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "grosfichiers.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9]+)_([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(param.getCryptedUrl());
            if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (this.looksLikeDownloadableContent(con)) {
                /* We have a direct downloadable URL. */
                final DownloadLink direct = getCrawler().createDirectHTTPDownloadLink(con.getRequest(), con);
                ret.add(direct.getDownloadLink());
                return ret;
            }
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        final String thispath = br._getURL().getPath();
        final String[] links = br.getRegex("(/[A-Za-z0-9]+_[A-Za-z0-9]+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final List<String> pathsWithoutDupes = new ArrayList<String>();
        for (final String path : links) {
            if (!path.equals(thispath) && !pathsWithoutDupes.contains(path)) {
                pathsWithoutDupes.add(path);
            }
        }
        final String[] filesizes = br.getRegex("class=\"file_size\">(\\d+[^<]+)<").getColumn(0);
        final String[] filenames = br.getRegex("class=\"file_name\">\\s*<a [^>]*>([^<]+)</a>").getColumn(0);
        for (int i = 0; i < pathsWithoutDupes.size(); i++) {
            final String path = pathsWithoutDupes.get(i);
            if (path.equals(thispath)) {
                /* Current browsers' path goes to a folder link -> Exclude this from our results */
                continue;
            }
            final String fullURL = br.getURL(path).toExternalForm();
            final DownloadLink direct = createDownloadlink(DirectHTTP.createURLForThisPlugin(fullURL));
            if (filesizes != null && filesizes.length == pathsWithoutDupes.size()) {
                final String filesize = filesizes[i];
                direct.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            if (filenames != null && filenames.length == pathsWithoutDupes.size()) {
                final String filename = filenames[i];
                direct.setName(Encoding.htmlDecode(filename).trim());
            }
            direct.setAvailable(true);
            ret.add(direct);
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }
}
