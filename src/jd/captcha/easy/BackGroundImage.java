package jd.captcha.easy;

import java.awt.Image;
import java.io.File;
import jd.captcha.pixelgrid.Captcha;
import jd.controlling.JDLogger;
import jd.captcha.utils.Utilities;

public class BackGroundImage extends CPoint {
    /**
     * 
     */
    private static final long serialVersionUID = 8700548338559980634L;
    private transient Image image =null;
    private String backgroundImage;
    /**
     * gibt den namen des Hindergrundbildes aus
     * @return
     */
    public String getBackgroundImage() {
        return backgroundImage;
    }
    /**
     * Name des Hintergrundbildes setzen
     * @param backgroundImage
     */
    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }
    /**
     * gibt das Hintergrundbild als Image zurück
     * @param methode
     * @return
     */
    public Image getImage(EasyFile methode) {
        if(image==null)
        {
            image=Utilities.loadImage(new File(methode.file, getBackgroundImage()));
        }
        return image;
    }
    /**
     * Entfernt das Hintergrundbild vom übergebenen Captcha
     * @param captchaImage
     */
    public void clearCaptcha(Captcha captchaImage) {
        EasyFile methode = new EasyFile(captchaImage.owner.getResourceFile("jacinfo.xml").getParentFile());
        Image bImage = getImage(methode);

        if (bImage==null||bImage.getWidth(null) != captchaImage.getWidth() || bImage.getWidth(null) != captchaImage.getHeight()) {
            if (Utilities.isLoggerActive()) {
                JDLogger.getLogger().info("ERROR Maske und Bild passen nicht zusammmen");
            }
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
}
