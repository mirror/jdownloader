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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bestcash2020.com" }, urls = { "https?://(?:www\\.)?bestcash2020\\.com/([A-Za-z0-9]+)" })
public class Bestcash2020Com extends MightyScriptAdLinkFly {
    public Bestcash2020Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String refererHost = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final boolean followRedirectsOld = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(true);
            br.getPage(this.getContentURL(param));
            refererHost = br.getRequest().getLocation();
        } finally {
            br.setFollowRedirects(followRedirectsOld);
        }
        return super.decryptIt(param, progress);
    }

    @Override
    protected String getSpecialReferer() {
        /* Pre-set Referer to skip multiple ad pages e.g. bestcash2020.com -> e3raftech.online -> bestcash2020.com */
        final boolean returnStaticReferer = true;
        if (returnStaticReferer) {
            /* 2022-09-26 */
            return "https://ta.ta2deem7arbya.com/";
        } else {
            return refererHost;
        }
    }
}
