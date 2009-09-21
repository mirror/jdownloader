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

import jd.captcha.gui.BasicWindow;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;
import jd.nutils.Colors;

/**
 * 
 * 
 * @author JD-Team
 */
public class DuckLoad {
	private static void mergeObjects(Vector<PixelObject> os) {
		for (PixelObject a : os) {
			for (PixelObject b : os) {
				if (a == b)
					continue;

				int xMin = Math.max(a.getXMin(), b.getXMin());
				int xMax = Math.min(a.getXMin() + a.getWidth(), b.getXMin()
						+ b.getWidth());
				if (xMax <= xMin)
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
    /**
     * enbtfernt eine farbe... die tolleranz gibt die farbdistanz an die njoch
     * entfernt wird
     * 
     * @param i
     *            vergleichsfarbe
     * @param d
     *            tolleranz
     */
    public static void cleanByColor(int i, double d, Captcha captcha) {
        int mv = captcha.getMaxPixelValue();
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                int c = captcha.getPixelValue(x, y);
                if (c!=mv&&Colors.getRGBColorDifference2(i, c) < d) {
                    captcha.setPixelValue(x, y, mv);
                }

            }
        }
        // grid = newgrid;
    }
	private static void clean(Captcha captcha) {
		int mv = captcha.getMaxPixelValue();
		ArrayList<Integer[]> a = new ArrayList<Integer[]>();
		int dist=2;
		Vector<Integer> cleaned = new Vector<Integer>();
		for (int x = 0; x < captcha.getWidth(); x++) {
			for (int y = 0; y < 3; y++) {
				int p = captcha.getPixelValue(x, y);
				if (p != mv && !cleaned.contains(p)) {
					cleanByColor(p, dist, captcha);
					cleaned.add(p);
				}
			}
			for (int y = captcha.getHeight()-3; y <  captcha.getHeight(); y++) {
				int p = captcha.getPixelValue(x, y);
				if (p != mv&& !cleaned.contains(p)) {
                    cleanByColor(p, dist, captcha);
                    cleaned.add(p);

				}
			}

		}
		for (int x = 0; x < captcha.getWidth(); x++) {
			for (int y = 0; y < captcha.getHeight(); y++) {

				int p = captcha.getPixelValue(x, y);

				if (p != mv) {
					int pos = -1;
					for (Integer[] b : a) {
						if (b[0].intValue() == p) {
							pos = b[1];
							break;
						}
					}
					if (pos != -1 && (x - pos) > 50&& !cleaned.contains(p)) {
	                    cleanByColor(p, dist, captcha);
	                    cleaned.add(p);

					} else
						a.add(new Integer[] { p, x });

				}
			}

		}
	}

	private static void mergeObjects2(Vector<PixelObject> os) {
		if (os.size() > 6) {
			PixelObject smallest = os.get(0);
			PixelObject mergepo = smallest;
			for (int i = 1; i < os.size(); i++) {
				PixelObject po = os.get(i);
				if (po.getArea() < smallest.getArea())
					smallest = po;
			}
			int xMin = Math.max(mergepo.getXMin(), smallest.getXMin());
			int xMax = Math.min(mergepo.getXMin() + mergepo.getWidth(),
					smallest.getXMin() + smallest.getWidth());
			int m = Math.abs(xMax - xMin);
			for (int i = 1; i < os.size(); i++) {
				PixelObject po = os.get(i);
				xMin = Math.max(po.getXMin(), smallest.getXMin());
				xMax = Math.min(po.getXMin() + po.getWidth(), smallest
						.getXMin()
						+ smallest.getWidth());
				int mi = Math.abs(xMax - xMin);
				if (mi < m) {
					m = mi;
					mergepo = po;
				}
			}

			os.remove(smallest);
			if (smallest.getArea() > 5)
				mergepo.add(smallest);
			mergeObjects2(os);

		}

	}

	public static Letter[] getLetters(Captcha captcha) {
		// captcha.cleanByRGBDistance(1, 25);
		clean(captcha);
		captcha.removeSmallObjects(0.75, 0.75, 10);
		return EasyCaptcha.getLetters(captcha);

	}

}