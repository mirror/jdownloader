//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {}, flags = {})
public class DirectHttpCreatorDecrypter extends PluginForDecrypt {
    /**
     * Returns the annotations names array
     * 
     * @return
     */
    /*
     * Decrypter which converts normal http:// directlinks to directhttp:// links so they can be handled by the directhttp host plugin,
     * that's all ;)
     */
    public static String[] getAnnotationNames() {
        return new String[] { "Directhttp links" };
    }

    /**
     * returns the annotation pattern array
     * 
     * @return
     */
    public static String[] getAnnotationUrls() {
        StringBuilder completePattern = new StringBuilder();

        String[] list = { "http://dl\\d+\\.onlinefilefactory\\.net/downl/\\d+/[a-f0-9]{32}" };

        for (String pattern : list) {
            if (completePattern.length() > 0) {
                completePattern.append("|");
            }
            completePattern.append(pattern);
        }
        System.out.println(("Directhttp: " + list.length + " pattern added!"));
        return new String[] { completePattern.toString() };
    }

    /**
     * Returns the annotations flags array
     * 
     * @return
     */
    public static int[] getAnnotationFlags() {
        return new int[] { 0 };
    }

    public DirectHttpCreatorDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String declink = "directhttp://" + parameter;
        decryptedLinks.add(createDownloadlink(declink));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
