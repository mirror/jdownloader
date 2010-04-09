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

package jd.gui.swing.jdgui.views.settings.panels.premium;

import jd.config.Property;

public class HostAccounts extends Property {
    /**
     * 
     */
    private static final long serialVersionUID = 1332558916724384633L;
    private String host = null;
    private long traffic = -1;
    private boolean enabled = true;
    private boolean gotaccountinfos = false;

    public HostAccounts(String host) {
        this.host = host;
    }

    public boolean gotAccountInfos() {
        return gotaccountinfos;
    }

    public void hasAccountInfos(boolean b) {
        gotaccountinfos = b;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean b) {
        enabled = b;
    }

    public String getHost() {
        return host;
    }

    public long getTraffic() {
        return traffic;
    }

    /*-1 for unlimited*/
    public void setTraffic(long t) {
        traffic = t;
    }

}
