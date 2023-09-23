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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hotfiles.eu" }, urls = { "https?://(?:www\\.)?(hotfil\\.es/\\d+|hotfiles\\.eu/download/[^/]+/\\d+)" })
public class HotfilEs extends PluginForDecrypt {
    public HotfilEs(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        final String fid = new Regex(br.getURL(), "(\\d+)$").getMatch(0);
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML(">A PHP Error was encountered")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (fid == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.br.setFollowRedirects(false);
        String fpName = new Regex(parameter, "/download/([^/]+)/\\d+").getMatch(0);
        final String[] hosts = br.getRegex("data-host=\"([^\"]+)\"").getColumn(0);
        if (hosts.length == 0) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        for (final String singleHost : hosts) {
            this.br.getPage("/fisier/redirect/" + singleHost + "/" + fid);
            final String finallink = this.br.getRedirectLocation();
            if (finallink == null || new Regex(finallink, this.getSupportedLinks()).matches()) {
                /* Skip invalid items. */
                continue;
            }
            ret.add(createDownloadlink(finallink));
            if (this.isAbort()) {
                logger.info("Decrtyption aborted by user");
                return ret;
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
        }
        return ret;
    }
}
