package jd.captcha.specials.icaptcha;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.imageio.ImageIO;

import jd.captcha.gui.BasicWindow;
import jd.http.Browser;
import jd.utils.JDUtilities;

public class ICaptcha {

    private static final int DIST = 30;

    public static void main(String[] args) throws Exception {
        JDUtilities.getResourceFile("caps/icaptcha/").mkdirs();
        for (int i = 0; i < 500; i++) {
            if (JDUtilities.getResourceFile("caps/icaptcha/img" + i + ".png").exists()) continue;
            try {
                Browser br = new Browser();
                br.getPage("http://odsiebie.com/weryfikacja/icaptcha.php");
                String v = br.toString().substring(2);
                if (v.length() / 12 > 40) {
                    i--;
                    continue;
                }
                BufferedImage image = paintImage(v, 250, 70);

                System.out.println((v.length() / 12) + " --> div:" + (v.length() / (12 * 5)));

                BasicWindow.showImage(image, (v.length() / 12) + "");
                ImageIO.write(image, "png", JDUtilities.getResourceFile("caps/icaptcha/img" + i + ".png"));
                System.out.println(JDUtilities.getResourceFile("caps/icaptcha/img" + i + ".png").getAbsolutePath());
                Thread.sleep(500);
            } catch (Exception e) {
                i--;
                e.printStackTrace();
            }
        }
    }

    public static BufferedImage paintImage(String code, int width, int height) {
      
        int z = 30;
        int length = code.length();
        int l = length / 12;
        
       if(l==40) width += DIST * 5;
        int f = 0;
        int g = 0;

        ArrayList<Curve> list = new ArrayList<Curve>();
        while (f < l) {
            g = 0;
            int[] s;
            int[] p;
            int[] c;
            list.add(new Curve(p = new int[2], s = new int[2], c = new int[2]));
            while (g < 2) {
                p[g] = Integer.parseInt(code.substring(f * 12 + g * 2, f * 12 + g * 2 + 2), 16);
                s[g] = Integer.parseInt(code.substring(f * 12 + g * 2 + 4, f * 12 + g * 2 + 6), 16);
                c[g] = Integer.parseInt(code.substring(f * 12 + g * 2 + 8, f * 12 + g * 2 + 10), 16);
                g++;
            }

            f++;
        }

        Collections.sort(list, new Comparator<Curve>() {
            public int compare(Curve o1, Curve o2) {
                int middle1 = (o1.s[0] + o1.p[0]) / 2;
                int middle2 = (o2.s[0] + o2.p[0]) / 2;

                if (middle1 < middle2) return -1;
                if (middle1 > middle2) return 1;
                return 0;
            }

        });

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(8));
        g2.setBackground(Color.WHITE);
        g2.clearRect(0, 0, width, height);
        g2.setColor(Color.BLACK);
        GeneralPath p = new GeneralPath();
        f = 0;
        Color[] cols = new Color[] { Color.BLUE, Color.GREEN, Color.MAGENTA, Color.DARK_GRAY,Color.RED ,Color.RED, Color.RED };

        int offset = 0;
        int letter = 0;
        int div = list.size() / 5;
        for (int i = 0; i < list.size(); i++) {
            if (i % div == 0) {
                if(l==40)offset += DIST;
                if(l==40) g2.setColor(cols[letter]);
                letter++;
            }
            Curve curve = list.get(i);
            int xx = curve.p[0];
            int yy = curve.p[1] - z;
            p.moveTo(xx + offset, yy);
            p.quadTo(curve.c[0] + offset, curve.c[1] - z, curve.s[0] + offset, curve.s[1] - z);
            g2.draw(p);

            p = new GeneralPath();
            f++;
        }
        return image;
    }

}
