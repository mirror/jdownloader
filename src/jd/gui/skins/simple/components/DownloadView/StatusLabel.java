package jd.gui.skins.simple.components.DownloadView;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdesktop.swingx.renderer.PainterAware;

/**
 * A Renderercomponent, that supports multiple JLabels in one cell. TODO: ignore
 * setter if nothing to change e.g. (setIcon(xy)) should do nothing if xy is
 * already set.
 * 
 * @author coalado
 */
public class StatusLabel extends JPanel implements PainterAware {

    private static final long serialVersionUID = -378709535509849986L;
    public static final int ICONCOUNT = 3;
    public JRendererLabel left;
    public JRendererLabel[] rights = new JRendererLabel[3];
    public JLabel righter;
    private Painter painter;

    public StatusLabel(MigLayout migLayout) {
        super(migLayout);

        add(left = new JRendererLabel());

        for (int i = 0; i < ICONCOUNT; i++) {
            add(rights[i] = new JRendererLabel(), "dock east");
            rights[i].setOpaque(false);
        }

        left.setOpaque(false);
        this.setOpaque(true);

    }

    public void setEnabled(boolean b) {
        for (int i = 0; i < ICONCOUNT; i++) {
            rights[i].setEnabled(b);
        }
    }

    /**
     * Remember, that its always the same panel instance. so we have to reset to
     * defaults before each cellrenderer call.
     * 
     * 
     * @param counter
     */
    public void clearIcons(int counter) {
        for (int i = counter; i < ICONCOUNT; i++) {
            rights[i].setIcon(null);
        }

    }

    public Painter getPainter() {
        return painter;
    }

    public void setPainter(Painter painter) {
        this.painter = painter;
    }

    private void paintPainter(Graphics g) {
        // fail fast: we assume that g must not be null
        // which throws an NPE here instead deeper down the bowels
        // this differs from corresponding core implementation!
        Graphics2D scratch = (Graphics2D) g.create();
        try {
            painter.paint(scratch, this, getWidth(), getHeight());
        } finally {
            scratch.dispose();
        }
    }

    /**
     * PRE: painter != null, isOpaque()
     * 
     * @param g
     */
    protected void paintComponentWithPainter(Graphics2D g) {
        // 1. be sure to fill the background
        // 2. paint the painter
        // by-pass ui.update and hook into ui.paint directly
        if (ui != null) {
            // fail fast: we assume that g must not be null
            // which throws an NPE here instead deeper down the bowels
            // this differs from corresponding core implementation!
            Graphics2D scratchGraphics = (Graphics2D) g.create();
            try {
                scratchGraphics.setColor(getBackground());
                scratchGraphics.fillRect(0, 0, getWidth(), getHeight());
                paintPainter(g);
                ui.paint(scratchGraphics, this);
            } finally {
                scratchGraphics.dispose();
            }
        }
    }

    protected void paintComponent(Graphics g) {
        if (painter != null) {
            // we have a custom (background) painter
            // try to inject if possible
            // there's no guarantee - some LFs have their own background
            // handling elsewhere
            if (isOpaque()) {
                // replace the paintComponent completely
                paintComponentWithPainter((Graphics2D) g);
            } else {
                // transparent apply the background painter before calling super
                paintPainter(g);
                super.paintComponent(g);
            }
        } else {
            // nothing to worry about - delegate to super
            super.paintComponent(g);
        }
    }
}
