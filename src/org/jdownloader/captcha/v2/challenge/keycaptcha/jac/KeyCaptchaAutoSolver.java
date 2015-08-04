package org.jdownloader.captcha.v2.challenge.keycaptcha.jac;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import jd.nutils.Colors;

import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaImages;

// KeyCaptcha stuff
/**
 * Solves KeyCaptcha for us
 *
 * @author flubshi
 *
 */
public class KeyCaptchaAutoSolver {
    // min line length for border detection
    private final int           borderPixelMin = 15;
    // threshold for detection similar pixels
    private final Double        threshold      = 7.0d;

    private LinkedList<Integer> mouseArray     = new LinkedList<Integer>();

    private void marray(Point loc) {
        if (loc != null) {
            if (mouseArray.size() == 0) {
                mouseArray.add(loc.x + 465);
                mouseArray.add(loc.y + 264);
            }
            if (mouseArray.get(mouseArray.size() - 2) != loc.x + 465 || mouseArray.get(mouseArray.size() - 1) != loc.y + 264) {
                mouseArray.add(loc.x + 465);
                mouseArray.add(loc.y + 264);
            }
            if (mouseArray.size() > 40) {
                ArrayList<Integer> tmpMouseArray = new ArrayList<Integer>();
                tmpMouseArray.addAll(mouseArray.subList(2, 40));
                mouseArray.clear();
                mouseArray.addAll(tmpMouseArray);
            }
        }
    }

    public LinkedList<Integer> getMouseArray() {
        return mouseArray;
    }

    public String solve(KeyCaptchaImages images) {
        HashMap<BufferedImage, Point> imgPosition = new HashMap<BufferedImage, Point>();
        int limit = images.pieces.size();

        LinkedList<BufferedImage> piecesOld = new LinkedList<BufferedImage>(images.pieces);

        for (int i = 0; i < limit; i++) {
            List<DirectedBorder> borders = getBreakingBordersInImage(images.backgroundImage, borderPixelMin);
            ImageAndPosition imagePos = getBestPosition(images, borders);
            marray(new Point((int) (Math.random() * imagePos.position.x), (int) (Math.random() * imagePos.position.y)));
            marray(imagePos.position);

            imgPosition.put(imagePos.image, imagePos.position);
            images.integratePiece(imagePos.image, imagePos.position);
        }

        String positions = "";
        int i = 0;
        for (int c = 0; c < piecesOld.size(); c++) {
            BufferedImage image = piecesOld.get(c);
            final Point p = imgPosition.get(image);
            positions += (i != 0 ? "." : "") + String.valueOf(p.x) + "." + String.valueOf(p.y);
            i++;
        }
        return positions;
    }

    /**
     * Find vertical & horizontal borders within an image
     *
     * @param img
     *            the image to search in
     * @param min
     *            line length for border detection
     * @return a set of directed borders
     */
    private List<DirectedBorder> getBreakingBordersInImage(BufferedImage img, int minPixels) {
        List<DirectedBorder> triples = new LinkedList<DirectedBorder>();
        // horizontal
        for (int y = 0; y < img.getHeight() - 1; y++) {
            int c = -1;
            boolean whiteToColor = true;
            for (int x = 0; x < img.getWidth(); x++) {
                if (Colors.getCMYKColorDifference1(img.getRGB(x, y), Color.WHITE.getRGB()) < threshold && Colors.getCMYKColorDifference1(img.getRGB(x, y + 1), Color.WHITE.getRGB()) > threshold) {
                    if (!whiteToColor) {
                        whiteToColor = true;
                        c = -1;
                    }
                    c++;
                    if (c >= minPixels) {
                        triples.add(new DirectedBorder(new Point(x - c, y + 1), new Point(x, y + 1), Direction.TOPDOWN));
                        if (c > minPixels) {
                            triples.remove(triples.size() - 2);
                        }
                    }
                } else if (Colors.getCMYKColorDifference1(img.getRGB(x, y), Color.WHITE.getRGB()) > threshold && Colors.getCMYKColorDifference1(img.getRGB(x, y + 1), Color.WHITE.getRGB()) < threshold) {
                    if (whiteToColor) {
                        whiteToColor = false;
                        c = -1;
                    }
                    c++;
                    if (c >= minPixels) {
                        triples.add(new DirectedBorder(new Point(x - c, y), new Point(x, y), Direction.BOTTOMUP));
                        if (c > minPixels) {
                            triples.remove(triples.size() - 2);
                        }
                    }
                } else {
                    c = -1;
                }
            }
        }
        // vertical
        for (int x = 0; x < img.getWidth() - 1; x++) {
            int c = -1;
            boolean whiteToColor = true;
            for (int y = 0; y < img.getHeight(); y++) {

                if (Colors.getCMYKColorDifference1(img.getRGB(x, y), Color.WHITE.getRGB()) < threshold && Colors.getCMYKColorDifference1(img.getRGB(x + 1, y), Color.WHITE.getRGB()) > threshold) {
                    if (!whiteToColor) {
                        whiteToColor = true;
                        c = -1;
                    }
                    c++;
                    if (c >= minPixels) {
                        triples.add(new DirectedBorder(new Point(x + 1, y - c), new Point(x + 1, y), Direction.LEFTRIGHT));
                        if (c > minPixels) {
                            triples.remove(triples.size() - 2);
                        }
                    }
                } else if (Colors.getCMYKColorDifference1(img.getRGB(x, y), Color.WHITE.getRGB()) > threshold && Colors.getCMYKColorDifference1(img.getRGB(x + 1, y), Color.WHITE.getRGB()) < threshold) {
                    if (whiteToColor) {
                        whiteToColor = false;
                        c = -1;
                    }
                    c++;
                    if (c >= minPixels) {
                        triples.add(new DirectedBorder(new Point(x, y - c), new Point(x, y), Direction.RIGHTLEFT));
                        if (c > minPixels) {
                            triples.remove(triples.size() - 2);
                        }
                    }
                } else {
                    c = -1;
                }

            }
        }
        return triples;
    }

    /**
     * Gets the image and its position with highest possible probability to be correct for this puzzle piece
     *
     * @param keyCaptchaImages
     *            all keycaptcha images (background, sample, pieces)
     * @param borders
     *            a collection of all borders within the background image
     * @return one puzzle piece and its position within the puzzle
     */
    private ImageAndPosition getBestPosition(KeyCaptchaImages keyCaptchaImages, List<DirectedBorder> borders) {
        int bestMin = Integer.MAX_VALUE;
        Point bestPos = new Point();
        BufferedImage bestPiece = null;

        for (BufferedImage piece : keyCaptchaImages.pieces) {
            for (DirectedBorder border : borders) {
                if (border.direction == Direction.TOPDOWN || border.direction == Direction.BOTTOMUP) {
                    // horizontal
                    if ((border.direction == Direction.TOPDOWN && border.p1.y - piece.getHeight() < 0) || (border.direction == Direction.BOTTOMUP && (border.p1.y + piece.getHeight() > keyCaptchaImages.backgroundImage.getHeight()))) {
                        continue;
                    }

                    for (int x = Math.max(border.p1.x - piece.getWidth(), 0); x <= Math.min(border.p2.x, keyCaptchaImages.backgroundImage.getWidth() - 1); x++) {
                        int tmp = rateHorizontalLine(keyCaptchaImages.backgroundImage, piece, new Point(x, border.p1.y), border.direction == Direction.TOPDOWN ? piece.getHeight() - 1 : 0);
                        tmp /= piece.getWidth();
                        if (tmp < bestMin) {
                            bestMin = tmp;
                            bestPos = new Point(x, border.p1.y + (border.direction == Direction.TOPDOWN ? -piece.getHeight() : 1));
                            bestPiece = piece;
                        }
                    }
                } else {
                    // vertical
                    if ((border.direction == Direction.LEFTRIGHT && border.p1.x - piece.getWidth() < 0) || (border.direction == Direction.RIGHTLEFT && border.p1.x + piece.getWidth() > keyCaptchaImages.backgroundImage.getWidth())) {
                        continue;
                    }

                    for (int y = Math.max(border.p1.y - piece.getHeight(), 0); y <= Math.min(border.p2.y, keyCaptchaImages.backgroundImage.getHeight() - 1); y++) {
                        int tmp = rateVerticalLine(keyCaptchaImages.backgroundImage, piece, new Point(border.p1.x, y), border.direction == Direction.LEFTRIGHT ? piece.getWidth() - 1 : 0);
                        tmp /= piece.getHeight();
                        if (tmp < bestMin) {
                            bestMin = tmp;
                            bestPos = new Point(border.p1.x + (border.direction == Direction.LEFTRIGHT ? -piece.getWidth() : 1), y);
                            bestPiece = piece;
                        }
                    }
                }
            }
        }
        return new ImageAndPosition(bestPiece, bestPos);
    }

    /**
     * Rates probability the puzzle piece fits horizontal to this position
     *
     * @param background
     *            the background image
     * @param piece
     *            puzzle piece image
     * @param backgroundPosition
     *            the position to rate (within background image)
     * @param pieceY
     *            the y offset within puzzle to compare
     * @return a rating (smaller is better)
     */
    private int rateHorizontalLine(BufferedImage background, BufferedImage piece, Point backgroundPosition, int pieceY) {
        int diff = 0;
        for (int x = 0; x < piece.getWidth(); x++) {
            if (backgroundPosition.x + x >= background.getWidth()) {
                diff += (backgroundPosition.x + piece.getWidth() - background.getWidth()) * 150;
                break;
            }
            int bgColor = background.getRGB(backgroundPosition.x + x, backgroundPosition.y);
            int pColor = piece.getRGB(x, pieceY);
            diff += Colors.getColorDifference(bgColor, pColor);
            if (Colors.getCMYKColorDifference1(pColor, Color.WHITE.getRGB()) < threshold) {
                diff += 30;
            }
        }
        return diff;
    }

    /**
     * Rates probability the puzzle piece fits vertical to this position
     *
     * @param background
     *            the background image
     * @param piece
     *            puzzle piece image
     * @param backgroundPosition
     *            the position to rate (within background image)
     * @param pieceX
     *            the x offset within puzzle to comapre
     * @return a rating (smaller is better)
     */
    private int rateVerticalLine(BufferedImage background, BufferedImage piece, Point backgroundPosition, int pieceX) {
        int diff = 0;
        for (int y = 0; y < piece.getHeight(); y++) {
            if (backgroundPosition.y + y >= background.getHeight()) {
                diff += (backgroundPosition.y + piece.getHeight() - background.getHeight()) * 150;
                break;
            }
            int bgColor = background.getRGB(backgroundPosition.x, backgroundPosition.y + y);
            int pColor = piece.getRGB(pieceX, y);
            diff += Colors.getColorDifference(bgColor, pColor);
            if (Colors.getCMYKColorDifference1(pColor, Color.WHITE.getRGB()) < threshold) {
                diff += 30;
            }
        }
        return diff;
    }
}