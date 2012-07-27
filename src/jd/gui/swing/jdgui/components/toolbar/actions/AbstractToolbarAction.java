package jd.gui.swing.jdgui.components.toolbar.actions;

import org.jdownloader.actions.AppAction;
import org.jdownloader.images.NewTheme;

public abstract class AbstractToolbarAction extends AppAction {
    private boolean inited = false;

    public AbstractToolbarAction() {
        setIconSizes(32);
        setIconKey(createIconKey());

    }

    public boolean isDefaultVisible() {
        return true;
    }

    public String getID() {
        return getClass().getSimpleName();
    }

    abstract public String createIconKey();

    public String createMnemonic() {
        // we hardly use Mnemonics in JD
        return "-";
    }

    final public AbstractToolbarAction init() {
        if (inited) return this;
        synchronized (this) {
            if (inited) return this;
            inited = true;
            this.doInit();
            return this;
        }

    }

    abstract protected void doInit();

    abstract protected String createAccelerator();

    public Object getValue(String key) {

        if (LARGE_ICON_KEY == (key)) { return NewTheme.I().getIcon(createIconKey(), 24); }
        if (SMALL_ICON == (key)) { return NewTheme.I().getIcon(createIconKey(), 24); }
        if (ACCELERATOR_KEY == key) {
            Object ret = super.getValue(key);
            if (ret == null) {
                setAccelerator(createAccelerator());
            }
            return super.getValue(key);
        }
        if (MNEMONIC_KEY == key || DISPLAYED_MNEMONIC_INDEX_KEY == key) {
            Object ret = super.getValue(key);
            if (ret == null) {
                if (getName() == null) setName(createTooltip());
                setMnemonic(createMnemonic());
            }
            return super.getValue(key);
        }
        if (SHORT_DESCRIPTION == key) { return createTooltip(); }
        return super.getValue(key);
    }

    protected abstract String createTooltip();

    // abstract public String createName();
}
