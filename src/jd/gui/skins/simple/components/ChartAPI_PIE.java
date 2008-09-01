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

public class ChartAPI_PIE extends ChartAPI {
	private static final long serialVersionUID = 7576517180813229367L;

	public ChartAPI_PIE(String caption, int width, int height, Color RGB) {
		super(caption, width, height, RGB);
	}
	
	public String createDataString() {
		String data = "";
		for(ChartAPI_Entity tmp : super.getHashMap().values()) {
			data += getRelativeValue(tmp.getData()) + ",";
		}
		if(data.endsWith(",")) {
			return data.substring(0, data.length()-1);
		} else {
			return data;
		}
	}
	
	   public String createColorString() {
	        String data = "";
	        for(ChartAPI_Entity tmp : super.getHashMap().values()) {
	            if(tmp.getColor() != null) data += String.format( "%02X%02X%02X", tmp.getColor().getRed(), tmp.getColor().getGreen(), tmp.getColor().getBlue()) + ",";
	        }
	        if(data.endsWith(",")) {
	            return data.substring(0, data.length()-1);
	        } else {
	            return data;
	        }
	    }
		
	public String getUrl() {
		return "http://" + getServerAdress() + "/chart?cht=p&chd=t:" + createDataString() + "&chco=" + createColorString() + "&chs=" + getWidth() + "x" + getHeight() + "&chl=" + createCaptionString() + "&chf=bg,s," + String.format( "%02X%02X%02X", getBackgroundColor().getRed(), getBackgroundColor().getGreen(), getBackgroundColor().getBlue());
	}

	public void fetchImage() {
		super.downloadImage(getUrl());
	}

}
