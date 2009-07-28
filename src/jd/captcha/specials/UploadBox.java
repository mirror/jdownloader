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

package jd.captcha.specials;

import java.awt.Color;

import jd.nutils.Colors;

import jd.captcha.pixelgrid.PixelGrid;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;

/**
 * 
 * 
 * @author JD-Team
 */
public class UploadBox {
	private static void clean(Captcha captcha) {
		int[][] newgrid = new int[captcha.getWidth()][captcha.getHeight()];
		Color lastC = null;
		int[] lastpos = new int[] {0,0};
		for (int x = 0; x < captcha.getWidth(); x++) {
			for (int y = 0; y < captcha.getHeight(); y++) {

				int p = captcha.getPixelValue(x, y);

				Color c = new Color(p);


				if (c.getBlue() > 180 && c.getRed() > 180 && c.getGreen() > 180) {
					if (lastC == null || !((Math.abs(lastpos[0]-x)+Math.abs(lastpos[1]-y))<1 && Colors.getColorDifference(lastC.getRGB(), c.getRGB()) < 80))
						PixelGrid.setPixelValue(x, y, newgrid, captcha
								.getMaxPixelValue());
					else
						newgrid[x][y] = captcha.grid[x][y];
				} else {
					newgrid[x][y] = captcha.grid[x][y];
					lastC = c;
					lastpos=new int[] {x,y};
				}

			}
		}
		captcha.grid = newgrid;

	}
	private static int getGapAt(Captcha captcha, int position)
	{
		int avg = captcha.getAverage();
		int empty = -1;
		for (int x = position-5; x < position+5; x++) {
			if(captcha.getPixelValue(x, 25)!=avg)
			{
				if(empty==-1)
					empty++;
				else
					return x;
			}
			else
			{
				if(empty>5)
					empty=-1;
				else
					empty++;
			}
				
		}
		for (int x = position-5; x < position+5; x++) {
			if(captcha.getPixelValue(x, 24)!=avg)
			{
				if(empty==-1)
					empty++;
				else
					return x;
			}
			else
			{
				if(empty>5)
					empty=-1;
				else
					empty++;
			}
				
		}
		for (int x = position-5; x < position+5; x++) {
			if(captcha.getPixelValue(x, 26)!=avg)
			{
				if(empty==-1)
					empty++;
				else
					return x;
			}
			else
			{
				if(empty>5)
					empty=-1;
				else
					empty++;
			}
				
		}
		return position;
	}
	private static int[] getGaps(Captcha captcha) {
		return new int[] {getGapAt(captcha, 25), getGapAt(captcha, 50), getGapAt(captcha, 75), getGapAt(captcha, 100), getGapAt(captcha, 125), 150};
	}
	public static Letter[] getLetters(Captcha captcha) {
		clean(captcha);
		captcha.removeSmallObjects(0.75, 0.75, 6);
		captcha.toBlackAndWhite(0.95);
		return captcha.getLetters(6, getGaps(captcha));

	}

}