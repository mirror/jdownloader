package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;

import jd.controlling.AccountController;
import jd.controlling.IOEQ;

import org.appwork.swing.components.searchcombo.SearchComboBox;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.images.Interpolation;
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
import org.jdownloader.premium.BuyAndAddPremiumAccount;
import org.jdownloader.premium.BuyAndAddPremiumDialogInterface;

public class BuyAction extends AbstractAction {
    /**
     * 
     */
    private static final long   serialVersionUID = 1L;
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
        this.putValue(NAME, _GUI._.settings_accountmanager_buy());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("buy", 16));
    }

    public static String getPreselectedHoster() {
        if ("Europe/Berlin".equalsIgnoreCase(Calendar.getInstance().getTimeZone().getID())) {
            return "uploaded.to";
        } else {
            return "rapidgator.net";
        }
    }

    public void actionPerformed(ActionEvent e) {
        IOEQ.add(new Runnable() {
            public void run() {
                final Collection<LazyHostPlugin> pluginsAll = HostPluginController.getInstance().list();
                final java.util.List<LazyHostPlugin> plugins = new ArrayList<LazyHostPlugin>();
                /* only show plugins with account support */
                for (LazyHostPlugin lhp : pluginsAll) {
                    if (lhp.isPremium()) plugins.add(lhp);
                }
                final LazyHostPlugin[] options = plugins.toArray(new LazyHostPlugin[plugins.size()]);
                LazyHostPlugin plg = HostPluginController.getInstance().get(getPreselectedHoster());
                if (table != null && plg == null) {
                    List<AccountEntry> selection = table.getExtTableModel().getSelectedObjects();
                    if (selection != null && selection.size() > 0) {

                        for (Iterator<?> iterator = plugins.iterator(); iterator.hasNext();) {
                            LazyHostPlugin hostPluginWrapper = (LazyHostPlugin) iterator.next();
                            if (hostPluginWrapper.getDisplayName().equals(selection.get(0).getAccount().getHoster())) {
                                plg = hostPluginWrapper;
                                break;
                            }
                        }
                    }
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

                        protected String getIconConstraints() {
                            // TODO Auto-generated method stub
                            return "gapright 10,gaptop 2,width 32!,height 32!,alignx center, aligny center";
                        }

                        @Override
                        protected JComboBox getComboBox(final Object[] options2) {

                            combo = new SearchComboBox<LazyHostPlugin>(plugins) {

                                /**
								 * 
								 */
                                private static final long serialVersionUID = -7421876925835937449L;

                                @Override
                                protected Icon getIconForValue(LazyHostPlugin value) {
                                    if (value == null) return null;
                                    return DomainInfo.getInstance(value.getDisplayName()).getFavIcon();
                                }

                                @Override
                                protected String getTextForValue(LazyHostPlugin value) {
                                    if (value == null) return null;
                                    return value.getDisplayName();
                                }

                            };

                            final ComboBoxDialog _this = this;
                            combo.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent e) {
                                    Object item = combo.getSelectedItem();
                                    if (item == null) {
                                        _this.setIcon(null);
                                        return;
                                    }
                                    DomainInfo domainInfo = DomainInfo.getInstance(((LazyHostPlugin) item).getDisplayName());
                                    String tld = domainInfo.getTld();
                                    Image ic = null;
                                    if (NewTheme.I().hasIcon("fav/big." + tld)) {
                                        ic = NewTheme.I().getImage("fav/big." + tld, -1);
                                    }
                                    if (ic == null && NewTheme.I().hasIcon("fav/" + tld)) {
                                        ic = NewTheme.I().getImage("fav/" + tld, -1);
                                    }
                                    if (ic != null) {
                                        _this.setIcon(new ImageIcon(IconIO.getScaledInstance(ic, Math.min(ic.getWidth(null), 32), Math.min(ic.getHeight(null), 32), Interpolation.BILINEAR, true)));
                                        return;
                                    } else {
                                        _this.setIcon(domainInfo.getFavIcon());
                                    }
                                }
                            });
                            combo.setSelectedItem(defaultSelection);
                            return combo;
                        }

                    };

                    Dialog.getInstance().showDialog(d);
                    if (d.getReturnValue() < 0) return;
                    LazyHostPlugin buyIt = options[d.getReturnValue()];
                    if (buyIt == null || StringUtils.isEmpty(buyIt.getPremiumUrl())) return;
                    CrossSystem.openURLOrShowMessage(AccountController.createFullBuyPremiumUrl(buyIt.getPremiumUrl(), "accountmanager" + (table == null ? "/context" : "/table")));
                    try {
                        BuyAndAddPremiumAccount dia;
                        UIOManager.I().show(BuyAndAddPremiumDialogInterface.class, dia = new BuyAndAddPremiumAccount(DomainInfo.getInstance(buyIt.getHost()), "accountmanager" + (table == null ? "/context" : "/table")));
                        dia.checkCloseReason();
                    } catch (DialogClosedException e1) {
                        e1.printStackTrace();
                    } catch (DialogCanceledException e1) {
                        e1.printStackTrace();
                    }

                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
            }
        });

    }
}
