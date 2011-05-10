package org.jdownloader.extensions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;

import org.appwork.storage.config.ConfigEventListener;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.settings.AbstractConfigPanel;

public abstract class ExtensionConfigPanel<T extends AbstractExtension> extends AbstractConfigPanel implements ConfigEventListener {

    private static final long serialVersionUID = 1L;

    protected T               extension;

    private Header            header;

    public ExtensionConfigPanel(T plg, boolean clean) {
        super();
        this.extension = plg;
        plg.getSettings().addListener(this);
        if (!clean) {
            header = new Header(plg.getName(), plg.getIcon(32), new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        extension.setEnabled(header.isHeaderEnabled());
                        updateHeaders(header.isHeaderEnabled());
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        Dialog.getInstance().showExceptionDialog("Error", e1.getMessage(), e1);
                    }
                }
            });
            add(header, "spanx,growx,pushx");

            header.setEnabled(plg.isEnabled());
            if (plg.getDescription() != null) {
                addDescription(plg.getDescription());
            }
        }

    }

    public void onConfigValueModified(ConfigInterface config, String key, Object newValue) {
        if ("enabled".equals(key)) {
            updateHeaders((Boolean) newValue);

        }
    }

    public ExtensionConfigPanel(T plg) {
        this(plg, false);

    }

    @Override
    protected void onShow() {
        super.onShow();
        updateHeaders(extension.isEnabled());
    }

    private void updateHeaders(boolean b) {
        for (Component c : this.getComponents()) {
            if (c instanceof Header) {
                ((Header) c).setHeaderEnabled(b);
            }
        }
    }

    @Override
    public ImageIcon getIcon() {
        return extension.getIcon(32);
    }

    @Override
    public String getTitle() {
        return extension.getName();
    }

    public T getExtension() {
        return extension;
    }
}
