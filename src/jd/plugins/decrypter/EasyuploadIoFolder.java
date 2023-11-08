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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.EasyuploadIo;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { EasyuploadIo.class })
public class EasyuploadIoFolder extends PluginForDecrypt {
    public EasyuploadIoFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return EasyuploadIo.getPluginDomains();
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
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/.+");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final PluginForHost hosterplugin = this.getNewPluginForHostInstance(this.getHost());
        final String[] fileids = br.getRegex("data-url=\"([a-z0-9]+)\"").getColumn(0);
        final String[] filenames = br.getRegex("class=\"col s\\d+ left-align valign-wrapper\"><p>([^<]+)<").getColumn(0);
        String[] filesizes = br.getRegex("<div class=\"col s3 left-align valign-wrapper\"><p>([^<]+)<").getColumn(0);
        if (filesizes == null || filesizes.length == 0) {
            filesizes = br.getRegex("class=\"turbo\"[^>]*>\\(([^<]+)\\)\\s*<").getColumn(0);
        }
        if (fileids != null && fileids.length > 0) {
            final FilePackage fp = FilePackage.getInstance();
            for (int i = 0; i < fileids.length; i++) {
                final String fileid = fileids[i];
                final DownloadLink link = this.createDownloadlink(br.getURL("/" + fileid).toString());
                link.setDefaultPlugin(hosterplugin);
                link._setFilePackage(fp);
                if (filenames.length == fileids.length) {
                    final String filename = filenames[i];
                    link.setName(Encoding.htmlDecode(filename));
                }
                if (filesizes.length == fileids.length) {
                    final String filesize = filesizes[i];
                    link.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                link.setAvailable(true);
                ret.add(link);
            }
        } else {
            /* Assume we got a single file */
            final DownloadLink file = this.createDownloadlink(br.getURL());
            file.setDefaultPlugin(hosterplugin);
            EasyuploadIo.parseFileInfo(br, file);
            file.setAvailable(true);
            ret.add(file);
        }
        return ret;
    }
}
