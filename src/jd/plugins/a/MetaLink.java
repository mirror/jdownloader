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

package jd.plugins.a;

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.nutils.io.JDIO;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
import jd.plugins.PluginsC;
import jd.utils.JDUtilities;

public class MetaLink extends PluginsC {

    public MetaLink(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ContainerStatus callDecryption(File lc) {
        ContainerStatus cs = new ContainerStatus(lc);
        String linkContent = JDIO.readFileToString(lc);
        jd.plugins.decrypter.MtLnk decrypter = (jd.plugins.decrypter.MtLnk) JDUtilities.getPluginForDecrypt("metalinker.org");
        if (decrypter == null) {
            cs.setStatus(ContainerStatus.STATUS_FAILED);
            return cs;
        }
        decrypter = (jd.plugins.decrypter.MtLnk) decrypter.getWrapper().getNewPluginInstance();
        ArrayList<DownloadLink> links = decrypter.decryptString(linkContent);
        cls = links;
        dlU = new ArrayList<String>();
        for (DownloadLink l : links) {
            dlU.add(l.getDownloadURL());
        }
        cs.setStatus(ContainerStatus.STATUS_FINISHED);
        return cs;
    }

    @Override
    public String[] encrypt(String plain) {
        return null;
    }

    @Override
    public long getVersion() {
        return 1;
    }

    /*
     * we dont have to hide metalink container links
     */
    @Override
    public boolean hideLinks() {
        return false;
    }

}