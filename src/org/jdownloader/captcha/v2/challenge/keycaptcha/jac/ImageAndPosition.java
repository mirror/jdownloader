package org.jdownloader.captcha.v2.challenge.keycaptcha.jac;

import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * Datastructure to assign a position to an image
 *
 * @author flubshi
 *
 */
class ImageAndPosition {
    public final BufferedImage image;
    public final Point         position;

    /**
     * Assign a position to an image
     *
     * @param image
     *            the image (usually a puzzle piece for KeyCaptcha)
     * @param position
     *            the position to assign to the image
     */
    public ImageAndPosition(BufferedImage image, Point position) {
        this.image = image;
        this.position = position;
    }
}