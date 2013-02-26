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

package jd.captcha.easy;

import java.awt.Image;
import java.io.File;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.Utilities;

import org.jdownloader.logging.LogController;

public class BackGroundImage extends CPoint {
    /**
     * 
     */
    private static final long serialVersionUID = 8700548338559980634L;
    private transient Image   image            = null;
    private String            backgroundImage;

    /**
     * gibt den namen des Hindergrundbildes aus
     * 
     * @return
     */
    public String getBackgroundImage() {
        return backgroundImage;
    }

    /**
     * Name des Hintergrundbildes setzen
     * 
     * @param backgroundImage
     */
    public void setBackgroundImage(String backgroundImage) {
        image = null;
        this.backgroundImage = backgroundImage;
    }

    /**
     * gibt das Hintergrundbild als Image zurück
     * 
     * @param methode
     * @return
     */
    public Image getImage(EasyMethodFile methode) {
        if (image == null) {
            image = Utilities.loadImage(new File(methode.file, getBackgroundImage()));
        }
        return image;
    }

    /**
     * Entfernt das Hintergrundbild vom übergebenen Captcha
     * 
     * @param captchaImage
     * @throws InterruptedException
     */
    public void clearCaptcha(Captcha captchaImage) throws InterruptedException {

        EasyMethodFile methode = new EasyMethodFile(captchaImage.owner.getResourceFile("jacinfo.xml").getParentFile());
        Image bImage = getImage(methode);

        if (bImage == null || bImage.getWidth(null) != captchaImage.getWidth() || bImage.getHeight(null) != captchaImage.getHeight()) {

            LogController.CL().severe("ERROR Maske und Bild passen nicht zusammmen");

            return;
        }
        Captcha cleanImg = captchaImage.owner.createCaptcha(bImage);
        int color = getColor();

        for (int x = 0; x < captchaImage.getWidth(); x++) {
            for (int y = 0; y < captchaImage.getHeight(); y++) {
                int pv = captchaImage.getPixelValue(x, y);
                setColor(cleanImg.getPixelValue(x, y));
                if (getColorDifference(pv) < getDistance()) captchaImage.setPixelValue(x, y, color);
            }
        }
        setColor(color);
    }

    @Override
    public BackGroundImage clone() {
        BackGroundImage ret = new BackGroundImage();
        ret.setLocation(getLocation());
        ret.setColor(getColor());
        ret.setDistance(getDistance());
        ret.setColorDistanceMode(getColorDistanceMode());
        ret.setForeground(isForeground());
        ret.backgroundImage = backgroundImage;
        ret.image = image;
        return ret;
    }
}
