package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.event.ActionEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jd.controlling.IOEQ;

import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.BooleanFilter;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class DefaultRulesAction extends JMenu {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public DefaultRulesAction() {
        super(_GUI._.LinkgrabberFilter_default_rules());
        setIcon(NewTheme.getInstance().getIcon("wizard", 18));

        this.add(new JMenuItem(new DefaultRule(_JDT._.PackagizerSettings_folderbypackage_rule_name(), "folder") {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                IOEQ.add(new Runnable() {

                    @Override
                    public void run() {
                        PackagizerRule folderByPackage = new PackagizerRule();
                        folderByPackage.setMatchAlwaysFilter(new BooleanFilter(true));
                        folderByPackage.setDownloadDestination("<jd:packagename>");
                        folderByPackage.setIconKey("folder");
                        folderByPackage.setName(_JDT._.PackagizerSettings_folderbypackage_rule_name());
                        folderByPackage.setEnabled(false);
                        PackagizerController.getInstance().add(folderByPackage);
                    }
                }, true);
            }

        }));
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