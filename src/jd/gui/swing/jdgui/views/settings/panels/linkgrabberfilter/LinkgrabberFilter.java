package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import jd.controlling.linkcrawler.CrawledLink;
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
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class LinkgrabberFilter extends JPanel implements SettingsComponent {
    private static final long              serialVersionUID = 6070464296168772795L;

    private FilterTable                    filterTable;
    private ExtButton                      btadd;

    private ExtButton                      btImport;
    private ExtButton                      btExport;
    private JComboBox                      combobox;
    private JScrollPane                    card;
    private ExceptionsTable                exceptionsTable;
    private ExtButton                      btRemove;

    protected AbstractFilterTable          view;

    private MigPanel                       tb;

    private ExtTextField                   txtTest;

    private ExtButton                      btTest;
    private static final LinkgrabberFilter INSTANCE         = new LinkgrabberFilter();

    public static LinkgrabberFilter getInstance() {
        return INSTANCE;
    }

    public void setSelectedIndex(final int i) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                combobox.setSelectedIndex(i);
            }
        };

    }

    private LinkgrabberFilter() {
        super(new MigLayout("ins 0,wrap 5", "[grow,fill][][]8[][]", "[24!][grow,fill][]"));

        initComponents();
        setOpaque(false);
        this.add(this.combobox, "growx, pushx,height 26!");
        this.add(btImport, "height 26!,sg 1");
        this.add(btExport, "height 26!,sg 1");
        this.add(btadd, "height 26!,sg 1");
        this.add(btRemove, "height 26!,sg 1");

        add(card, "spanx,pushy,growy");

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
        this.combobox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setView(combobox.getSelectedIndex() == 0 ? filterTable : exceptionsTable);
            }
        });

        exceptionsTable.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(exceptionsTable, btRemove.getAction(), 1));
        filterTable.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(filterTable, btRemove.getAction(), 1));
        setView(filterTable);
    }

    protected void startTest() {
        TestWaitDialog d;
        try {
            ArrayList<CrawledLink> ret = Dialog.getInstance().showDialog(d = new TestWaitDialog(txtTest.getText(), LinkFilterController.getInstance()));
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }

    }

    protected void setView(final AbstractFilterTable gui) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                view = gui;
                card.getViewport().setView(gui);
            }
        };

    }

    public AbstractFilterTable getView() {
        return view;
    }

    private void initComponents() {
        filterTable = new FilterTable(this);
        exceptionsTable = new ExceptionsTable(this);
        btRemove = new ExtButton(new RemoveAction(this));
        btadd = new ExtButton(new NewAction(this));
        this.combobox = new JComboBox(new String[] { _GUI._.LinkgrabberFilter_initComponents_filter_(), _GUI._.LinkgrabberFilter_initComponents_exceptions_() });
        final ListCellRenderer org = combobox.getRenderer();
        combobox.setRenderer(new ListCellRenderer() {

            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel ret = (JLabel) org.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                ret.setIcon(value == _GUI._.LinkgrabberFilter_initComponents_filter_() ? NewTheme.I().getIcon("false", 20) : NewTheme.I().getIcon("true", 20));
                return ret;
            }
        });
        btImport = new ExtButton(new ImportAction(this));
        btExport = new ExtButton(new ExportAction(this));
        this.card = new JScrollPane();

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

        filterTable.getExtTableModel()._fireTableStructureChanged(LinkFilterController.getInstance().listFilters(), true);
        exceptionsTable.getExtTableModel()._fireTableStructureChanged(LinkFilterController.getInstance().listExceptions(), true);
    }

    public AbstractFilterTable getTable() {
        return view;
    }
}
