package jd.gui.swing.jdgui.views.settings.panels.pluginsettings;

import javax.swing.JSeparator;
import javax.swing.JTextArea;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.settings.sidebar.AddonConfig;
import net.miginfocom.swing.MigLayout;

import org.jdownloader.plugins.controller.LazyPlugin;

public class PluginConfigPanel extends SwitchPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private PluginConfigPanel() {
        super(new MigLayout("ins 0, wrap 2", "[][grow,fill]", "[]"));
    }

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {
    }

    public static PluginConfigPanel create(LazyPlugin<?> selectedItem) {
        final AddonConfig cp = AddonConfig.getInstance(selectedItem.getPrototype().getConfig(), "", false);

        // ImageIcon icon = null;
        // if (selectedItem instanceof HostPluginWrapper) {
        // icon = ((HostPluginWrapper) selectedItem).getIconUnscaled();
        // if (icon.getIconWidth() > 32 || icon.getIconHeight() > 32) {
        // icon = JDImage.getScaledImageIcon(icon, 32, 32);
        // }
        // } else {
        // icon = Theme.getIcon("linkgrabber", 32);
        // }
        // gui.addTopHeader(selectedItem.getHost(), icon);

        PluginConfigPanel ret = new PluginConfigPanel() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected void onShow() {
                cp.setShown();
            }

            @Override
            protected void onHide() {
                cp.setHidden();
            }
        };
        String desc = selectedItem.getPrototype().getDescription();
        if (desc != null) {
            JTextArea txt = new JTextArea();
            txt.setEditable(false);
            txt.setLineWrap(true);
            txt.setWrapStyleWord(true);
            txt.setFocusable(false);
            txt.setEnabled(false);

            txt.setText(desc);
            ret.add(txt, "gaptop 0,spanx,growx,pushx,gapbottom 5,wmin 10");
            ret.add(new JSeparator(), "spanx,growx,pushx,gapbottom 5");

        }
        ret.add(cp, "spanx,growx,pushx,growy,pushy");
        return ret;
    }

}
