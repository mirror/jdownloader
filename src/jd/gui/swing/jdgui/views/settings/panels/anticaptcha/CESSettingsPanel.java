package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.ces.CaptchaBrotherhoodService;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.ces.MyJDownloaderService;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.ces.NinekwService;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.jdownloader.updatev2.gui.LAFOptions;

public class CESSettingsPanel extends JPanel implements SettingsComponent {
    /**
     * 
     */
    private static final long       serialVersionUID = 1L;

    // private MigPanel card;
    protected CESGenericConfigPanel configPanel;
    protected List<Pattern>         filter;

    private JTabbedPane             tab;

    // private SearchComboBox<CESService> searchCombobox;

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }

    public Dimension getPreferredScrollableViewportSize() {

        return this.getPreferredSize();
    }

    public CESSettingsPanel() {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill]"));

        // searchCombobox = new SearchComboBox<CESService>(null) {
        //
        // @Override
        // protected Icon getIconForValue(CESService value) {
        // if (value == null) return null;
        // return value.getIcon(16);
        // }
        //
        // @Override
        // public void onChanged() {
        // super.onChanged();
        //
        // }
        //
        // @Override
        // protected String getTextForValue(CESService value) {
        // if (value == null) return "";
        // return value.getDisplayName();
        // }
        // };
        // searchCombobox.setActualMaximumRowCount(20);
        // searchCombobox.addActionListener(this);
        setOpaque(false);

        // left.setBorder(new JTextField().getBorder());
        // selector.setPreferredSize(new Dimension(200, 20000));
        // sp.setBorder(null);

        // this.card = new MigPanel("ins 3", "[grow,fill]", "[grow,fill]");
        // card.setBorder(new JTextArea().getBorder());
        // card.setOpaque(false);
        tab = new JTabbedPane();

        // add(searchCombobox, "pushx,growx");
        // add(Box.createHorizontalGlue(), "split 2,shrinkx,sg 1");
        add(tab);

        add(new MyJDownloaderService());
        add(new NinekwService());
        add(new CaptchaBrotherhoodService());
        tab.addChangeListener(new ChangeListener() {

            private int lastSelectedIndex = 0;

            @Override
            public void stateChanged(ChangeEvent e) {
                try {
                    int newSelected = tab.getSelectedIndex();
                    if (newSelected != lastSelectedIndex) {

                        CESGenericConfigPanel old = (CESGenericConfigPanel) ((ScrollablePanel) ((JScrollPane) tab.getComponentAt(lastSelectedIndex)).getViewport().getView()).getPanel();
                        CESGenericConfigPanel newC = (CESGenericConfigPanel) ((ScrollablePanel) ((JScrollPane) tab.getComponentAt(newSelected)).getViewport().getView()).getPanel();

                        old.setHidden();
                        newC.setShown();
                        lastSelectedIndex = newSelected;

                    }
                } catch (Exception e1) {
                    // too much casting.. let's be sure and catch Exceptions here.
                    JDGui.getInstance().getLogger().log(e1);
                }
            }

        });
        // searchCombobox.setList(lst);

        // String active = JsonConfig.create(GraphicalUserInterfaceSettings.class).getActiveCESConfigPanel();
        // int selectIndex = 0;
        // if (active != null) {
        // for (int i = 0; i < searchCombobox.getModel().getSize(); i++) {
        // if (((CESService) searchCombobox.getModel().getElementAt(i)).getClass().getName().equals(active)) {
        // selectIndex = i;
        // break;
        // }
        // }
        //
        // }
        // searchCombobox.setSelectedIndex(selectIndex);
        // show((CESService) selector.getModel().getElementAt(selectIndex));

    }

    public static class ScrollablePanel extends MigPanel implements Scrollable {

        private CESGenericConfigPanel panel;

        public CESGenericConfigPanel getPanel() {
            return panel;
        }

        public ScrollablePanel(CESGenericConfigPanel panel) {
            super("ins 0", "[grow,fill]", "[grow,fill]");
            add(panel);
            setOpaque(false);
            LAFOptions.getInstance().applyPanelBackground(this);
            this.panel = panel;
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return ((orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width) - 10;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

    }

    private void add(CESService service) {
        JScrollPane sp;
        CESGenericConfigPanel panel = service.createPanel();

        sp = new JScrollPane(new ScrollablePanel(panel));
        // sp.setBackground(Color.RED);
        // sp.setOpaque(true);
        LAFOptions.getInstance().applyPanelBackground(panel);
        sp.setViewportBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tab.addTab(service.getDisplayName(), service.getIcon(18), sp, service.getDescription());
    }

    public String getConstraints() {
        return "wmin 10,height 200:n:n,growy,pushy";
    }

    public boolean isMultiline() {
        return true;
    }

    // private void show(final CESService selectedItem) {
    // new EDTRunner() {
    //
    // @Override
    // protected void runInEDT() {
    // card.removeAll();
    // if (configPanel != null) {
    // configPanel.setHidden();
    // }
    //
    // if (selectedItem != null) {
    // configPanel = selectedItem.createPanel();
    // if (configPanel != null) {
    // configPanel.setShown();
    // card.add(configPanel);
    //
    // }
    //
    // }
    // revalidate();
    // card.revalidate();
    // card.repaint();
    // }
    // };
    // }

    public void setHidden() {
        if (configPanel != null) {
            configPanel.setHidden();
        }
    }

    public void setShown() {
        if (configPanel != null) {
            configPanel.setShown();
        }
    }

}
