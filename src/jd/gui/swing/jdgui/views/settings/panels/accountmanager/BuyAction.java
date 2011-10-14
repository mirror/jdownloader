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

import jd.controlling.IOEQ;
import jd.plugins.Account;
import jd.plugins.hoster.FileSonicCom;

import org.appwork.swing.components.searchcombo.SearchComboBox;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ComboBoxDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public class BuyAction extends AbstractAction {
    /**
     * 
     */
    private static final long   serialVersionUID = 1L;
    private LazyHostPlugin      preSelection     = null;
    private PremiumAccountTable table            = null;

    public BuyAction(PremiumAccountTable table) {
        this.table = table;
        this.putValue(NAME, _GUI._.settings_accountmanager_buy());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("buy", 20));
    }

    public BuyAction() {
        this((LazyHostPlugin) null);
    }

    public BuyAction(LazyHostPlugin hoster) {
        this.preSelection = hoster;
        this.putValue(NAME, _GUI._.settings_accountmanager_buy());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("buy", 16));
    }

    public void actionPerformed(ActionEvent e) {
        IOEQ.add(new Runnable() {
            public void run() {
                final ArrayList<LazyHostPlugin> plugins = HostPluginController.getInstance().list();
                Collections.sort(plugins, new Comparator<LazyHostPlugin>() {
                    public int compare(final LazyHostPlugin a, final LazyHostPlugin b) {
                        return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
                    }
                });
                final LazyHostPlugin[] options = plugins.toArray(new LazyHostPlugin[plugins.size()]);
                LazyHostPlugin plg = preSelection;
                if (table != null && plg == null) {
                    ArrayList<Account> selection = table.getExtTableModel().getSelectedObjects();
                    if (selection != null && selection.size() > 0) {

                        for (Iterator<?> iterator = plugins.iterator(); iterator.hasNext();) {
                            LazyHostPlugin hostPluginWrapper = (LazyHostPlugin) iterator.next();
                            if (hostPluginWrapper.getDisplayName().equals(selection.get(0).getHoster())) {
                                plg = hostPluginWrapper;
                                break;
                            }
                        }
                    }
                }
                if (plg == null) {
                    plg = HostPluginController.getInstance().get(FileSonicCom.class);
                }
                final LazyHostPlugin defaultSelection = plg;
                try {

                    final ComboBoxDialog d = new ComboBoxDialog(0, _GUI._.buyaction_title(), _GUI._.buyaction_message(), options, 0, NewTheme.I().getIcon("buy", 32), _GUI._.buyaction_title_buy_account(), null, null) {
                        private SearchComboBox<LazyHostPlugin> combo;

                        @Override
                        protected void packed() {
                            super.packed();
                            combo.requestFocus();

                        }

                        @Override
                        protected JComboBox getComboBox(final Object[] options2) {

                            combo = new SearchComboBox<LazyHostPlugin>(plugins) {

                                @Override
                                protected Icon getIconForValue(LazyHostPlugin value) {

                                    return isPopupVisible() ? DomainInfo.getInstance(value.getDisplayName()).getFavIcon() : null;
                                }

                                @Override
                                protected String getTextForValue(LazyHostPlugin value) {
                                    return value.getDisplayName();
                                }
                            };
                            final ComboBoxDialog _this = this;
                            combo.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent e) {
                                    _this.setIcon(DomainInfo.getInstance(((LazyHostPlugin) combo.getSelectedItem()).getDisplayName()).getFavIcon());
                                }
                            });
                            combo.setSelectedItem(defaultSelection);

                            return combo;
                        }

                    };

                    Dialog.getInstance().showDialog(d);
                    LazyHostPlugin buyIt = options[d.getReturnValue()];
                    CrossSystem.openURLOrShowMessage(buyIt.getPrototype().getBuyPremiumUrl());
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
            }
        });

    }
}
