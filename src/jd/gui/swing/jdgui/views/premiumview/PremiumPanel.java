package jd.gui.swing.jdgui.views.premiumview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.Timer;

import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.gui.swing.components.AccountDialog;
import jd.gui.swing.jdgui.actions.ThreadedAction;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.plugins.Account;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class PremiumPanel extends SwitchPanel implements ActionListener, AccountControllerListener {

    /**
     * 
     */
    private static final long serialVersionUID = -7685744533817989161L;
    private PremiumTable internalTable;
    private JScrollPane scrollPane;
    private Timer Update_Async;
    private boolean visible = false;
    private boolean tablerefreshinprogress = false;
    protected Logger logger = jd.controlling.JDLogger.getLogger();

    public PremiumPanel() {
        super(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[fill,grow]"));
        internalTable = new PremiumTable(new PremiumJTableModel(), this);
        scrollPane = new JScrollPane(internalTable);
        this.add(scrollPane, "cell 0 0");
        Update_Async = new Timer(250, this);
        Update_Async.setInitialDelay(250);
        Update_Async.setRepeats(false);
        AccountController.getInstance().addListener(this);
        initActions();
    }

    private void initActions() {
        new ThreadedAction("action.premiumview.addacc", "gui.images.premium") {

            /**
             * 
             */
            private static final long serialVersionUID = -4407938288408350792L;

            @Override
            public void initDefaults() {
                this.setToolTipText(JDL.L("action.premiumview.addacc", "Clear List"));
            }

            @Override
            public void init() {
            }

            public void threadedActionPerformed(ActionEvent e) {
                AccountDialog.showDialog();
            }
        };
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    @Override
    protected void onHide() {
        AccountController.getInstance().removeListener(this);
        Update_Async.stop();
        visible = false;
    }

    public void fireTableChanged(final boolean fast) {
        if (tablerefreshinprogress && !fast) return;
        new Thread() {
            public void run() {
                if (!fast) tablerefreshinprogress = true;
                this.setName("PremiumPanel: refresh Table");
                try {
                    internalTable.fireTableChanged();
                } catch (Exception e) {
                    logger.severe("TreeTable Exception, complete refresh!");
                    Update_Async.restart();
                }
                if (!fast) tablerefreshinprogress = false;
            }
        }.start();
    }

    @Override
    protected void onShow() {
        AccountController.getInstance().addListener(this);
        fireTableChanged(false);
        visible = true;
        Update_Async.restart();
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == Update_Async) {
            if (visible) fireTableChanged(false);
            return;
        }
    }

    public void onAccountControllerEvent(AccountControllerEvent event) {
        switch (event.getID()) {
        case AccountControllerEvent.ACCOUNT_ADDED:
        case AccountControllerEvent.ACCOUNT_REMOVED:
        case AccountControllerEvent.ACCOUNT_UPDATE:
            Update_Async.restart();
            break;
        default:
            break;
        }
    }

    public boolean vetoAccountGetEvent(String host, Account account) {
        // TODO Auto-generated method stub
        return false;
    }

}
