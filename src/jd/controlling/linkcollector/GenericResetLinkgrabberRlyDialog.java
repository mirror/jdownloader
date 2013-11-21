package jd.controlling.linkcollector;

import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.controlling.linkcrawler.CrawledLink;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkgrabberSearchField;
import org.jdownloader.images.NewTheme;

public class GenericResetLinkgrabberRlyDialog extends ConfirmDialog {

    private List<CrawledLink> nodesToDelete;
    private boolean           containsOnline;

    private boolean           resetTableSorter;
    private boolean           clearSearchFilter;
    private boolean           clearFilteredLinks;
    private boolean           cancelLinkcrawlerJobs;
    private JCheckBox         cbLinks;
    private JCheckBox         cbSort;
    private JCheckBox         cbSearch;
    private JCheckBox         cbCrawler;
    private JCheckBox         cbFiltered;
    private JLabel            lblLinks;
    private JLabel            lblSort;
    private JLabel            lblSearch;
    private JLabel            lblCrawler;
    private JLabel            lblFiltered;

    public GenericResetLinkgrabberRlyDialog(List<CrawledLink> nodesToDelete, boolean containsOnline, String string, boolean CancelLinkcrawlerJobs, boolean ClearFilteredLinks, boolean ClearSearchFilter, boolean ResetTableSorter) {
        super(UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.literally_are_you_sure(), _GUI._.GenericResetLinkgrabberRlyDialog_GenericResetLinkgrabberRlyDialog_sure_(string), NewTheme.I().getIcon("robot_del", -1), _GUI._.lit_continue(), _GUI._.lit_cancel());
        this.nodesToDelete = nodesToDelete;
        this.containsOnline = containsOnline;
        this.cancelLinkcrawlerJobs = CancelLinkcrawlerJobs;
        this.clearFilteredLinks = ClearFilteredLinks;
        this.clearSearchFilter = ClearSearchFilter;
        this.resetTableSorter = ResetTableSorter;

    }

    public boolean isCancelCrawler() {
        return cbCrawler == null ? cancelLinkcrawlerJobs : cbCrawler.isSelected();
    }

    public boolean isDeleteLinks() {
        return cbLinks == null ? nodesToDelete.size() > 0 : cbLinks.isSelected();
    }

    public boolean isResetSort() {
        return cbSort == null ? resetTableSorter : cbSort.isSelected();
    }

    public boolean isResetSearch() {
        return cbSearch == null ? clearSearchFilter : cbSearch.isSelected();

    }

    public boolean isClearFiltered() {
        return cbFiltered == null ? clearFilteredLinks : cbFiltered.isSelected();
    }

    @Override
    protected void initDoNotShowAgainCheckbox(MigPanel bottom) {
        super.initDoNotShowAgainCheckbox(bottom);
        dontshowagain.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (!dontshowagain.isSelected()) {
                    cbLinks.setEnabled(nodesToDelete.size() > 0);
                    cbSort.setEnabled(LinkGrabberTableModel.getInstance().getSortColumn() != null);
                    cbSearch.setEnabled(!LinkgrabberSearchField.getInstance().isEmpty());
                    cbCrawler.setEnabled(LinkCollector.getInstance().isCollecting());
                    cbFiltered.setEnabled(LinkCollector.getInstance().getfilteredStuffSize() > 0);

                } else {
                    cbLinks.setEnabled(false);
                    cbSort.setEnabled(false);
                    cbSearch.setEnabled(false);
                    cbCrawler.setEnabled(false);
                    cbFiltered.setEnabled(false);

                }

                lblLinks.setEnabled(cbLinks.isEnabled());
                lblSort.setEnabled(cbSort.isEnabled());
                lblSearch.setEnabled(cbSearch.isEnabled());
                lblCrawler.setEnabled(cbCrawler.isEnabled());
                lblFiltered.setEnabled(cbFiltered.isEnabled());
            }
        });
    }

    @Override
    public String getDontShowAgainKey() {
        return "GenericResetLinkgrabberRlyDialog2";
    }

    @Override
    public JComponent layoutDialogContent() {
        JComponent m = super.layoutDialogContent();
        m.setLayout(new MigLayout("ins 0,wrap 2", "[][]", "[]0"));

        m.add(new JSeparator(), "spanx,growx,pushx,newline,gaptop 10");
        m.add(new JLabel(_GUI._.GenericResetLinkgrabberRlyDialog_layoutDialogContent_todo_()), "spanx,");
        m.add(lblLinks = createLabel(_GUI._.ResetLinkGrabberOptionDialog_layoutDialogContent_remove_links2(nodesToDelete.size(), LinkCollector.getInstance().getChildrenCount() - nodesToDelete.size())), "hidemode 3");
        m.add(cbLinks = new JCheckBox(), "hidemode 3");
        m.add(lblSort = createLabel(_GUI._.ResetLinkGrabberOptionDialog_layoutDialogContent_sort()), "hidemode 3");
        m.add(cbSort = new JCheckBox(), "hidemode 3");
        m.add(lblSearch = createLabel(_GUI._.ResetLinkGrabberOptionDialog_layoutDialogContent_search()), "hidemode 3");
        m.add(cbSearch = new JCheckBox(), "hidemode 3");
        m.add(lblCrawler = createLabel(_GUI._.ResetLinkGrabberOptionDialog_layoutDialogContent_interrup_crawler()), "hidemode 3");
        m.add(cbCrawler = new JCheckBox(), "hidemode 3");
        m.add(lblFiltered = createLabel(_GUI._.ResetLinkGrabberOptionDialog_layoutDialogContent_filtered()), "hidemode 3");
        m.add(cbFiltered = new JCheckBox(), "hidemode 3");
        cbLinks.setVisible(nodesToDelete.size() > 0);
        cbSort.setVisible(LinkGrabberTableModel.getInstance().getSortColumn() != null);
        cbSearch.setVisible(!LinkgrabberSearchField.getInstance().isEmpty());
        cbCrawler.setVisible(LinkCollector.getInstance().isCollecting());
        cbFiltered.setVisible(LinkCollector.getInstance().getfilteredStuffSize() > 0);

        lblLinks.setVisible(cbLinks.isVisible());
        lblSort.setVisible(cbSort.isVisible());
        lblSearch.setVisible(cbSearch.isVisible());
        lblCrawler.setVisible(cbCrawler.isVisible());
        lblFiltered.setVisible(cbFiltered.isVisible());

        cbLinks.setSelected(cbLinks.isVisible());

        cbSort.setSelected(cbSort.isEnabled() && resetTableSorter);
        cbSearch.setSelected(cbSearch.isEnabled() && clearSearchFilter);
        cbCrawler.setSelected(cbCrawler.isEnabled() && cancelLinkcrawlerJobs);
        cbFiltered.setEnabled(cbFiltered.isEnabled() && clearFilteredLinks);

        return m;
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setHorizontalAlignment(JLabel.RIGHT);
        return lbl;
    }

}
