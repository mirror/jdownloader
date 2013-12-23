package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.RouterPlugin;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.EDTRunner;

public class ReconnectManager extends MigPanel implements SettingsComponent, ActionListener, GenericConfigEventListener<String> {
    private static final long serialVersionUID = 1L;
    private JComboBox         combobox;

    private MigPanel          card;
    private ExtButton         autoSetupButton;
    private ExtButton         reconnectTestButton;

    public ReconnectManager() {
        super("ins 0,wrap 3", "[grow,fill][]", "");
        initComponents();
        layoutComponents();
        fill();
        org.jdownloader.settings.staticreferences.CFG_RECONNECT.ACTIVE_PLUGIN_ID.getEventSender().addListener(this);
    }

    public void actionPerformed(final ActionEvent e) {
        ReconnectPluginController.getInstance().setActivePlugin((RouterPlugin) this.combobox.getSelectedItem());

    }

    private void layoutComponents() {

        setOpaque(false);
        this.add(this.combobox, "growx, pushx,height 26!");
        this.add(autoSetupButton = new ExtButton(new AutoSetupAction()).setTooltipsEnabled(true), "height 26!");
        this.add(reconnectTestButton = new ExtButton(new ReconnectTestAction()).setTooltipsEnabled(true), "height 26!");
        this.combobox.addActionListener(this);
        add(card, "spanx,pushy,growy");
    }

    private void fill() {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                combobox.setModel(new DefaultComboBoxModel(ReconnectPluginController.getInstance().getPlugins().toArray(new RouterPlugin[] {})));
                combobox.setSelectedItem(ReconnectPluginController.getInstance().getActivePlugin());
                boolean enabled = !"DummyRouterPlugin".equalsIgnoreCase(ReconnectPluginController.getInstance().getActivePlugin().getID());
                reconnectTestButton.setEnabled(enabled);
                setView(ReconnectPluginController.getInstance().getActivePlugin().getGUI());
            }
        };

    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }

    protected void setView(final JComponent gui) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                card.removeAll();
                card.add(gui);
            }
        };

    }

    private void initComponents() {
        this.combobox = new JComboBox();
        final ListCellRenderer org = combobox.getRenderer();
        combobox.setRenderer(new ListCellRenderer() {

            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel ret = (JLabel) org.getListCellRendererComponent(list, ((RouterPlugin) value).getName(), index, isSelected, cellHasFocus);
                ret.setIcon(((RouterPlugin) value).getIcon16());
                return ret;
            }
        });

        this.card = new MigPanel("ins 0", "[grow,fill]", "[grow,fill]");
        card.setOpaque(false);
    }

    public boolean isMultiline() {
        return true;
    }

    public String getConstraints() {
        return "wmin 10,height 60:n:n,pushy,growy";
    }

    public void onConfigValidatorError(KeyHandler<String> keyHandler, String invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<String> keyHandler, String newValue) {
        fill();
    }

}
