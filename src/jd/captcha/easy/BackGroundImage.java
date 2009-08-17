package jd.captcha.easy;

import java.awt.Image;
import java.io.File;
import jd.captcha.utils.Utilities;

public class BackGroundImage extends CPoint {
    /**
     * 
     */
    private static final long serialVersionUID = 8700548338559980634L;
    private transient Image image =null;
    private String backgroundImage;

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public Image getImage(EasyFile methode) {
        if(image==null)
        {
            image=Utilities.loadImage(new File(methode.file, getBackgroundImage()));
        }
        return image;
    }
}
