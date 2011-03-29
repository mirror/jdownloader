package jd.plugins.optional;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import jd.controlling.JDLogger;
import jd.nutils.JDImage;

import org.appwork.utils.Application;
import org.appwork.utils.images.IconIO;

public class ExtensionGuiEnableAction extends AbstractAction {

    private PluginOptional plg;
    private ImageIcon      icon16Enabled;
    private ImageIcon      icon16Disabled;
    private boolean        java15;

    public ExtensionGuiEnableAction(PluginOptional plg) {
        super(plg.getName());
        this.plg = plg;
        java15 = Application.getJavaVersion() < 16000000;
        putValue(SELECTED_KEY, plg.getGUI().isActive());
        icon16Enabled = getCheckBoxImage(20, true);
        icon16Disabled = getCheckBoxImage(20, false);
        updateIcon();

    }

    private void updateIcon() {
        if (isSelected()) {
            putValue(AbstractAction.SMALL_ICON, icon16Enabled);
        } else {
            putValue(AbstractAction.SMALL_ICON, icon16Disabled);
        }

    }

    public void actionPerformed(ActionEvent e) {
        if (java15) {
            this.setSelected(!this.isSelected());
        } else {
            updateIcon();

        }

        plg.getGUI().setActive(!plg.getGUI().isActive());
    }

    public boolean isSelected() {
        final Object value = getValue(SELECTED_KEY);
        return (value == null) ? false : (Boolean) value;
    }

    public void setSelected(final boolean selected) {
        putValue(SELECTED_KEY, selected);
        updateIcon();
    }

    public ImageIcon getCheckBoxImage(final int size, boolean selected) {

        try {

            final Image img = plg.getIcon(32).getImage();
            Image checkBox;

            if (selected) {
                checkBox = IconIO.getImage(ExtensionGuiEnableAction.class.getResource("/org/jdownloader/img/enabled.png"));
            } else {
                checkBox = IconIO.getImage(ExtensionGuiEnableAction.class.getResource("/org/jdownloader/img/disabled.png"));
            }

            BufferedImage ret = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) ret.getGraphics();
            g.drawImage(img, 0, 0, null);
            g.drawImage(checkBox, 0, ret.getHeight() - checkBox.getHeight(null), null);
            // return new ImageIcon(img);
            return new ImageIcon(JDImage.getScaledImage(ret, size, size));

        } catch (Exception e) {

            JDLogger.exception(e);
        }

        return null;
    }
}
