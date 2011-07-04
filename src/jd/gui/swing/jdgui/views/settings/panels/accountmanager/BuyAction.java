package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComboBox;

import jd.HostPluginWrapper;
import jd.PluginWrapper;
import jd.controlling.IOEQ;
import jd.plugins.Account;
import jd.plugins.hoster.FileSonicCom;
import jd.utils.JDUtilities;

import org.appwork.swing.components.searchcombo.SearchComboBox;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ComboBoxDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class BuyAction extends AbstractAction {
    /**
     * 
     */
    private static final long   serialVersionUID = 1L;
    // private PremiumAccountTable table;
    private HostPluginWrapper   preSelection;
    private PremiumAccountTable table;

    public BuyAction(HostPluginWrapper hoster, PremiumAccountTable table) {
        this.preSelection = hoster;
        this.table = table;
        this.putValue(NAME, _GUI._.settings_accountmanager_buy());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("buy", ActionColumn.SIZE));
    }

    public void actionPerformed(ActionEvent e) {

        final ArrayList<HostPluginWrapper> plugins = JDUtilities.getPremiumPluginsForHost();
        Collections.sort(plugins, new Comparator<HostPluginWrapper>() {
            public int compare(final HostPluginWrapper a, final HostPluginWrapper b) {
                return a.getHost().compareToIgnoreCase(b.getHost());
            }
        });
        final HostPluginWrapper[] options = plugins.toArray(new HostPluginWrapper[plugins.size()]);
        PluginWrapper plg = preSelection;

        if (table != null) {
            ArrayList<Account> selection = table.getExtTableModel().getSelectedObjects();
            if (plg == null && selection != null && selection.size() > 0) {

                for (Iterator<?> iterator = plugins.iterator(); iterator.hasNext();) {
                    HostPluginWrapper hostPluginWrapper = (HostPluginWrapper) iterator.next();
                    if (hostPluginWrapper.getHost().equals(selection.get(0).getHoster())) {
                        plg = hostPluginWrapper;
                        break;
                    }
                }
            }
        }
        if (plg == null) {
            plg = HostPluginWrapper.getWrapper(FileSonicCom.class.getName());
        }
        final PluginWrapper defaultSelection = plg;
        try {

            final ComboBoxDialog d = new ComboBoxDialog(0, _GUI._.buyaction_title(), _GUI._.buyaction_message(), options, 0, NewTheme.I().getIcon("buy", 32), _GUI._.buyaction_title_buy_account(), null, null) {
                private SearchComboBox<HostPluginWrapper> combo;

                @Override
                protected void packed() {
                    super.packed();
                    combo.requestFocus();

                }

                @Override
                protected JComboBox getComboBox(final Object[] options2) {

                    combo = new SearchComboBox<HostPluginWrapper>(plugins, IOEQ.TIMINGQUEUE) {

                        @Override
                        protected Icon getIcon(HostPluginWrapper value) {

                            return isPopupVisible() ? value.getIconScaled() : null;
                        }

                        @Override
                        protected String getText(HostPluginWrapper value) {
                            return value.getHost();
                        }
                    };
                    final ComboBoxDialog _this = this;
                    combo.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            _this.setIcon(NewTheme.I().getScaledInstance(((HostPluginWrapper) combo.getSelectedItem()).getIconUnscaled(), 32));
                        }
                    });
                    combo.setSelectedItem(defaultSelection);

                    return combo;
                }

            };

            Dialog.getInstance().showDialog(d);
            HostPluginWrapper buyIt = options[d.getReturnValue()];
            CrossSystem.openURLOrShowMessage(buyIt.getPlugin().getBuyPremiumUrl());
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}
