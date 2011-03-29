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

package org.jdownloader.extensions.extraction;

import java.io.File;

import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;

/**
 * DownloadLink for files which are not from the downloadlist.
 * 
 * @author botzi
 * 
 */
public class DummyDownloadLink extends DownloadLink {
    private static final long serialVersionUID = 4075187183435835432L;

    private File              file;

    public DummyDownloadLink(String name) {
        super(null, name, "dum.my", "", true);
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFileOutput() {
        return file.getAbsolutePath();
    }

    public String getFileOutput0() {
        return file.getAbsolutePath();
    }

    public LinkStatus getLinkStatus() {
        LinkStatus ls = new LinkStatus(this);
        ls.setStatus(LinkStatus.FINISHED);
        return ls;
    }
}