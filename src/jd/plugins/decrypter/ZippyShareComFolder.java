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

import org.appwork.utils.formatter.SizeFormatter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.UserAgents;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zippyshare.com" }, urls = { "https?://(?:www\\.)?zippyshare\\.com/[a-z0-9\\-_%,]+(/[a-z0-9\\-_%]+/dir\\.html)?" })
public class ZippyShareComFolder extends PluginForDecrypt {

    public ZippyShareComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getHeaders().put("User-Agent", UserAgents.stringUserAgent());
        br.addAllowedResponseCodes(500);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        // offline ?
        if (br.containsHTML(">User [^<]+ does not exist\\.</div>")) {
            // not worth adding offline link
            return decryptedLinks;
        }
        // Over 50 links? Maybe there is more...
        final String[] userDir = new Regex(parameter, "zippyshare\\.com/([a-z0-9\\-_,]+)(?:/([a-z0-9\\-_]+)/)?").getRow(0);
        final int r = 250;
        while (true) {
            final Browser br = this.br.cloneBrowser();
            final int dsize = decryptedLinks.size();
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("/fragments/publicDir/filetable.jsp", "page=" + (dsize / r) + "&user=" + userDir[0] + "&dir=" + (userDir[1] != null ? userDir[1] : "0") + "&sort=nameasc&pageSize=" + r + "&search=&viewType=default");
            if (br.getHttpConnection().getResponseCode() == 500) {
                // invalid directory
                return decryptedLinks;
            }
            parseLinks(decryptedLinks, br);
            if (decryptedLinks.size() != dsize + r) {
                break;
            }
        }
        if (userDir[1] == null) {
            // user directory
            final FilePackage fp = FilePackage.getInstance();
            fp.setName("User Directory - " + userDir[0]);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void parseLinks(final ArrayList<DownloadLink> decryptedLinks, final Browser br) throws PluginException {
        // lets parse each of the results and keep them trusted as online... this will reduce server loads
        String[] results = br.getRegex("<tr[^>]+class=(\"|')filerow even\\1.*?</tr>").getColumn(-1);
        if (results == null || results.length == 0) {
            results = br.getRegex("<div style[^>]+>\\s*<div style.*?</div>\\s*</div>").getColumn(-1);
        }
        if (results != null) {
            for (final String result : results) {
                final String link = new Regex(result, "\"((?:https?:)?(?://www\\d+\\.zippyshare\\.com)?/v/[a-zA-Z0-9]+/file\\.html)\"").getMatch(0);
                final String name = new Regex(result, ">([^\r\n]+)</a>").getMatch(0);
                final String size = new Regex(result, ">\\s*(\\d+(?:[\\.,]\\d+)?\\s*(?:B(?:yte)?|KB|MB|GB))\\s*").getMatch(0);
                if (link == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final DownloadLink dl = createDownloadlink(Request.getLocation(link, br.getRequest()));
                if (name != null) {
                    dl.setName(Encoding.htmlOnlyDecode(name));
                    dl.setAvailableStatus(AvailableStatus.TRUE);
                }
                if (size != null) {
                    dl.setDownloadSize(SizeFormatter.getSize(size.replace(",", ".")));
                }
                decryptedLinks.add(dl);
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}