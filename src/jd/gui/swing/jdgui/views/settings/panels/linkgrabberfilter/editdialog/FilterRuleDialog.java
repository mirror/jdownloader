package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog;

import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.test.SingleFilterResultTableModel;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.test.TestWaitDialog;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.MigPanel;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class FilterRuleDialog extends ConditionDialog<LinkgrabberFilterRule> {

    private LinkgrabberFilterRule rule;

    public FilterRuleDialog(LinkgrabberFilterRule filterRule) {
        super();
        this.rule = filterRule;
        setTitle(_GUI._.FilterRuleDialog_FilterRuleDialog_title_());

    }

    protected void runTest(String text) {
        TestWaitDialog d;
        try {

            LinkFilterController lfc = LinkFilterController.createEmptyTestInstance();
            LinkgrabberFilterRule rule = getCurrentCopy();
            rule.setEnabled(true);
            lfc.add(rule);

            ArrayList<CrawledLink> ret = Dialog.getInstance().showDialog(d = new TestWaitDialog(text, _GUI._.FilterRuleDialog_runTest_title_(rule.toString()), lfc) {

                @Override
                protected ExtTableModel<CrawledLink> createTableModel() {
                    return new SingleFilterResultTableModel();
                }

            });
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected MigPanel createHeader(String string) {
        MigPanel ret = new MigPanel("ins 0", "[21,fill][][grow,fill]", "[]");
        ret.add(new JSeparator());
        JLabel label;
        ret.add(SwingUtils.toBold(label = new JLabel(string)));
        label.setIcon(NewTheme.I().getIcon("linkgrabber", 14));
        ret.add(new JSeparator());
        return ret;
    }

    public static void main(String[] args) {
        try {
            LookAndFeelController.getInstance().setUIManager();
            Dialog.getInstance().showDialog(new FilterRuleDialog(new LinkgrabberFilterRule()));
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected LinkgrabberFilterRule createReturnValue() {
        return rule;
    }

    @Override
    protected void setReturnmask(boolean b) {
        super.setReturnmask(b);
        if (b) {
            save(this.rule);
        }
    }

    /**
     * Returns a Linkgrabberfilter representing current settings. does NOT save the original one
     * 
     * @return
     */
    private LinkgrabberFilterRule getCurrentCopy() {
        LinkgrabberFilterRule ret = this.rule.duplicate();
        save(ret);
        return ret;
    }

    private void save(LinkgrabberFilterRule rule) {
        rule.setFilenameFilter(getFilenameFilter());
        rule.setHosterURLFilter(getHosterFilter());
        rule.setName(getName());
        rule.setFilesizeFilter(getFilersizeFilter());
        rule.setSourceURLFilter(getSourceFilter());
        rule.setFiletypeFilter(getFiletypeFilter());
        rule.setOnlineStatusFilter(getOnlineStatusFilter());
        rule.setPluginStatusFilter(getPluginStatusFilter());
        rule.setAccept(false);
        rule.setTestUrl(getTxtTestUrl());
        rule.setIconKey(getIconKey());

    }

    private void updateGUI() {

        setIconKey(rule.getIconKey());
        setFilenameFilter(rule.getFilenameFilter());
        setHosterFilter(rule.getHosterURLFilter());
        setName(rule.getName());
        setFilesizeFilter(rule.getFilesizeFilter());
        setOnlineStatusFilter(rule.getOnlineStatusFilter());
        setPluginStatusFilter(rule.getPluginStatusFilter());
        setSourceFilter(rule.getSourceURLFilter());
        setFiletypeFilter(rule.getFiletypeFilter());
        txtTestUrl.setText(rule.getTestUrl());
    }

    protected String getIfText() {
        return _GUI._.FilterRuleDialog_getIfText_();
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel ret = (MigPanel) super.layoutDialogContent();
        // ret.add(createHeader(_GUI._.FilterRuleDialog_layoutDialogContent_then()),
        // "gaptop 10, spanx,growx,pushx");

        updateGUI();
        JScrollPane sp = new JScrollPane(ret);
        sp.setBorder(null);
        return sp;
    }

}
