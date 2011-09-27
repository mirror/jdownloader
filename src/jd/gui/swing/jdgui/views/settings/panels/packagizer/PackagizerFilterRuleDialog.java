package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SpinnerNumberModel;

import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.ConditionDialog;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.FilterPanel;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.ExtSpinner;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.gui.translate._GUI;

public class PackagizerFilterRuleDialog extends ConditionDialog<PackagizerRule> {

    private PackagizerRule rule;
    private JLabel         lblDest;
    private JLabel         lblPriority;
    private JLabel         lblPackagename;
    private JLabel         lblExtract;
    private JLabel         lblChunks;

    public PackagizerFilterRuleDialog(PackagizerRule filterRule) {
        super();
        this.rule = filterRule;

    }

    @Override
    protected PackagizerRule createReturnValue() {
        return rule;
    }

    public static void main(String[] args) {
        try {
            LookAndFeelController.getInstance().setUIManager();
            Dialog.getInstance().showDialog(new PackagizerFilterRuleDialog(new PackagizerRule()));
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setReturnmask(boolean b) {
        super.setReturnmask(b);
        if (b) {
            save();
        }
    }

    private void save() {
        rule.setFilenameFilter(getFilenameFilter());
        rule.setHosterURLFilter(getHosterFilter());
        rule.setName(getName());
        rule.setFilesizeFilter(getFilersizeFilter());
        rule.setSourceURLFilter(getSourceFilter());
        rule.setFiletypeFilter(getFiletypeFilter());

    }

    private void updateGUI() {
        setFilenameFilter(rule.getFilenameFilter());
        setHosterFilter(rule.getHosterURLFilter());
        setName(rule.getName());
        setFilesizeFilter(rule.getFilesizeFilter());
        setSourceFilter(rule.getSourceURLFilter());
        setFiletypeFilter(rule.getFiletypeFilter());

    }

    private FilterPanel  fpDest;
    private FilterPanel  fpPriority;
    private ExtTextField txtPackagename;
    private FilterPanel  fpExtract;
    private ExtSpinner   spChunks;
    private ExtCheckBox  cbDest;
    private ExtCheckBox  cbPriority;
    private ExtCheckBox  cbPackagename;
    private ExtCheckBox  cbExtract;
    private ExtCheckBox  cbChunks;

    @Override
    public JComponent layoutDialogContent() {
        MigPanel ret = (MigPanel) super.layoutDialogContent();
        ret.add(createHeader(_GUI._.PackagizerFilterRuleDialog_layoutDialogContent_then()), "gaptop 10, spanx,growx,pushx");
        lblDest = createLbl(_GUI._.PackagizerFilterRuleDialog_layoutDialogContent_dest());
        lblPriority = createLbl(_GUI._.PackagizerFilterRuleDialog_layoutDialogContent_priority());
        lblPackagename = createLbl(_GUI._.PackagizerFilterRuleDialog_layoutDialogContent_packagename());
        lblExtract = createLbl(_GUI._.PackagizerFilterRuleDialog_layoutDialogContent_extract());
        lblChunks = createLbl(_GUI._.PackagizerFilterRuleDialog_layoutDialogContent_chunks());

        fpDest = new FilterPanel("ins 0", "[grow,fill][]", "[]");
        fpPriority = new FilterPanel("ins 0", "[][][][][][][][]", "[]");
        txtPackagename = new ExtTextField();
        fpExtract = new FilterPanel("ins 0", "[][]", "[]");
        spChunks = new ExtSpinner(new SpinnerNumberModel(2, 1, 20, 1));

        cbDest = new ExtCheckBox(fpDest);
        cbPriority = new ExtCheckBox(fpPriority);
        cbPackagename = new ExtCheckBox(txtPackagename);
        cbExtract = new ExtCheckBox(fpExtract);
        cbChunks = new ExtCheckBox(spChunks);

        ret.add(cbDest);
        ret.add(lblDest, "spanx 2");
        ret.add(fpDest, "spanx");
        link(cbDest, lblDest, fpDest);

        ret.add(cbPriority);
        ret.add(lblPriority, "spanx 2");
        ret.add(fpPriority, "spanx");
        link(cbPriority, lblPriority, fpPriority);

        ret.add(cbPackagename);
        ret.add(lblPackagename, "spanx 2");
        ret.add(txtPackagename, "spanx");
        link(cbPackagename, lblPackagename, txtPackagename);

        ret.add(cbExtract);
        ret.add(lblExtract, "spanx 2");
        ret.add(fpExtract, "spanx");
        link(cbExtract, lblExtract, fpExtract);

        ret.add(cbChunks);
        ret.add(lblChunks, "spanx 2");
        ret.add(spChunks, "spanx");
        link(cbChunks, lblChunks, spChunks);

        updateGUI();
        return ret;
    }

    private void link(final ExtCheckBox cb, JComponent... components) {
        MouseListener ml = new MouseListener() {

            public void mouseReleased(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseClicked(MouseEvent e) {
                cb.setSelected(true);
            }
        };
        for (JComponent c : components)
            c.addMouseListener(ml);
    }

    private JLabel createLbl(String packagizerFilterRuleDialog_layoutDialogContent_dest) {
        JLabel ret = new JLabel(packagizerFilterRuleDialog_layoutDialogContent_dest);
        return ret;
    }

}
