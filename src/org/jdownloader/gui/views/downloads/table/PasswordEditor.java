package org.jdownloader.gui.views.downloads.table;

import java.util.ArrayList;

import javax.swing.JLabel;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class PasswordEditor extends SubMenuEditor {

    private ExtTextField txt;
    private DownloadLink contextObject;

    public PasswordEditor(final DownloadLink contextObject, ArrayList<DownloadLink> links, ArrayList<FilePackage> fps) {
        super();
        setLayout(new MigLayout("ins 2,wrap 1", "[grow,fill]", "[]"));
        setOpaque(false);

        JLabel lbl = getLbl(_GUI._.PasswordEditor__lbl(), NewTheme.I().getIcon("password", 18));
        add(SwingUtils.toBold(lbl));

        this.contextObject = contextObject;
        txt = new ExtTextField();
        txt.setHelpText(_GUI._.PasswordEditor_PasswordEditor_help_());
        add(txt);
        txt.setText(contextObject.getDownloadPassword());
    }

    @Override
    public void reload() {

    }

    @Override
    public void save() {
        contextObject.setDownloadPassword(txt.getText());
    }
}
