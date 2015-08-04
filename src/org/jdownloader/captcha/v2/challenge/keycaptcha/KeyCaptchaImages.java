package org.jdownloader.captcha.v2.challenge.keycaptcha;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

/**
 * Represents a KeyCaptcha, which consists of a background image, sample image and a few puzzle pieces
 *
 * @author flubshi
 *
 */
public class KeyCaptchaImages {
    public BufferedImage             backgroundImage;
    public BufferedImage             sampleImage;
    public LinkedList<BufferedImage> pieces;

    /**
     * Creates an object
     *
     * @param backgroundImage
     *            The background image of the KeyCaptcha
     * @param sampleImage
     *            a smaller, monochrome version of the assembled image
     * @param pieces
     *            a collection of puzzle pieces
     */
    public KeyCaptchaImages(BufferedImage backgroundImage, BufferedImage sampleImage, LinkedList<BufferedImage> pieces) {
        this.backgroundImage = backgroundImage;
        this.sampleImage = sampleImage;
        this.pieces = pieces;
    }

    /**
     * Integrates a puzzle piece into the puzzle: removes piece from list of puzzle and draws the piece on background
     *
     * @param image
     *            the puzzle piece image
     * @param position
     *            the puzzle piece's position
     */
    public void integratePiece(BufferedImage image, Point position) {
        if (pieces.remove(image)) {
            Graphics2D g2d = backgroundImage.createGraphics();
            g2d.drawImage(image, position.x, position.y, null);
            g2d.dispose();
        }
    }
}