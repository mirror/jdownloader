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

package jd.gui.swing.jdgui.components.premiumbar;

import java.awt.Cursor;

import javax.swing.JLabel;

import org.jdownloader.DomainInfo;

public class TinyProgressBar extends JLabel {

    private static final long serialVersionUID = 8385631080915257786L;

    private DomainInfo        domainInfo       = null;

    public TinyProgressBar() {
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /**
     * @param domainInfo
     *            the domainInfo to set
     */
    public void setDomainInfo(DomainInfo domainInfo) {
        this.domainInfo = domainInfo;
        this.setIcon(domainInfo.getFavIcon());
        this.setVisible(true);
        this.setToolTipText(domainInfo.getTld());
    }

    /**
     * @return the domainInfo
     */
    public DomainInfo getDomainInfo() {
        return domainInfo;
    }

}
