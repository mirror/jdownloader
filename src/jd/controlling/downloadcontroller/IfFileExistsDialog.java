package jd.controlling.downloadcontroller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

import jd.SecondLevelLaunch;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.IfFileExistsAction;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
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

    public IfFileExistsDialog(String filepath, String packagename, String packageID) {
        super(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | Dialog.LOGIC_COUNTDOWN, _JDT._.jd_controlling_SingleDownloadController_askexists_title(), null, null, null);
        //
        this.packagename = packagename;
        this.packageID = packageID;
        this.path = filepath;
        setCountdownTime(60);

    }

    @Override
    public String getDontShowAgainKey() {
        // returning null causes the dialog to show a checkbox, but the dialog itself does not handle the results
        return null;
    }

    @Override
    protected IfFileExistsAction createReturnValue() {
        if (isDontShowAgainSelected()) {
            if (result != null) CFG_GENERAL.IF_FILE_EXISTS_ACTION.setValue(result);
        }

        if (result != null) org.jdownloader.settings.staticreferences.CFG_GUI.CFG.setLastIfFileExists(result);
        return result;
    }

    public static void main(String[] args) {

        try {
            Application.setApplication(".jd_home");
            SecondLevelLaunch.statics();

            LookAndFeelController.getInstance().setUIManager();

            IfFileExistsAction res = Dialog.getInstance().showDialog(new IfFileExistsDialog("c:/apfelbaum.txt", "obst", "z87fdghfdfdjg38fd_3"));
            System.out.println(res);
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }

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
        txt.setText(_JDT._.jd_controlling_SingleDownloadController_askexists(new File(path).getName(), packagename));
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

}
