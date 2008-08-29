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

package jd.gui.skins.simple.components;

import java.awt.Color;

public class ChartAPI_Entity {
	private String caption = "";
	private String data = "";
	private Color color;
	
	public ChartAPI_Entity(String caption, String data, String RGB) {
		String csplit[] = RGB.split(",");
		this.caption = caption;
		this.data = data;
		this.color = new Color(Integer.parseInt(csplit[0]), Integer.parseInt(csplit[1]), Integer.parseInt(csplit[2]));
	}
	
	public ChartAPI_Entity(String caption, String data) {
		this.caption = caption;
		this.data = data;
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
	public void setData(String data) {
		this.data = data;
	}
	public Color getColor() {
		return color;
	}
	public void setColor(Color color) {
		this.color = color;
	}
}
