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

package jd.plugins;

import java.io.Serializable;

public class TransferStatus implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -5529970643122096722L;
    private boolean supportsresume = false;
    private boolean supportspremium = false;
    private boolean usespremium = false;

    public boolean supportsResume() {
        return supportsresume;
    }

    public boolean usesPremium() {
        return usespremium;
    }

    public boolean supportsPremium() {
        return supportspremium;
    }

    public void setResumeSupport(final boolean b) {
        supportsresume = b;
    }

    public void setPremiumSupport(final boolean b) {
        supportspremium = b;
    }

    public void usePremium(final boolean b) {
        usespremium = b;
    }

}
