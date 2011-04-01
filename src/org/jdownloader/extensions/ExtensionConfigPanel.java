package org.jdownloader.extensions;

import javax.swing.ImageIcon;

public abstract class ExtensionConfigPanel<T extends AbstractExtension> extends AbstractConfigPanel {

    private static final long serialVersionUID = 1L;

    private T                 extension;

    public ExtensionConfigPanel(T plg) {
        super();
        this.extension = plg;
        addTopHeader(plg.getName(), plg.getIcon(32));
        if (plg.getDescription() != null) {
            addDescription(plg.getDescription());
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

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {
    }

    public T getExtension() {
        return extension;
    }
}
