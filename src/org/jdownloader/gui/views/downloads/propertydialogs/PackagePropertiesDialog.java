package org.jdownloader.gui.views.downloads.propertydialogs;

import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.plugins.FilePackage;

import org.appwork.app.gui.MigPanel;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class PackagePropertiesDialog extends AbstractDialog<Object> {

    public PackagePropertiesDialog(FilePackage fp) {
        super(0, _GUI._.PackagePropertiesDialog_PackagePropertiesDialog(fp.getName()), null, null, null);
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 0", "[]", "[]");
        JLabel icon = new JLabel(NewTheme.I().getIcon("archive", 64));
        p.add(icon);

        return p;
    }

}
