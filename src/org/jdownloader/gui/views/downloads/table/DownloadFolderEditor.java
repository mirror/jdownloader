package org.jdownloader.gui.views.downloads.table;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.Dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.Dialog.FileChooserType;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class DownloadFolderEditor extends SubMenuEditor {

    private ExtTextField txt;
    private DownloadLink contextObject;
    private JButton      bt;

    public DownloadFolderEditor(final DownloadLink contextObject, ArrayList<DownloadLink> links, ArrayList<FilePackage> fps) {
        super();
        setLayout(new MigLayout("ins 2,wrap 2", "[grow,fill][]", "[]"));
        setOpaque(false);

        JLabel lbl = getLbl(_GUI._.DownloadFolderEditor__lbl(), NewTheme.I().getIcon("folder", 18));
        add(SwingUtils.toBold(lbl), "spanx");

        this.contextObject = contextObject;
        txt = new ExtTextField();
        txt.setEditable(false);
        bt = new JButton(_GUI._.DownloadFolderEditor_DownloadFolderEditor_browse_());
        bt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    closeMenu();
                    File[] files = Dialog.getInstance().showFileChooser("chooseDownloadFolder", _GUI._.DownloadFolderEditor__lbl(), FileChooserSelectionMode.DIRECTORIES_ONLY, null, false, FileChooserType.SAVE_DIALOG, new File(contextObject.getFilePackage().getDownloadDirectory()));
                    if (files != null && files.length == 1) {
                        contextObject.getFilePackage().setDownloadDirectory(files[0].getAbsolutePath());
                    }
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                }

            }
        });
        add(txt);
        add(bt);

    }

    @Override
    public void reload() {
        // txt.setText(contextObject.getFilePackage().getDownloadDirectory());
    }

    @Override
    public void save() {

    }
}
