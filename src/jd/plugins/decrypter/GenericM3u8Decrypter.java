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

import org.appwork.utils.Regex;

//Decrypts embedded videos from dailymotion
@DecrypterPlugin(revision = "$Revision: 26321 $", interfaceVersion = 3, names = { "m3u8" }, urls = { "https?://.+\\.m3u8" }, flags = { 0 })
public class GenericM3u8Decrypter extends PluginForDecrypt {

    public GenericM3u8Decrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.containsHTML("#EXT-X-STREAM-INF")) {
            for (String line : Regex.getLines(br.toString())) {
                if (!line.startsWith("#")) {
                    if (line.startsWith("http")) {
                        DownloadLink link = createDownloadlink(line);
                        ret.add(link);
                    } else {
                        DownloadLink link = createDownloadlink(br.getBaseURL() + line);
                        ret.add(link);
                    }
                }

            }
        } else {
            DownloadLink link = createDownloadlink("m3u8" + param.getCryptedUrl().substring(4));
            if (br.containsHTML("EXT-X-KEY")) {
                link.setProperty("ENCRYPTED", true);
            }
            ret.add(link);
        }

        return ret;
    }
}