package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.ces.CaptchaBrotherhoodService;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.ces.MyJDownloaderService;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.ces.NinekwService;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.searchcombo.SearchComboBox;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class CESSettingsPanel extends JPanel implements SettingsComponent, ActionListener {
    /**
     * 
     */
    private static final long          serialVersionUID = 1L;

    private MigPanel                   card;
    protected CESGenericConfigPanel    configPanel;
    protected List<Pattern>            filter;

    private SearchComboBox<CESService> searchCombobox;

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }

    public Dimension getPreferredScrollableViewportSize() {

        return this.getPreferredSize();
    }

    public CESSettingsPanel() {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][][grow,fill]"));

        searchCombobox = new SearchComboBox<CESService>(null) {

            @Override
            protected Icon getIconForValue(CESService value) {
                if (value == null) return null;
                return value.getIcon(16);
            }

            @Override
            public void onChanged() {
                super.onChanged();

            }

            @Override
            protected String getTextForValue(CESService value) {
                if (value == null) return "";
                return value.getDisplayName();
            }
        };
        searchCombobox.setActualMaximumRowCount(20);
        searchCombobox.addActionListener(this);
        setOpaque(false);

        // left.setBorder(new JTextField().getBorder());
        // selector.setPreferredSize(new Dimension(200, 20000));
        // sp.setBorder(null);

        this.card = new MigPanel("ins 3", "[grow,fill]", "[grow,fill]");
        card.setBorder(new JTextArea().getBorder());
        card.setOpaque(false);

        add(SwingUtils.toBold(new JLabel(_GUI._.CESSettingsPanel_CESSettingsPanel_choose_())), "split 2,shrinkx,sg 1");
        add(searchCombobox, "pushx,growx");
        add(Box.createHorizontalGlue(), "split 2,shrinkx,sg 1");
        add(card, "spanx,pushx,growx");

        ArrayList<CESService> lst = new ArrayList<CESService>();
        lst.add(new MyJDownloaderService());
        lst.add(new NinekwService());
        lst.add(new CaptchaBrotherhoodService());

        searchCombobox.setList(lst);

        String active = JsonConfig.create(GraphicalUserInterfaceSettings.class).getActiveCESConfigPanel();
        int selectIndex = 0;
        if (active != null) {
            for (int i = 0; i < searchCombobox.getModel().getSize(); i++) {
                if (((CESService) searchCombobox.getModel().getElementAt(i)).getClass().getName().equals(active)) {
                    selectIndex = i;
                    break;
                }
            }

        }
        searchCombobox.setSelectedIndex(selectIndex);
        // show((CESService) selector.getModel().getElementAt(selectIndex));

    }

    public String getConstraints() {
        return "wmin 10,height 200:n:n,growy,pushy";
    }

    public boolean isMultiline() {
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        CESService selected = (CESService) searchCombobox.getSelectedItem();

        if (selected != null) {
            JsonConfig.create(GraphicalUserInterfaceSettings.class).setActiveCESConfigPanel(selected.getClass().getName());

            show(selected);
        }
    }

    private void show(final CESService selectedItem) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                card.removeAll();
                if (configPanel != null) {
                    configPanel.setHidden();
                }

                if (selectedItem != null) {
                    configPanel = selectedItem.createPanel();
                    if (configPanel != null) {
                        configPanel.setShown();
                        card.add(configPanel);

                    }

                }
                revalidate();
                card.revalidate();
                card.repaint();
            }
        };
    }

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
