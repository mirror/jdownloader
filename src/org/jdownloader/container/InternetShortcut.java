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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.ContainerStatus;
import jd.plugins.PluginsC;

import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

public class InternetShortcut extends PluginsC {
    public InternetShortcut() {
        super("InternetShortcut", "file:/.+\\.url$", "$Revision$");
    }

    public InternetShortcut newPluginInstance() {
        return new InternetShortcut();
    }

    @Override
    protected void deleteContainer(CrawledLink source, File file) {
    }

    public ContainerStatus callDecryption(File lc) {
        final ContainerStatus cs = new ContainerStatus(lc);
        try {
            cs.setStatus(ContainerStatus.STATUS_FINISHED);
            final FileInputStream fis = new FileInputStream(lc);
            try {
                final String fileContent = IO.readStreamToString(fis, 1024 * 32, true);
                if (fileContent.startsWith("[InternetShortcut]")) {
                    final String url = new Regex(fileContent, "\\s*URL\\s*=\\s*([^\r\n]*?)(?:\r|\n|$)").getMatch(0);
                    if (StringUtils.isNotEmpty(url)) {
                        final ArrayList<CrawledLink> retLinks = new ArrayList<CrawledLink>(1);
                        retLinks.add(new CrawledLink(url));
                        cls = retLinks;
                    }
                } else {
                    // can also be user generated .url files and no [InternetShortcut]
                    // throw new IOException("No InternetShortcut:" + lc);
                }
            } finally {
                fis.close();
            }
        } catch (IOException e) {
            logger.log(e);
            cs.setStatus(ContainerStatus.STATUS_FAILED);
        }
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