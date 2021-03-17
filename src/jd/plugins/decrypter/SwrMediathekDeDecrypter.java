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
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "swrmediathek.de" }, urls = { "https?://(?:www\\.)?swrmediathek\\.de/player\\.htm\\?show=[a-f0-9\\-]+" })
public class SwrMediathekDeDecrypter extends PluginForDecrypt {
    public SwrMediathekDeDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** 2021-03-17: Legacy plugin to handle old swrmediathek.de URLs which can sometimes redirect to new ardmediathek.de URLs. */
    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        final PluginForDecrypt plg = JDUtilities.getPluginForDecrypt("ardmediathek.de");
        if (!plg.canHandle(br.getURL()) || this.br.getURL().length() < 40) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
        } else {
            decryptedLinks.add(this.createDownloadlink(this.br.getURL()));
        }
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}