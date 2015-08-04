package org.jdownloader.captcha.v2.challenge.keycaptcha.jac;

import java.awt.Point;

/**
 * Represents a border an the direction of white to 'color'
 *
 * @author flubshi
 *
 */
class DirectedBorder {
    public final Point     p1;
    public final Point     p2;
    public final Direction direction;

    /**
     * @param p1
     *            start of bordering line
     * @param p2
     *            end of bordering line
     * @param direction
     *            the direction (e.g. TOPDOWN if a horizontal line with white above and color below)
     */
    public DirectedBorder(Point p1, Point p2, Direction direction) {
        this.p1 = p1;
        this.p2 = p2;
        this.direction = direction;
    }
}