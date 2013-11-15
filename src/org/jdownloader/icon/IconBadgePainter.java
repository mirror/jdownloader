package org.jdownloader.icon;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.font.FontRenderContext;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.IconPainter;
import org.appwork.utils.Application;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.images.NewTheme;

public class IconBadgePainter {

    private Image              back;
    private CircledProgressBar m;

    public IconBadgePainter(Image image) {
        back = image;

        m = new CircledProgressBar();
        // m.setIndeterminate(true);
        m.setValueClipPainter(new IconPainter() {

            public void paint(final CircledProgressBar bar, final Graphics2D g2, final Shape shape, final int diameter, final double progress) {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                ((Graphics2D) g2).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
                final Area a = new Area(shape);

                // g2.draw(a);
                g2.setColor(Color.GREEN);
                a.intersect(new Area(new Ellipse2D.Float(-(diameter) / 2, -(diameter) / 2, diameter, diameter)));

                g2.fill(a);

                // g2.setClip(null);

                // g2.draw(shape);
                g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND));
                g2.setColor(Color.DARK_GRAY);
                g2.draw(new Area(new Ellipse2D.Float(-(diameter - 2) / 2, -(diameter - 2) / 2, diameter - 2, diameter - 2)));

            }

            private Dimension dimension;
            {
                dimension = new Dimension(75, 75);
            }

            @Override
            public Dimension getPreferredSize() {
                return dimension;
            }
        });

        m.setNonvalueClipPainter(new IconPainter() {

            public void paint(final CircledProgressBar bar, final Graphics2D g2, final Shape shape, final int diameter, final double progress) {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                ((Graphics2D) g2).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
                final Area a = new Area(shape);

                g2.setColor(Color.WHITE);
                a.intersect(new Area(new Ellipse2D.Float(-(diameter) / 2, -(diameter) / 2, diameter, diameter)));

                g2.fill(a);
                // g2.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND));
                // g2.setColor(Color.GRAY);
                // g2.draw(a);

            }

            private Dimension dimension;
            {
                dimension = new Dimension(75, 75);
            }

            @Override
            public Dimension getPreferredSize() {
                return dimension;
            }
        });
        m.setMaximum(100);
    }

    public static void main(String[] args) {
        Application.setApplication(".jd_home");

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                try {
                    while (true) {
                        try {
                            Dialog.getInstance().showConfirmDialog(0, "title", "msg", new ImageIcon(new IconBadgePainter(NewTheme.I().getImage("logo/jd_logo_128_128", 128)).getImage(23, "23")), null, null);
                        } catch (DialogClosedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (DialogCanceledException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public Image getImage(int percent, String string) {
        // com.apple.eawt.Application.getApplication().setDockIconBadge(DownloadInformations.getInstance().getPercent()
        // + "%");

        // int percent = 0;
        // if (aggn.getTotalBytes() > 0) {
        // percent = (int) ((aggn.getLoadedBytes() * 100) / aggn.getTotalBytes());
        // }

        m.setValue(percent);
        // System.out.println(m.getValue());
        m.setSize(75, 75);

        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice gd = ge.getDefaultScreenDevice();
        /* WARNING: this can cause deadlock under linux and EDT/XAWT */
        final GraphicsConfiguration gc = gd.getDefaultConfiguration();
        final BufferedImage img = gc.createCompatibleImage(back.getWidth(null), back.getHeight(null), Transparency.TRANSLUCENT);
        final Graphics2D g2 = img.createGraphics();
        g2.drawImage(back, 0, 0, null);

        g2.translate(128 - m.getWidth() - 3, 128 - m.getHeight() - 3);

        m.paint(g2);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 30));
        int w = g2.getFontMetrics().stringWidth(string);
        FontRenderContext renderContext = g2.getFontRenderContext();
        Rectangle bounds = g2.getFont().createGlyphVector(renderContext, string).getPixelBounds(null, 0, 0);

        g2.drawString(string, (m.getWidth() - w) / 2, (int) ((bounds.getHeight() + m.getHeight()) / 2));
        // g.fillRect(0, 0, 40, 40);
        g2.dispose();
        return img;
    }
}
