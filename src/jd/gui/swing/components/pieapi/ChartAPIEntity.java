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

package jd.gui.swing.components.pieapi;

import java.awt.Color;

public class ChartAPIEntity {
    private String caption = "";
    private String data = "";
    private Color color;

    public ChartAPIEntity(String caption, Object data, Color RGB) {
        this.caption = caption;
        this.data = data.toString();
        this.color = RGB;
    }

    public ChartAPIEntity(String caption, Object data) {
        this.caption = caption;
        this.data = data.toString();
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data.toString();
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
