package org.jdesktop.swingx.multislider;

import java.awt.Graphics2D;
import javax.swing.JComponent;
import org.jdesktop.swingx.*;


public interface TrackRenderer {
    public JComponent getRendererComponent(JXMultiThumbSlider slider);
}