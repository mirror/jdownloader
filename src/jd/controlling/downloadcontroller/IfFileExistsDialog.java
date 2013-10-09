package jd.controlling.downloadcontroller;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

import jd.plugins.DownloadLink;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.uio.UIOManager;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.IfFileExistsAction;
import org.jdownloader.translate._JDT;

public class IfFileExistsDialog extends AbstractDialog<IfFileExistsAction> implements IfFileExistsDialogInterface {

    private String             path;
    private IfFileExistsAction result;
    private String             packagename;

    public String getPackagename() {
        return packagename;
    }

    public String getPackageID() {
        return packageID;
    }

    private JRadioButton skip;
    private JRadioButton overwrite;
    private JRadioButton rename;
    private String       packageID;
    private DownloadLink downloadLink;

    public IfFileExistsDialog(DownloadLink downloadLink) {
        super(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_COUNTDOWN, _JDT._.jd_controlling_SingleDownloadController_askexists_title(), null, null, null);
        //
        this.packagename = downloadLink.getFilePackage().getName();
        this.packageID = downloadLink.getFilePackage().getName() + "_" + downloadLink.getFilePackage().getCreated();
        this.path = downloadLink.getFileOutput();
        this.downloadLink = downloadLink;
        setTimeout(60000);
    }

    @Override
    public ModalityType getModalityType() {
        return ModalityType.MODELESS;
    }

    @Override
    public String getDontShowAgainKey() {
        // returning null causes the dialog to show a checkbox, but the dialog itself does not handle the results
        return null;
    }

    @Override
    protected IfFileExistsAction createReturnValue() {

        if (result != null) org.jdownloader.settings.staticreferences.CFG_GUI.CFG.setLastIfFileExists(result);
        return result;
    }

    protected String getDontShowAgainLabelText() {

        return _GUI._.IfFileExistsDialog_getDontShowAgainLabelText_();
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 0,wrap 1", "", "");
        ExtTextArea txt = new ExtTextArea();
        txt.setLabelMode(true);
        txt.setToolTipText(path);
        File localFile = new File(downloadLink.getFileOutput());
        if (!localFile.exists()) {
            localFile = new File(downloadLink.getFileOutput() + ".part");
        }

        txt.setText(_JDT._.jd_controlling_SingleDownloadController_askexists2(new File(path).getName(), packagename, SizeFormatter.formatBytes(localFile.length()), downloadLink.getDomainInfo().getTld()));
        p.add(txt);

        skip = new JRadioButton(_GUI._.IfFileExistsDialog_layoutDialogContent_skip_());
        skip.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                result = IfFileExistsAction.SKIP_FILE;
            }
        });
        overwrite = new JRadioButton(_GUI._.IfFileExistsDialog_layoutDialogContent_overwrite_());
        overwrite.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                result = IfFileExistsAction.OVERWRITE_FILE;
            }
        });
        rename = new JRadioButton(_GUI._.IfFileExistsDialog_layoutDialogContent_rename_());
        rename.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                result = IfFileExistsAction.AUTO_RENAME;
            }
        });

        // Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(skip);
        group.add(overwrite);
        group.add(rename);
        p.add(new JSeparator(), "pushx,growx");
        p.add(skip, "gapleft 10");
        p.add(overwrite, "gapleft 10");
        p.add(rename, "gapleft 10");
        IfFileExistsAction def = org.jdownloader.settings.staticreferences.CFG_GUI.CFG.getLastIfFileExists();
        if (def == null) def = IfFileExistsAction.SKIP_FILE;
        switch (def) {
        case AUTO_RENAME:
            rename.setSelected(true);
            break;

        case OVERWRITE_FILE:
            overwrite.setSelected(true);
            break;
        default:

            skip.setSelected(true);
        }
        result = def;
        return p;
    }

    public IfFileExistsAction getAction() {
        return result;
    }

    public String getFilePath() {
        return path;
    }

    public IfFileExistsDialogInterface show() {

        return UIOManager.I().show(IfFileExistsDialogInterface.class, this);
    }

}
