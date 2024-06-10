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
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.TypeRef;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FreeFrFolder extends PluginForDecrypt {
    public FreeFrFolder(PluginWrapper wrapper) {
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
        ret.add(new String[] { "transfert.free.fr" });
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
            ret.add("https?://" + buildHostsPatternPart(domains) + "/([A-Za-z0-9]{2,})");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String contenturl = param.getCryptedUrl();
        final String folderID = new Regex(contenturl, this.getSupportedLinks()).getMatch(0);
        /* Check for invalid folderID e.g. https://transfert.free.fr/upload --> "upload" */
        final boolean folderidContainsNumbers = !folderID.replaceAll("\\d", "").equals(folderID);
        if (folderID.equals(folderID.toLowerCase(Locale.ENGLISH)) && !folderidContainsNumbers) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage("https://api.scw.iliad.fr/freetransfert/v2/transfers/" + folderID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final List<Map<String, Object>> files = (List<Map<String, Object>>) entries.get("files");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(folderID);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int index = 0;
        final Browser brc = br.cloneBrowser();
        for (final Map<String, Object> file : files) {
            final String filename = file.get("path").toString();
            final String sizeBytesStr = file.get("size").toString();
            brc.getPage("/freetransfert/v2/files?transferKey=" + folderID + "&path=" + Encoding.urlEncode(filename));
            final Map<String, Object> dlresp = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final String directurl = dlresp.get("url").toString();
            final DownloadLink dl = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(directurl));
            dl.setName(filename);
            dl.setDownloadSize(Long.parseLong(sizeBytesStr));
            dl.setAvailable(true);
            /* Only group results if we got more than one file in this folder. */
            if (files.size() > 1) {
                dl._setFilePackage(fp);
            }
            ret.add(dl);
            distribute(dl);
            index++;
            logger.info("Crawled file " + index + "/" + files.size() + " -> " + filename);
        }
        return ret;
    }
}
