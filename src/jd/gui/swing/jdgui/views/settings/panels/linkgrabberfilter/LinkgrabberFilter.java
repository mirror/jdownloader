package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.test.TestWaitDialog;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.exttable.utils.MinimumSelectionObserver;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class LinkgrabberFilter extends JPanel implements SettingsComponent {
    private static final long              serialVersionUID = 6070464296168772795L;

    private FilterTable                    filterTable;
    private ExtButton                      btadd;

    private ExtButton                      btImport;
    private ExtButton                      btExport;

    private ExceptionsTable                exceptionsTable;
    private ExtButton                      btRemove;

    protected AbstractFilterTable          view;

    private MigPanel                       tb;

    private ExtTextField                   txtTest;

    private ExtButton                      btTest;

    private JTabbedPane                    tab;
    private static final LinkgrabberFilter INSTANCE         = new LinkgrabberFilter();

    public static LinkgrabberFilter getInstance() {
        return INSTANCE;
    }

    public void setSelectedIndex(final int i) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                tab.setSelectedIndex(i);

            }
        };

    }

    private LinkgrabberFilter() {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill][]"));

        initComponents();
        setOpaque(false);
        this.add(this.tab, "");

        // this.combobox = new JComboBox(new String[] { _GUI._.LinkgrabberFilter_initComponents_filter_(),
        // _GUI._.LinkgrabberFilter_initComponents_exceptions_() });
        // final ListCellRenderer org = combobox.getRenderer();
        // combobox.setRenderer(new ListCellRenderer() {
        //
        // public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        // JLabel ret = (JLabel) org.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        // ret.setIcon(value == _GUI._.LinkgrabberFilter_initComponents_filter_() ? NewTheme.I().getIcon("false", 20) :
        // NewTheme.I().getIcon("true", 20));
        // return ret;
        // }
        // });
        tab.addTab(_GUI._.LinkgrabberFilter_initComponents_filter__title(), createTab(_GUI._.LinkgrabberFilter_initComponents_filter_(), filterTable));
        tab.addTab(_GUI._.LinkgrabberFilter_initComponents_exceptions_title(), createTab(_GUI._.LinkgrabberFilter_initComponents_exceptions_(), exceptionsTable));
        tab.setTabComponentAt(0, createHeader(_GUI._.LinkgrabberFilter_initComponents_filter_(), _GUI._.LinkgrabberFilter_initComponents_filter__title(), NewTheme.I().getIcon("false", 16)));
        tab.setTabComponentAt(1, createHeader(_GUI._.LinkgrabberFilter_initComponents_exceptions_(), _GUI._.LinkgrabberFilter_initComponents_exceptions_title(), NewTheme.I().getIcon("true", 16)));
        tab.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                view = tab.getSelectedIndex() == 0 ? filterTable : exceptionsTable;
            }
        });
        view = filterTable;
        tab.setSelectedIndex(0);
        txtTest = new ExtTextField();
        txtTest.addFocusListener(new FocusListener() {

            public void focusLost(FocusEvent e) {
            }

            public void focusGained(FocusEvent e) {
                txtTest.selectAll();
            }
        });
        txtTest.setHelpText(_GUI._.LinkgrabberFilter_LinkgrabberFilter_test_help_());
        btTest = new ExtButton(new AppAction() {
            {
                setIconKey("media-playback-start");
                setTooltipText(_GUI._.LinkgrabberFilter_LinkgrabberFilter_test_());
            }

            public void actionPerformed(ActionEvent e) {
                startTest();
            }
        });
        tb = new MigPanel("ins 0", "[grow,fill][]", "[]");
        tb.add(txtTest, "height 24!");
        tb.add(btTest, "height 24!,width 24!");
        add(tb, "spanx, height 24!");

        MigPanel buttonbar = new MigPanel("ins 0, wrap 5", "[][][grow,fill][][]", "[]");
        buttonbar.setOpaque(false);
        add(buttonbar, "spanx, height 26!");
        buttonbar.add(btadd, "height 26!,sg 2");
        buttonbar.add(btRemove, "height 26!,sg 2");
        buttonbar.add(Box.createHorizontalGlue());

        buttonbar.add(btImport, "height 26!,sg 1");
        buttonbar.add(btExport, "height 26!,sg 1");

        exceptionsTable.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(exceptionsTable, btRemove.getAction(), 1) {
            public void valueChanged(final ListSelectionEvent e) {
                boolean en = true;
                for (LinkgrabberFilterRule rule : LinkgrabberFilter.this.exceptionsTable.getModel().getSelectedObjects()) {
                    en &= !rule.isStaticRule();

                }

                if (!en) {
                    btRemove.setToolTipText(_GUI._.PackagizerFilter_valueChanged_disable_static());
                    action.setEnabled(false);
                    return;
                } else {
                    btRemove.setToolTipText(null);
                }

                this.action.setEnabled(LinkgrabberFilter.this.exceptionsTable.getSelectedRowCount() >= this.minSelections);

            }
        });
        filterTable.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(filterTable, btRemove.getAction(), 1) {
            public void valueChanged(final ListSelectionEvent e) {
                boolean en = true;
                for (LinkgrabberFilterRule rule : LinkgrabberFilter.this.filterTable.getModel().getSelectedObjects()) {
                    en &= !rule.isStaticRule();

                }

                if (!en) {
                    btRemove.setToolTipText(_GUI._.PackagizerFilter_valueChanged_disable_static());
                    action.setEnabled(false);
                    return;
                } else {
                    btRemove.setToolTipText(null);
                }

                this.action.setEnabled(LinkgrabberFilter.this.filterTable.getSelectedRowCount() >= this.minSelections);

            }
        });

    }

    private Component createHeader(String tooltip, String lbl, ImageIcon icon) {

        JLabel ret = new JLabel(lbl, icon, JLabel.LEFT);
        ret.setToolTipText(tooltip);
        ret.setOpaque(false);
        JDMouseAdapter ma = new JDMouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, tab);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, tab);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, tab);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, tab);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, tab);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, tab);
            }

            @Override
            public void mouseReleased(MouseEvent e) {

                JDMouseAdapter.forwardEvent(e, tab);

            }

        };
        ret.addMouseMotionListener(ma);
        ret.addMouseListener(ma);
        return ret;
    }

    private Component createTab(String desc, AbstractFilterTable filterTable2) {
        MigPanel ret = new MigPanel("ins 0, wrap 1", "[grow,fill]", "[grow,fill]");

        ret.add(new JScrollPane(filterTable2));
        return ret;
    }

    protected void startTest() {
        TestWaitDialog d;
        try {
            java.util.List<CrawledLink> ret = Dialog.getInstance().showDialog(d = new TestWaitDialog(txtTest.getText(), LinkFilterController.getInstance()));
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }

    }

    public AbstractFilterTable getView() {
        return view;
    }

    private void initComponents() {
        filterTable = new FilterTable(this);
        exceptionsTable = new ExceptionsTable(this);
        btRemove = new ExtButton(new RemoveAction(this));
        btadd = new ExtButton(new NewAction(this));

        tab = new JTabbedPane();

        btImport = new ExtButton(new ImportAction(this));
        btExport = new ExtButton(new ExportAction(this));

    }

    public String getConstraints() {

        return "height 60:n:n,pushy,growy";
    }

    public boolean isMultiline() {
        return false;
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }

    public void update() {
        filterTable.getModel()._fireTableStructureChanged(LinkFilterController.getInstance().listFilters(), true);
        exceptionsTable.getModel()._fireTableStructureChanged(LinkFilterController.getInstance().listExceptions(), true);
    }

    public AbstractFilterTable getTable() {
        return view;
    }
}
