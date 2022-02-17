package jd.gui.swing.jdgui.views.settings.panels.extensionmanager;

import jd.gui.swing.jdgui.BasicJDTable;

import org.jdownloader.extensions.OptionalExtension;

public class OptionalExtensionsTable extends BasicJDTable<OptionalExtension> {
    /**
     *
     */
    private static final long serialVersionUID = 1153823766916158314L;

    public OptionalExtensionsTable() {
        super(new OptionalExtensionsTabelModel());
        setSearchEnabled(true);
        this.setDragEnabled(false);
    }
}
