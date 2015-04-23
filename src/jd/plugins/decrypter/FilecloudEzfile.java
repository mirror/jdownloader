//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

/**
 * delete this plugin when filecloud goes RIP
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision: 20458 $", interfaceVersion = 3, names = { "filecloud.io_ezfile.ch_wrapper" }, urls = { "(https?://)(?:www\\.)?(ifile\\.it|filecloud\\.io)/([a-z0-9]+)" }, flags = { 0 })
public class FilecloudEzfile extends PluginForDecrypt {

    public FilecloudEzfile(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // ok we want to rename old links to new format.
        final String parameter = param.toString().replace("ifile.it/", "filecloud.io/");
        final String protocol = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final String uid = new Regex(parameter, this.getSupportedLinks()).getMatch(2);
        // filecloud links are still valid but might be available on ezfile also, so we will add the other link to cover our bases.
        decryptedLinks.add(createDownloadlink(protocol + "decryptedezfile.ch/" + uid));
        // now add the original link!
        decryptedLinks.add(createDownloadlink(parameter.replace("filecloud.io/", "decryptedfilecloud.io/")));
        // return links
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}