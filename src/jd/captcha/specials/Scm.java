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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;

/**
 * 
 * 
 * @author JD-Team
 */
public class Scm {

	private static void mergeObjects(Vector<PixelObject> os) {
		for (PixelObject a : os) {
			for (PixelObject b : os) {
				if (a == b)
					continue;

				int xMin = Math.max(a.getXMin(), b.getXMin());
				int xMax = Math.min(a.getXMin() + a.getWidth(), b.getXMin()
						+ b.getWidth());
				if (xMax + 1 < xMin)
					continue;
				int yMin = Math.max(a.getYMin(), b.getYMin());
				int yMax = Math.min(a.getYMin() + a.getHeight(), b.getYMin()
						+ b.getHeight());

				if (((xMax - xMin) < 30) && ((yMax - yMin) < 30)) {
					a.add(b);
					os.remove(b);
					mergeObjects(os);
					return;
				}
			}
		}

	}

	private static void clean(Captcha captcha) {
		int mv = captcha.getMaxPixelValue();

		for (int x = 0; x < captcha.getWidth(); x++) {
			int p = captcha.getPixelValue(x, 1);
			if (p != mv) {
				captcha.cleanByColor(p, 1);

			}
		}
		for (int y = 0; y < captcha.getHeight(); y++) {
			int p = captcha.getPixelValue(1, y);
			if (p != mv) {
				captcha.cleanByColor(p, 1);

			}
		}

	}

	public static Letter[] getLetters(Captcha captcha) throws InterruptedException{
		clean(captcha);
		captcha.toBlackAndWhite(0.95);
		Vector<PixelObject> os = captcha.getObjects(0.5, 0.5);
		Collections.sort(os);
		mergeObjects(os);
		ArrayList<Letter> ret = new ArrayList<Letter>();
		for (PixelObject pixelObject : os) {
			if (pixelObject.getArea() > 50) {
				Letter let = pixelObject.toLetter();
				let.removeSmallObjects(0.75, 0.75, 6);
				let.resizetoHeight(25);
				ret.add(let);
			}
		}
		return ret.toArray(new Letter[] {});

	}

}