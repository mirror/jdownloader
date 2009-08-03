//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.settings.panels.premium;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.Timer;

import jd.config.Configuration;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.gui.UserIO;
import jd.gui.swing.components.AccountDialog;
import jd.gui.swing.jdgui.actions.ThreadedAction;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.gui.swing.jdgui.views.toolbar.ViewToolbar;
import jd.nutils.JDFlags;
import jd.plugins.Account;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class Premium extends ConfigPanel implements ActionListener, AccountControllerListener {

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.premium.Premium.";

    private static final long serialVersionUID = -7685744533817989161L;
    private PremiumTable internalTable;
    private JScrollPane scrollPane;
    private Timer Update_Async;
    private boolean visible = false;
    private boolean tablerefreshinprogress = false;
    protected Logger logger = jd.controlling.JDLogger.getLogger();

    public Premium(Configuration configuration) {

        super();
        initPanel();
        load();

    }

    public String getBreadcrum() {
        return JDL.L(this.getClass().getName() + ".breadcrum", this.getClass().getSimpleName());
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "title", "Premium");
    }

    public void initPanel() {
        // super(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[fill,grow]"));
        panel.setLayout(new MigLayout("ins 5,wrap 1", "[fill,grow]", "[fill,grow]"));
        initPanel(panel);
        JTabbedPane tabbed = new JTabbedPane();
        //        
        tabbed.setOpaque(false);
        tabbed.add(getBreadcrum(), panel);
        this.add(tabbed);
    }

    private void initPanel(JPanel panel) {

        internalTable = new PremiumTable(new PremiumJTableModel(), this);
        scrollPane = new JScrollPane(internalTable);

        Update_Async = new Timer(250, this);
        Update_Async.setInitialDelay(250);
        Update_Async.setRepeats(false);
        AccountController.getInstance().addListener(this);
        initActions();

        ViewToolbar vt = new ViewToolbar() {
            public void setDefaults(int i, AbstractButton ab) {
                ab.setForeground(new JLabel().getForeground());

            }
        };
        vt.setContentPainted(false);
        vt.setList(new String[] { "action.premiumview.addacc", "action.premiumview.removeacc" });

        panel.add(vt, "dock north");
        panel.add(scrollPane);
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        saveConfigEntries();

    }

    private void initActions() {
        new ThreadedAction("action.premiumview.addacc", "gui.images.premium") {

            /**
             * 
             */
            private static final long serialVersionUID = -4407938288408350792L;

            @Override
            public void initDefaults() {
                this.setToolTipText(JDL.L("action.premiumview.addacc.tooltip", "Add a new Account"));
            }

            @Override
            public void init() {
            }

            public void threadedActionPerformed(ActionEvent e) {
                AccountDialog.showDialog();
            }
        };
        new ThreadedAction("action.premiumview.removeacc", "gui.images.delete") {

            /**
             * 
             */
            private static final long serialVersionUID = -4407938288408350792L;

            @Override
            public void initDefaults() {
                this.setToolTipText(JDL.L("action.premiumview.removeacc.tooltip", "Remove selected Account(s)"));
            }

            @Override
            public void init() {
            }

            public void threadedActionPerformed(ActionEvent e) {
                ArrayList<Account> accs = internalTable.getSelectedAccounts();
                if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(0, JDL.L("action.premiumview.removeacc.ask", "Remove selected ") + " (" + JDL.LF("action.premiumview.removeacc.accs", "%s Account(s)", accs.size()) + ")"), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {
                    for (Account acc : accs) {
                        AccountController.getInstance().removeAccount((String) null, acc);
                    }
                }
            }
        };
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    @Override
    public void onHide() {
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
    public void onShow() {
        AccountController.getInstance().addListener(this);
        visible = true;
        fireTableChanged(true);
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
