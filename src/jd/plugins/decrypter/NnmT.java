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

import java.net.URI;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {}, flags = {})
public class NnmT extends PluginForDecrypt {

    /**
     * Returns the annotations names array
     * 
     * @return
     */
    public static String[] getAnnotationNames() {
        return new String[] { "anonym.to", "dereferer.info" };
    }

    /**
     * returns the annotation pattern array
     * 
     * @return
     */
    public static String[] getAnnotationUrls() {
        String[] names = getAnnotationNames();
        String[] ret = new String[names.length];
        // "http://[\\w\\.]*?anonym\\.to/\\?.+"
        for (int i = 0; i < ret.length; i++) {
            ret[i] = "(http://[\\w\\.]*?" + names[i].replaceAll("\\.", "\\\\.") + "\\?/.+)|(http://[\\w\\-]{5,16}\\." + names[i].replaceAll("\\.", "\\\\.") + ")";

        }
        return ret;
    }

    /**
     * Returns the annotations flags array
     * 
     * @return
     */
    public static int[] getAnnotationFlags() {
        String[] names = getAnnotationNames();
        int[] ret = new int[names.length];

        for (int i = 0; i < ret.length; i++) {
            ret[i] = 0;

        }
        return ret;
    }

    public NnmT(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        this.setBrowserExclusive();
        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        String host = new URI(parameter.getCryptedUrl()).getHost();
        links.add(this.createDownloadlink(parameter.getCryptedUrl().replaceFirst("http://.*?" + host + "/\\?", "")));
        return links;
    }

}
