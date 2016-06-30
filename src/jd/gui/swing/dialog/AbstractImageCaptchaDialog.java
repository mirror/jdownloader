package jd.gui.swing.dialog;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.appwork.utils.URLStream;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.images.Interpolation;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.AbstractCaptchaDialog;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

import jd.captcha.utils.GifDecoder;
import net.miginfocom.swing.MigLayout;

public abstract class AbstractImageCaptchaDialog extends AbstractCaptchaDialog<Object> {

    public static Image[] getGifImages(InputStream openStream) {

        try {
            GifDecoder decoder = new GifDecoder();
            decoder.read(openStream);
            BufferedImage[] ret = new BufferedImage[decoder.getFrameCount()];
            for (int i = 0; i < decoder.getFrameCount(); i++) {
                ret[i] = decoder.getFrame(i);
                // ImageIO.write(ret[i], "png", Application.getResource("img_" +
                // i + ".png"));
            }
            return ret;

        } finally {
            try {
                openStream.close();
            } catch (final Throwable e) {
            }
        }

    }

    /**
     * @param url
     * @return
     */
    public static Image[] getGifImages(URL url) {
        InputStream stream = null;
        try {
            stream = URLStream.openStream(url);
            return getGifImages(stream);
        } catch (IOException e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        } finally {
            try {
                stream.close();
            } catch (final Throwable e) {
            }
        }
        return null;
    }

    int                 fps;

    protected Image[]   images;

    protected Point     offset;
    private int         frame = 0;
    Timer               paintTimer;
    protected Rectangle bounds;
    protected double    scaleFaktor;

    public AbstractImageCaptchaDialog(ImageCaptchaChallenge<?> challenge, int flags, String title, DialogType type, DomainInfo domainInfo, String explain, Image... images) {
        super(challenge, flags, title, type, domainInfo, explain);

        // if we have gif images, but read them as non indexed images, we try to fix this here.
        java.util.List<Image> ret = new ArrayList<Image>();
        if (images != null) {
            for (int i = 0; i < images.length; i++) {
                if (images[i] instanceof BufferedImage) {
                    if (((BufferedImage) images[i]).getColorModel() instanceof IndexColorModel) {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        try {
                            ImageIO.write((BufferedImage) images[i], "gif", os);

                            InputStream is = new ByteArrayInputStream(os.toByteArray());
                            Image[] subImages = getGifImages(is);
                            for (Image ii : subImages) {
                                ret.add(ii);
                            }
                            continue;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                }
                ret.add(images[i]);
            }
        }

        this.images = ret.toArray(new Image[] {});
        fps = 24;

    }

    protected MigLayout getDialogLayout() {
        return new MigLayout("ins 0,wrap 1", "[fill,grow]", "[grow,fill]10[]");
    }

    @Override
    protected JPanel createCaptchaPanel() {
        JPanel iconPanel = new JPanel(new MigLayout("ins 0", "[grow]", "[grow]")) {

            /**
             *
             */
            private static final long serialVersionUID = 1L;
            private Color             col              = (LAFOptions.getInstance().getColorForPanelBackground());

            protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                Composite comp = g2.getComposite();
                try {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
                    super.paintChildren(g);

                } finally {
                    g2.setComposite(comp);
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int insets = CFG_GUI.CFG.isCaptchaDialogBorderAroundImageEnabled() ? 10 : 0;
                BufferedImage scaled = IconIO.getScaledInstance(images[frame], getWidth() - insets, getHeight() - insets, Interpolation.BICUBIC, true);
                scaleFaktor = images[frame].getWidth(null) / (double) scaled.getWidth();
                offset = new Point((getWidth() - scaled.getWidth()) / 2, (getHeight() - scaled.getHeight()) / 2);
                AbstractImageCaptchaDialog.this.bounds = new Rectangle((getWidth() - scaled.getWidth()) / 2, (getHeight() - scaled.getHeight()) / 2, scaled.getWidth(), scaled.getHeight());

                g.setClip(bounds);
                g.drawImage(scaled, (getWidth() - scaled.getWidth()) / 2, (getHeight() - scaled.getHeight()) / 2, col, null);
                paintIconComponent(g, getWidth(), getHeight(), (getWidth() - scaled.getWidth()) / 2, (getHeight() - scaled.getHeight()) / 2, scaled);

            }

        };

        final int size = org.jdownloader.settings.staticreferences.CFG_GUI.CAPTCHA_SCALE_FACTOR.getValue();
        if (size != 100) {

            iconPanel.setPreferredSize(new Dimension((int) (images[0].getWidth(null) * size / 100.0f), (int) (images[0].getHeight(null) * size / 100.0f)));
        } else {
            iconPanel.setPreferredSize(new Dimension(images[0].getWidth(null), images[0].getHeight(null)));
        }
        return iconPanel;
    }

    protected void paintIconComponent(Graphics g, int width, int height, int xOsset, int yOffset, BufferedImage scaled) {
    }

    @Override
    public void dispose() {
        if (paintTimer != null) {
            paintTimer.stop();
        }
        super.dispose();
    }

    abstract protected JComponent createInputComponent();

    @Override
    public JComponent layoutDialogContent() {
        final JComponent ret = super.layoutDialogContent();
        if (images.length > 1) {
            paintTimer = new Timer(1000 / fps, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    frame = (frame + 1) % images.length;
                    ret.repaint();
                }
            });
            paintTimer.setRepeats(true);
            paintTimer.start();

        }
        JComponent b = createInputComponent();
        if (b != null) {
            b.addMouseListener(this);
            b.addMouseMotionListener(this);
            ret.add(b);
        }
        return ret;
    }

    @Override
    protected String createReturnValue() {

        return null;
    }

    public Image[] getImages() {
        return images;
    }

    public Point getOffset() {
        return offset;
    }

    public double getScaleFaktor() {
        return scaleFaktor;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }
}
