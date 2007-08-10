package jd.captcha;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JComponent;

/**
 * Component um Images darzustellen
 * 
 * 
 */

public class ImageComponent extends JComponent {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1497469256400862388L;
	private Image image;

	public ImageComponent(Image image) {
		this.image = image;
		if(image!=null){
		setPreferredSize(new Dimension(image.getWidth(null), image
				.getHeight(null)));
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
	 */
	public void paintComponent(Graphics g) {
		g.drawImage(image, 0, 0, null);
	}
}