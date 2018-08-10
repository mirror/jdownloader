//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;

@DecrypterPlugin(revision = "$Revision: 39590 $", interfaceVersion = 3, names = { "dummydirect.jdownloader.org" }, urls = { "https?://dummydirect\\.jdownloader\\.org/[a-f0-9A-F]+" })
public class DummyDirect extends PluginForDecrypt {
    public DummyDirect(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String hex = new Regex(parameter, "https?://dummydirect\\.jdownloader\\.org/([a-f0-9A-F]+)").getMatch(0);
        final HashMap<String, Object> params = JSonStorage.restoreFromString(new String(HexFormatter.hexToByteArray(hex), "UTF-8"), TypeRef.HASHMAP);
        final String url = (String) params.get("url");
        if (StringUtils.isNotEmpty(url) && url.matches("^https?://.+$")) {
            final DownloadLink link = new DownloadLink(null, null, "DirectHTTP", "directhttp://" + url, true);
            final Number size = (Number) params.get("size");
            final Boolean verifiedSize = (Boolean) params.get("verifiedSize");
            if (size != null && size.longValue() >= 0) {
                if (Boolean.TRUE.equals(verifiedSize)) {
                    link.setVerifiedFileSize(size.longValue());
                } else {
                    link.setDownloadSize(size.longValue());
                }
            }
            final String name = (String) params.get("name");
            if (StringUtils.isNotEmpty(name)) {
                link.setFinalFileName(name);
            }
            final String referer = (String) params.get("referer");
            if (StringUtils.isNotEmpty(referer)) {
                link.setProperty("refURL", referer);
            }
            final String postData = (String) params.get("postData");
            if (StringUtils.isNotEmpty(postData)) {
                link.setProperty("post", postData);
            } else {
                final String method = (String) params.get("method");
                if (StringUtils.equalsIgnoreCase(method, "POST")) {
                    link.setProperty("post", "");
                }
            }
            final String cookies = (String) params.get("cookies");
            if (StringUtils.isNotEmpty(cookies)) {
                link.setProperty("cookies", cookies);
            }
            final Boolean recheck = (Boolean) params.get("recheck");
            if (!Boolean.TRUE.equals(recheck)) {
                link.setAvailable(true);
            }
            decryptedLinks.add(link);
        }
        return decryptedLinks;
    }

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }
}