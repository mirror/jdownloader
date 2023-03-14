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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linkjust.com" }, urls = { "https?://(?:www\\.)?linkjust\\.com/([A-Za-z0-9]+)" })
public class LinkjustCom extends MightyScriptAdLinkFly {
    public LinkjustCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected void correctURL(final CryptedLink param) {
        param.setCryptedUrl(param.getCryptedUrl().replaceFirst("http://", "https://"));
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        return super.decryptIt(param, progress);
    }

    @Override
    protected String getSpecialReferer() {
        /* Pre-set Referer to skip multiple ad pages e.g. linkjust.com -> ad domain -> ad domain 2 and so on -> linkjust.com */
        /* Last updated: 2023-03-14 */
        return "https://forexrw7.com";
    }
}
