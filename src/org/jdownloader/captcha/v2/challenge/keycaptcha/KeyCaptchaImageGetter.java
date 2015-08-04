package org.jdownloader.captcha.v2.challenge.keycaptcha;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.JLabel;

import jd.http.Browser;
import jd.utils.JDUtilities;

class KeyCaptchaImageGetter {
    private Image[]                            IMAGE;
    private BufferedImage[]                    kcImages;
    private final LinkedHashMap<String, int[]> coordinates;

    private int                                kcSampleImg;

    private KeyCaptchaImages                   keyCaptchaImage;
    private KeyCaptcha                         helper;

    public KeyCaptchaImageGetter(KeyCaptcha keyCaptcha, final String[] imageUrl, final LinkedHashMap<String, int[]> coordinates, Browser br, String url) throws Exception {
        this.coordinates = coordinates;
        this.helper = keyCaptcha;
        loadImage(imageUrl);
        handleCoordinates();

        makePieces();
        makeBackground();

        LinkedList<BufferedImage> pieces = new LinkedList<BufferedImage>();
        BufferedImage sampleImg = null;

        for (int i = 1; i < kcImages.length; i++) {
            if (kcImages[i] == null) {
                continue;
            } else if (i == kcSampleImg) {
                sampleImg = kcImages[i];
            } else {
                pieces.add(kcImages[i]);
            }
        }
        keyCaptchaImage = new KeyCaptchaImages(kcImages[0], sampleImg, pieces);
    }

    public KeyCaptchaImages getKeyCaptchaImage() {
        return this.keyCaptchaImage;
    }

    public void handleCoordinates() {
        kcImages = new BufferedImage[coordinates.size()];
    }

    public void loadImage(final String[] imagesUrl) {
        int i = 0;
        IMAGE = new Image[imagesUrl.length];
        File fragmentedPic;
        final Browser dlpic = new Browser();
        helper.prepareBrowser(dlpic, "image/png,image/*;q=0.8,*/*;q=0.5");
        final MediaTracker mt = new MediaTracker(new JLabel());
        for (final String imgUrl : imagesUrl) {
            try {
                // fragmentedPic = Application.getRessource("captchas/" + imgUrl.substring(imgUrl.lastIndexOf("/") + 1));
                fragmentedPic = JDUtilities.getResourceFile("captchas/" + imgUrl.substring(imgUrl.lastIndexOf("/") + 1));
                fragmentedPic.deleteOnExit();
                Browser.download(fragmentedPic, dlpic.openGetConnection(imgUrl));
                /* TODO: replace with ImageProvider.read in future */
                IMAGE[i] = ImageIO.read(fragmentedPic);
                // IMAGE[i] = Toolkit.getDefaultToolkit().getImage(new URL(imgUrl));
            } catch (final IOException e) {
                e.printStackTrace();
            }
            mt.addImage(IMAGE[i], i);
            i++;
        }
        try {
            mt.waitForAll();
        } catch (final InterruptedException ex) {
        }
    }

    private void makeBackground() {
        int curx = 0;
        int cik = 0;
        kcImages[0] = new BufferedImage(450, 160, BufferedImage.TYPE_INT_RGB);
        Graphics go = kcImages[0].getGraphics();
        go.setColor(Color.WHITE);
        go.fillRect(0, 0, 450, 160);
        final int[] bgCoord = coordinates.get("backGroundImage");
        while (cik < bgCoord.length) {
            go.drawImage(IMAGE[1], bgCoord[cik], bgCoord[cik + 1], bgCoord[cik] + bgCoord[cik + 2], bgCoord[cik + 1] + bgCoord[cik + 3], curx, 0, curx + bgCoord[cik + 2], bgCoord[cik + 3], null);
            curx = curx + bgCoord[cik + 2];
            cik = cik + 4;
        }
    }

    private void makePieces() {
        final Object[] key = coordinates.keySet().toArray();
        int pieces = 1;
        for (final Object element : key) {
            if (element.equals("backGroundImage")) {
                continue;
            }
            final int[] imgcs = coordinates.get(element);
            if (imgcs == null | imgcs.length == 0) {
                break;
            }
            final int w = imgcs[1] + imgcs[5] + imgcs[9];
            final int h = imgcs[3] + imgcs[15] + imgcs[27];
            int dX = 0;
            int dY = 0;
            kcImages[pieces] = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics go = kcImages[pieces].getGraphics();
            if (element.equals("kc_sample_image")) {
                kcSampleImg = pieces;
            }
            int sX = 0, sY = 0, sW = 0, sH = 0;
            dX = 0;
            dY = 0;
            for (int cik2 = 0; cik2 < 36; cik2 += 4) {
                sX = imgcs[cik2];
                sY = imgcs[cik2 + 2];
                sW = imgcs[cik2 + 1];
                sH = imgcs[cik2 + 3];
                if (sX + sW > IMAGE[0].getWidth(null) || sY + sH > IMAGE[0].getHeight(null)) {
                    continue;
                }
                if (dX + sW > w || dY + sH > h) {
                    continue;
                }
                if (sW == 0 || sH == 0) {
                    continue;
                }
                // create puzzle piece
                go.drawImage(IMAGE[0], dX, dY, dX + sW, dY + sH, sX, sY, sX + sW, sY + sH, null);
                dX = dX + sW;
                if (dX >= w) {
                    dY = dY + sH;
                    dX = 0;
                }
            }
            pieces += 1;
        }
    }
}