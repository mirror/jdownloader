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

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "8muses.com" }, urls = { "https?://(?:www\\.)?8muses\\.com/comix/(?:index/category/[a-z0-9\\-_]+|album(?:/[a-z0-9\\-_]+){1,6})" })
public class EightMusesComDecrypter extends antiDDoSForDecrypt {
    public EightMusesComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = parameter.substring(parameter.lastIndexOf("/") + 1);
        String[] categories = br.getRegex("(/index/category/[a-z0-9\\-_]+)\" data\\-original\\-title").getColumn(0);
        if (categories == null || categories.length == 0) {
            categories = br.getRegex("(\"|')(/album(?:/[a-z0-9\\-_]+){2,3})\\1").getColumn(1);
        }
        final String[] links = br.getRegex("(/picture/[^<>\"]*?)\"").getColumn(0);
        if ((links == null || links.length == 0) && (categories == null || categories.length == 0)) {
            logger.info("Unsupported or offline url");
            return decryptedLinks;
        }
        if (links != null && links.length > 0) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            for (final String singleLink : links) {
                final DownloadLink dl = createDownloadlink(Request.getLocation(singleLink, br.getRequest()));
                dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
                dl.setAvailable(true);
                fp.add(dl);
                decryptedLinks.add(dl);
            }
        }
        if (categories != null && categories.length > 0) {
            final String[] current = br.getURL().split("/");
            for (final String singleLink : categories) {
                final String[] corrected = Request.getLocation(singleLink, br.getRequest()).split("/");
                // since you can pick up cats lower down, we can evaluate based on how many so you don't re-decrypt stuff already decrypted.
                if (!StringUtils.endsWithCaseInsensitive(br.getURL(), singleLink) && current.length < corrected.length) {
                    decryptedLinks.add(createDownloadlink(Request.getLocation(singleLink, br.getRequest())));
                }
            }
        }
        return decryptedLinks;
    }
}
