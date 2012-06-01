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

package jd.captcha.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JComponent;

/**
 * Component um Images darzustellen
 */
public class ImageComponent extends JComponent {

    private static final long serialVersionUID = -1497469256400862388L;
    public Image image;

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    /**
     * @param image
     */
    public ImageComponent(Image image) {
        this.image = image;
        if (image != null) {
            setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));
        }
    }

    /**
     * 
     * @return ImageHeight
     */
    public int getImageHeight() {
        return image.getHeight(this);
    }

    /**
     * 
     * @return imagewidth
     */
    public int getImageWidth() {
        return image.getWidth(this);
    }

    /**
     * zeichnet Bild
     * 
     * @param g
     */
    // @Override
    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(image, 0, 0, null);
    }
}