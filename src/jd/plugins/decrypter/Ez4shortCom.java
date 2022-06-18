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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ez4short.com" }, urls = { "https?://(?:www\\.)?ez4short\\.com/([A-Za-z0-9]+)" })
public class Ez4shortCom extends MightyScriptAdLinkFly {
    public Ez4shortCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String refererHost = "";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(false);
        param.setCryptedUrl(param.getCryptedUrl().replaceFirst("http://", "https://"));
        br.getPage(param.getCryptedUrl());
        /* E.g. redirect to techmody.io fake blog --> 2 steps --> Back to ez4short.com */
        refererHost = br.getRequest().getLocation();
        return super.decryptIt(param, progress);
    }

    @Override
    protected String getSpecialReferer() {
        return refererHost;
    }
}
