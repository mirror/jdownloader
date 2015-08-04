package org.jdownloader.captcha.v2.challenge.keycaptcha.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import org.appwork.utils.images.IconIO;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.updatev2.gui.LAFOptions;

class KeyCaptchaDrawBackgroundPanel extends JPanel {
    private static final long   serialVersionUID = 1L;
    private final BufferedImage image;

    public KeyCaptchaDrawBackgroundPanel(final BufferedImage image) {
        this.image = IconIO.colorRangeToTransparency(image, Color.WHITE, 0.15d);

        SwingUtils.setOpaque(this, true);
        LAFOptions.applyBackground(LAFOptions.getInstance().getColorForPanelBackground(), this);
        setBounds(0, 0, image.getWidth(), image.getHeight());
        setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
    }

    @Override
    public Dimension getPreferredSize() {
        if (image != null) {
            return new Dimension(image.getWidth(), image.getHeight());
        } else {
            return super.getPreferredSize();
        }
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 0, 0, this);
        }
    }
}