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

package jd.config;

import java.io.Serializable;

import javax.swing.ImageIcon;

import jd.utils.JDTheme;

public class ConfigGroup implements Serializable {
    private static final long serialVersionUID = 1075652697591884926L;

    private final String name;
    private final ImageIcon icon;

    public ConfigGroup(final String name, final String iconKey) {
        this(name, JDTheme.II(iconKey, 32, 32));
    }

    public ConfigGroup(final String name, final ImageIcon icon) {
        this.name = name;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public ImageIcon getIcon() {
        return icon;
    }

}
