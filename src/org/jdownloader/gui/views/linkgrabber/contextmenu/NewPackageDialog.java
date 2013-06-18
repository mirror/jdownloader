package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.jdgui.views.settings.components.FolderChooser;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;

public class NewPackageDialog extends AbstractDialog<Object> {

    private SelectionInfo<CrawledPackage, CrawledLink> selection;
    private ExtTextField                               tf;
    private FolderChooser                              fc;

    public NewPackageDialog(SelectionInfo<CrawledPackage, CrawledLink> selection) {
        super(0, _GUI._.NewPackageDialog_NewPackageDialog_(), null, null, null);
        this.selection = selection;

    }

    protected int getPreferredWidth() {

        return Math.min(Math.max(tf.getPreferredSize().width, fc.getPreferredSize().width) * 2, getDialog().getParent().getWidth());
    }

    private String getNewName() {
        String defValue = _GUI._.MergeToPackageAction_actionPerformed_newpackage_();
        try {
            defValue = selection.getFirstPackage().getName();
        } catch (Throwable e2) {
            // too many unsafe casts. catch problems - just to be sure
            Log.exception(e2);
        }
        return defValue;
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 0,wrap 2", "[][grow,fill]", "[]");

        p.add(new JLabel(_GUI._.NewPackageDialog_layoutDialogContent_newname_()));
        tf = new ExtTextField();

        tf.setText(getNewName());
        p.add(tf);

        p.add(new JLabel(_GUI._.NewPackageDialog_layoutDialogContent_saveto()));
        fc = new FolderChooser();

        File path = LinkTreeUtils.getRawDownloadDirectory(selection.getContextPackage());
        fc.setText(path.getAbsolutePath());

        p.add(fc);
        return p;
    }

    @Override
    protected void packed() {
        tf.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent e) {
            }

            @Override
            public void focusGained(FocusEvent e) {
                tf.selectAll();
            }
        });

        this.tf.requestFocusInWindow();
        this.tf.selectAll();
    }

    public String getName() {
        return tf.getText();
    }

    public String getDownloadFolder() {
        return fc.getText();
    }
}
