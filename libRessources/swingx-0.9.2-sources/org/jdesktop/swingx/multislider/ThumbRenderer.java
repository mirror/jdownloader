
package org.jdesktop.swingx.multislider;

import java.awt.Graphics2D;
import javax.swing.JComponent;
import org.jdesktop.swingx.*;

public interface ThumbRenderer {
    public JComponent getThumbRendererComponent(JXMultiThumbSlider slider, int index, boolean selected);
}
