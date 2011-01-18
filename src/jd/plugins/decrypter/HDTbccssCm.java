//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hdtubeaccess.com" }, urls = { "http://[\\w\\.]*?(hdtubeaccess|vibetube)\\.com/videos/[0-9]+/" }, flags = { 0 })
public class HDTbccssCm extends PluginForDecrypt {

    public HDTbccssCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        // Get Page - site requires .html at the end (SEO)
        br.getPage(param.toString() + ".html");

        // Get Filename
        String file = br.getRegex("file=(.+?)\\&streamer").getMatch(0).replaceFirst("http", "directhttp");
        if (file == null) return null;

        // Set filename and add links
        DownloadLink link = createDownloadlink(file);
        link.setFinalFileName(br.getRegex("<h2>(.+?)</h2>").getMatch(0).concat(new Regex(file, "(\\.[^\\.]+)$").getMatch(0)));
        decryptedLinks.add(link);

        return decryptedLinks;
    }

}
