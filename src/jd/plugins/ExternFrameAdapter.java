package jd.plugins;

import javax.swing.Icon;


import org.jdownloader.extensions.AbstractExtension;

public class ExternFrameAdapter<T extends AbstractExtension<? extends ExtensionConfigInterface>> extends AddonPanel<T> {

    public ExternFrameAdapter(T plg) {
        super(plg);
    }

    @Override
    protected void onDeactivated() {
    }

    @Override
    protected void onActivated() {
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getID() {
        return null;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getTooltip() {
        return null;
    }

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {
    }

}
