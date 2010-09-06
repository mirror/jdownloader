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

package jd.nutils;

import org.appwork.utils.event.DefaultEvent;

public class FtpEvent extends DefaultEvent {

    public static final int DOWNLOAD_PROGRESS = 0;
    private long progress;

    /**
     * Returns bytes loaded
     * 
     * @return
     */
    public long getProgress() {
        return progress;
    }

    public FtpEvent(SimpleFTP source, int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }

    public FtpEvent(SimpleFTP source, int ID, long counter) {
        this(source, ID);
        this.progress = counter;
    }

}
