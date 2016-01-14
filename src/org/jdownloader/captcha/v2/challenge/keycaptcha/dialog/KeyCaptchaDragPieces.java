package org.jdownloader.captcha.v2.challenge.keycaptcha.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.MouseInputAdapter;

import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptcha;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaPuzzleChallenge;
import org.jdownloader.updatev2.gui.LAFOptions;

import jd.http.Browser;

class KeyCaptchaDragPieces extends JPanel {
    private static final long         serialVersionUID = 1L;
    private final BufferedImage       image;
    private final MouseAdapter        mouseAdapter;
    private int                       k;
    private KeyCaptchaPuzzleChallenge challenge;
    private KeyCaptcha                helper;
    private Browser                   br;

    public KeyCaptchaDragPieces(final BufferedImage image, final int offset, final boolean sampleImg, final ArrayList<Integer> mouseArray, final KeyCaptchaPuzzleChallenge challenge) {
        // this.image = IconIO.colorRangeToTransparency(image, Color.WHITE, 0.15d);
        this.image = image;
        LAFOptions.applyBackground(LAFOptions.getInstance().getColorForPanelBackground(), this);

        this.challenge = challenge;
        helper = challenge.getHelper();
        br = helper.getBrowser();
        mouseAdapter = new MouseInputAdapter() {
            private Point p1;
            private Point loc;

            private Timer mArrayTimer = new Timer(1000, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    marray(loc);
                }
            });

            @Override
            public void mouseDragged(final MouseEvent e) {
                Point p2 = e.getPoint();
                loc = getLocation();
                loc.translate(p2.x - p1.x, p2.y - p1.y);
                mArrayTimer.setRepeats(false);
                mArrayTimer.start();
                setLocation(loc);
            }

            @Override
            public void mousePressed(final MouseEvent e) {
                p1 = e.getPoint();
                setBorder(BorderFactory.createLineBorder(Color.black));
                // if (!br.getURL().equals(challenge)) {
                // new Thread() {
                // public void run() {
                // helper.sendOnMousePressFeedback();
                // try {
                // br.getPage(helper.getPuzzleData().getMmUrlReq());
                // } catch (IOException e) {
                // e.printStackTrace();
                // }
                // }
                // }.start();
                // }
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                p1 = e.getPoint();
                setBorder(BorderFactory.createEmptyBorder());
            }

            @Override
            public void mouseEntered(final MouseEvent e) {
                mArrayTimer.start();
            }

            @Override
            public void mouseMoved(final MouseEvent e) {
                p1 = e.getPoint();
                loc = getLocation();
                loc.translate(p1.x, p1.y);
                mArrayTimer.setRepeats(false);
                mArrayTimer.start();
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                mArrayTimer.stop();
            }

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
        };

        k = 0;
        setOpaque(false);
        setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        if (!sampleImg) {
            setBounds(offset, offset, image.getWidth(), image.getHeight());
            setLocation(offset, offset);
            setBorder(BorderFactory.createLineBorder(Color.black));
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
            enableEvents(MouseEvent.MOUSE_EVENT_MASK | MouseEvent.MOUSE_MOTION_EVENT_MASK);
        } else {
            setLayout(null);
            setBounds(449 - image.getWidth() - 10, 0, image.getWidth() + 10, image.getHeight() + 10);
            setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));
            setBackground(Color.white);
            k = 5;
        }
    }

    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, k, k, this);
        }
    }
}