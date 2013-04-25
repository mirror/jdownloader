package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jd.controlling.IOEQ;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter.OnlineStatus;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter.OnlineStatusMatchtype;

import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class DefaultRulesAction extends JMenu {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public DefaultRulesAction(boolean defaultAcceptRules) {
        super(_GUI._.LinkgrabberFilter_default_rules());
        setIcon(NewTheme.getInstance().getIcon("wizard", 18));
        if (defaultAcceptRules) {
            /* here are the accept default rules */
            this.add(new JMenuItem(new DefaultRule(_JDT._.LinkFilterSettings_DefaultFilterList_getDefaultValue_(), "error") {

                /**
             * 
             */
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    IOEQ.add(new Runnable() {

                        @Override
                        public void run() {
                            LinkgrabberFilterRule offline = new LinkgrabberFilterRule();
                            offline.setOnlineStatusFilter(new OnlineStatusFilter(OnlineStatusMatchtype.ISNOT, true, OnlineStatus.ONLINE));
                            offline.setName(_JDT._.LinkFilterSettings_DefaultFilterList_getDefaultValue_());
                            offline.setIconKey("error");
                            offline.setAccept(true);
                            offline.setEnabled(true);
                            LinkFilterController.getInstance().add(offline);
                        }
                    }, true);
                }

            }));
        } else {
            /* here are the deny default rules */
        }
    }

    protected abstract class DefaultRule extends AppAction {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public DefaultRule(String name, String iconKey) {
            setName(name);
            this.setIconKey(iconKey);
        }

    }
}
