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


public class PieChartAPI extends ChartAPI {
    private static final long serialVersionUID = 7576517180813229367L;

    public PieChartAPI(int width, int height) {
        super(width, height);
    }

    @Override
    public String createDataString() {
        StringBuilder data = new StringBuilder();
        for (ChartAPIEntity tmp : super.getHashMap().values()) {
            if (data.length() > 0) data.append(',');
            data.append(getRelativeValue(tmp.getData()));
        }
        return data.toString();
    }

    public String createColorString() {
        StringBuilder data = new StringBuilder();
        for (ChartAPIEntity tmp : super.getHashMap().values()) {
            if (tmp.getColor() != null) {
                if (data.length() > 0) data.append(',');
                data.append(String.format("%02X%02X%02X", tmp.getColor().getRed(), tmp.getColor().getGreen(), tmp.getColor().getBlue()));
            }
        }
        return data.toString();
    }

    @Override
    public String getUrl() {
        return new StringBuilder("http://chart.apis.google.com/chart?cht=p3&chd=t:").append(createDataString()).append("&chco=").append(createColorString()).append("&chs=").append(getWidth()).append("x").append(getHeight()).append("&chl=").append(createCaptionString()).append("&chf=bg,s,00000000").toString();
    }

    public void fetchImage() {
        super.downloadImage(getUrl());
    }

}
