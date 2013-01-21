//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package org.jdownloader.container;

import java.io.File;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.nutils.io.JDIO;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginsC;
import jd.utils.JDUtilities;

public class MetaLink extends PluginsC {

    public MetaLink() {
        super("MetaLink", "file://.+\\.metalink", "$Revision: 13393 $");
    }

    public ContainerStatus callDecryption(File lc) {
        ContainerStatus cs = new ContainerStatus(lc);

        /* load plugin first, then we can include it */
        PluginForDecrypt decrypter = JDUtilities.getPluginForDecrypt("metalinker.org");

        if (decrypter == null) {
            cs.setStatus(ContainerStatus.STATUS_FAILED);
            return cs;
        }
        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        try {
            decrypter.pluginAPI("decryptString", JDIO.readFileToString(lc), links);
        } catch (final Exception e) {
            logger.log(e);
            cs.setStatus(ContainerStatus.STATUS_FAILED);
            return cs;
        }
        ArrayList<CrawledLink> retLinks = new ArrayList<CrawledLink>(links.size());
        for (DownloadLink link : links) {
            retLinks.add(new CrawledLink(link));
        }
        cls = retLinks;
        cs.setStatus(ContainerStatus.STATUS_FINISHED);
        return cs;
    }

    @Override
    public String[] encrypt(String plain) {
        return null;
    }

    /*
     * we dont have to hide metalink container links
     */
    @Override
    public boolean hideLinks() {
        return false;
    }

}