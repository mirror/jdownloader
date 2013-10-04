package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.HosterOrderPanel;
import net.miginfocom.swing.MigLayout;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class AccountManager extends SwitchPanel implements SettingsComponent {
    private static final long serialVersionUID = 1473756660999062848L;

    private JTabbedPane       tab;

    private HosterOrderPanel  hosterOrderPanel;

    protected boolean         shown;

    private AccountListPanel  accountListPanel;

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

    /**
     * Create a new instance of AccountManager. This is a singleton class. Access the only existing instance by using {@link #getInstance()}
     * .
     * 
     * @param accountManagerSettings
     */
    public AccountManager(AccountManagerSettings accountManagerSettings) {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill]"));
        setOpaque(false);

        tab = new JTabbedPane();

        hosterOrderPanel = new HosterOrderPanel(this);
        accountListPanel = new AccountListPanel(this);
        tab.addTab(_GUI._.AccountManager_AccountManager_accounts_(), accountListPanel);
        tab.addTab(_GUI._.AccountManager_AccountManager_hosterorder_(), hosterOrderPanel);
        tab.setTabComponentAt(0, createHeader(_GUI._.AccountManager_AccountManager_accounts_tt(), _GUI._.AccountManager_AccountManager_accounts_(), NewTheme.I().getIcon("list", 16)));
        tab.setTabComponentAt(1, createHeader(_GUI._.AccountManager_AccountManager_hosterorder_tt(), _GUI._.AccountManager_AccountManager_hosterorder_(), NewTheme.I().getIcon("order", 16)));
        //
        tab.addChangeListener(new ChangeListener() {

            private Component last = null;

            public void stateChanged(ChangeEvent e) {

                if (last != null && last instanceof SwitchPanel) {
                    ((SwitchPanel) last).setHidden();

                }
                Component newComp = tab.getSelectedComponent();

                if (newComp != null && newComp instanceof SwitchPanel) {
                    ((SwitchPanel) newComp).setShown();

                }
                last = newComp;
            }

        });
        accountManagerSettings.getBroadcaster().addListener(new SwitchPanelListener() {

            @Override
            public void onPanelEvent(SwitchPanelEvent event) {
                if (event.getEventID() == SwitchPanelEvent.ON_SHOW) {
                    shown = true;
                } else if (event.getEventID() == SwitchPanelEvent.ON_HIDE) {
                    shown = false;
                }
            }
        }, true);

        add(tab);

    }

    public boolean isShown() {
        return shown;
    }

    public void setEnabled(boolean enabled) {
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }

    public String getConstraints() {
        return "wmin 10,height 60:n:n,growy,pushy";
    }

    public boolean isMultiline() {
        return false;
    }

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {

    }

}
