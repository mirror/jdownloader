package jd.captcha.specials;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;
import jd.nutils.ColorSpaceConverter;
import jd.nutils.Colors;

public class ColoredObject {
    private static double getDiff(double[] din99a, double[] din99b) {
        double dif0 = din99a[0] - din99b[0];
        double dif1 = din99a[1] - din99b[1];
        double dif2 = din99a[2] - din99b[2];
        return Math.sqrt(dif0 * dif0 + dif1 * dif1 + dif2 * dif2);
    }

    private static boolean getRGBDist(int a, int b, double dist) {
        float[] ca = new Color(a).getRGBColorComponents(null);
        float[] cb = new Color(b).getRGBColorComponents(null);
        for (int i = 0; i < cb.length; i++) {
            if (Math.abs(ca[i] - cb[i]) > dist) return false;
        }
        return true;
    }

    public static List<PixelObject> getObjects(PixelGrid grid, double tollerance, int neighbourradius, int maxObjects) {
        double[][][] din99Grid = new double[grid.getWidth()][grid.getHeight()][];
        ColorSpaceConverter csc = new ColorSpaceConverter();
        java.util.List<PixelObject> ret = new ArrayList<PixelObject>();
        java.util.List<PixelObject> merge;
        double tr = tollerance / 8;
        double trgb = tollerance * 3;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {

                int c = grid.getGrid()[x][y];
                if (c == 0xffffff || c < 0) continue;

                PixelObject n = new PixelObject(grid);
                n.add(x, y, grid.getGrid()[x][y]);
                double[] din99a = din99Grid[x][y];
                merge = new ArrayList<PixelObject>();

                for (PixelObject o : ret) {
                    if (o.isTouching(x, y, true, neighbourradius, neighbourradius) && getRGBDist(c, o.getAverage(), trgb)) {
                        if (din99a == null) {
                            din99Grid[x][y] = csc.RGBtoDIN99(Colors.getRGB(c));
                            din99a = din99Grid[x][y];
                        }

                        double cd = getDiff(din99a, csc.RGBtoDIN99(Colors.getRGB(o.getAverage())));

                        if (cd < tollerance)
                            merge.add(o);
                        else if (cd < tollerance * 2) {

                            int[] atk = o.getNextPixel(x, y);

                            if (atk != null) {
                                double[] atkc = din99Grid[atk[0]][atk[1]];
                                if (atkc == null) {
                                    din99Grid[x][y] = csc.RGBtoDIN99(Colors.getRGB(grid.getGrid()[atk[0]][atk[1]]));
                                    atkc = din99Grid[x][y];
                                }
                                double diff = getDiff(din99a, atkc);
                                if (diff < tr) {
                                    // System.out.println("add");
                                    merge.add(o);
                                }
                            }
                        }
                    }
                }

                if (merge.size() == 0) {
                    ret.add(n);
                } else if (merge.size() == 1) {
                    merge.get(0).add(n);
                } else {
                    for (PixelObject po : merge) {
                        ret.remove(po);
                        n.add(po);
                    }
                    ret.add(n);
                }

            }
        }
        if (maxObjects != -1) {
            while (ret.size() > maxObjects) {
                PixelObject smallest = null;
                for (PixelObject o : ret) {
                    if (smallest == null)
                        smallest = o;
                    else if (o.getSize() < smallest.getSize()) {
                        smallest = o;
                    }
                }

                double[] din99a = csc.RGBtoDIN99(Colors.getRGB(smallest.getAverage()));
                double best = Double.MAX_VALUE;
                ret.remove(smallest);
                PixelObject mergeob = null;
                for (PixelObject pixelObject : ret) {
                    double[] din99b = csc.RGBtoDIN99(Colors.getRGB(pixelObject.getAverage()));
                    double dist = getDiff(din99a, din99b);
                    if (best > dist) {
                        best = dist;
                        mergeob = pixelObject;
                    }
                }
                if (mergeob != null)
                    mergeob.add(smallest);
                else
                    ret.add(smallest);
            }
        }
        return ret;
    }
    /*
     * public static void main(String[] args) { JAntiCaptcha jac = new
     * JAntiCaptcha("cms"); Image ret; try { ret = ImageIO.read(new
     * File("/home/dwd/.jd_home/captchas/ugtf/1608970.gif")); Captcha captcha =
     * jac.createCaptcha(ret); // BackGroundImageManager bgi = new
     * BackGroundImageManager(captcha); // bgi.clearCaptchaAll(); //
     * BasicWindow.showImage(captcha.getImage()); double dist =12; //
     * List<PixelObject> obs2 = ColorObjects.getObjects(captcha,40, 7);
     * List<PixelObject> obs2 = getObjects(captcha, dist, 2,2);
     * 
     * PixelObject biggest = null; for (PixelObject o : obs2) {
     * if(biggest==null) biggest=o; else if(o.getSize()<biggest.getSize()) {
     * biggest=o; } }
     * BasicWindow.showImage(biggest.toColoredLetter().getImage());
     * System.out.println(obs2.size());
     * 
     * } catch (IOException e) { // TODO Auto-generated catch block
     * e.printStackTrace(); }
     * 
     * }
     */

}
