package jd.controlling.reconnect.pluginsinc.liveheader;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.net.UnknownHostException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import jd.controlling.reconnect.pluginsinc.liveheader.recoll.BadQueryException;
import jd.controlling.reconnect.pluginsinc.liveheader.recoll.RecollController;
import jd.controlling.reconnect.pluginsinc.liveheader.remotecall.RouterData;
import jd.controlling.reconnect.pluginsinc.liveheader.translate.T;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtTextField;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class SearchScriptDialog extends AbstractDialog<Object> {

    private ExtTextField          routerName;
    private ExtTextField          manufactor;
    private ExtTextField          isp;
    private RouterData            routerData;
    private ExtButton             searchButton;
    private JSeparator            seperator;
    private RouterDataResultTable table;
    private JScrollPane           scrollPane;
    private JLabel                status;
    protected RouterData          selected;

    public SearchScriptDialog(RouterData rd) {
        super(0, T._.SearchScriptDialog(), null, T._.search(), null);
        this.routerData = rd;

    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    protected int getPreferredHeight() {
        int pr = super.getPreferredHeight();
        if (pr > 0) {
            return Math.min(750, pr);
        }
        return Math.min(getRawPreferredSize().height + 20, 750);
    }

    private Component getLabel(String str) {
        JLabel ret = new JLabel(str);
        ret.setEnabled(false);
        return ret;
    }

    @Override
    protected int getPreferredWidth() {
        int pr = super.getPreferredHeight();
        if (pr > 0) {
            return Math.min(1000, pr);
        }
        return 700;
    }

    public static void main(String[] args) throws UnknownHostException, InterruptedException {

        Application.setApplication(".jd_home");
        LookAndFeelController.getInstance().init();

        RouterData rd = new LiveHeaderDetectionWizard() {
            protected void scanRemoteInfo() {
            }
        }.collectRouterDataInfo();
        rd.setIsp(RecollController.getInstance().getIsp());
        UIOManager.I().show(null, new SearchScriptDialog(rd));

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.okButton && StringUtils.equals(T._.search(), okButton.getText())) {
            doSearch();
            return;
        }
        super.actionPerformed(e);
    }

    /**
     * 
     */
    public void doSearch() {
        ProgressGetter pg = new ProgressGetter() {

            @Override
            public void run() throws Exception {
                try {
                    List<RouterData> list = RecollController.getInstance().query(routerName.getText(), manufactor.getText(), isp.getText());

                    updateList(list);
                } catch (BadQueryException e) {
                    UIOManager.I().showErrorMessage(T._.BadQueryException());
                }
            }

            @Override
            public String getString() {
                return null;
            }

            @Override
            public int getProgress() {
                return -1;
            }

            @Override
            public String getLabelString() {
                return null;
            }
        };

        ProgressDialog d = new ProgressDialog(pg, UIOManager.BUTTONS_HIDE_OK, T._.searching(), _GUI._.lit_please_wait(), new AbstractIcon(IconKey.ICON_WAIT, 32), null, null);
        UIOManager.I().show(null, d);
    }

    protected void updateList(final List<RouterData> list) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (list == null || list.size() == 0) {
                    searchButton.setVisible(false);

                    scrollPane.setVisible(false);
                    seperator.setVisible(true);
                    status.setVisible(true);
                    status.setText(T._.nothing_found());
                } else {
                    searchButton.setVisible(true);

                    scrollPane.setVisible(true);
                    seperator.setVisible(true);
                    status.setVisible(true);
                    status.setText(T._.found(list.size()));
                    table.update(list);
                }
                pack();

            }
        };
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 0,wrap 2", "[align right][grow,fill]", "[]");
        p.add(getLabel(T._.routername()));
        p.add(routerName = new ExtTextField());
        p.add(getLabel(T._.manufactor()));
        p.add(manufactor = new ExtTextField());
        p.add(getLabel(T._.isp()));
        p.add(isp = new ExtTextField());
        routerName.setHelpText(T._.routerName_help());

        manufactor.setHelpText(T._.manufactor_help());
        isp.setHelpText(T._.isp_help());
        if (routerData != null) {
            routerName.setText(routerData.getRouterName());
            manufactor.setText(routerData.getManufactor());
            isp.setText(routerData.getIsp().replaceAll("^\\w+\\d+ ", ""));
        }

        p.add(searchButton = new ExtButton(new AppAction() {
            {
                setName(T._.search());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                doSearch();
            }

        }), "skip,alignx right,hidemode 3");
        p.add(seperator = new JSeparator(JSeparator.HORIZONTAL), "spanx,growx,pushx,hidemode 3");
        p.add(status = new JLabel(), "spanx,growx,pushx,hidemode 3");
        p.add(scrollPane = new JScrollPane(table = new RouterDataResultTable() {
            @Override
            protected void onSelectionChanged() {
                super.onSelectionChanged();
                List<RouterData> objects = getModel().getSelectedObjects();
                selected = objects == null || objects.size() == 0 ? null : objects.get(0);
                if (selected == null) {
                    okButton.setText(T._.search());
                } else {
                    okButton.setText(T._.use());

                }
            }

        }), "spanx,growx,pushx,hidemode 3");

        searchButton.setVisible(false);
        seperator.setVisible(false);
        scrollPane.setVisible(false);
        status.setVisible(false);
        return p;
    }

    public RouterData getRouterData() {
        return selected;
    }
}
