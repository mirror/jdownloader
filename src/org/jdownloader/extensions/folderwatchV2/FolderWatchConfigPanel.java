package org.jdownloader.extensions.folderwatchV2;

import java.net.URL;

import javax.swing.JLabel;
import javax.swing.JTextArea;

import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedConfigTableModel;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedTable;

import org.appwork.storage.simplejson.mapper.ClassCache;
import org.appwork.storage.simplejson.mapper.Getter;
import org.appwork.storage.simplejson.mapper.Setter;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.updatev2.gui.LAFOptions;

public class FolderWatchConfigPanel extends ExtensionConfigPanel<FolderWatchExtension> {

    /**
     *
     */
    private static final long        serialVersionUID = 1L;
    private AdvancedConfigTableModel model;

    public FolderWatchConfigPanel(FolderWatchExtension extension) {
        super(extension);
        JLabel lbl;
        add(SwingUtils.toBold(lbl = new JLabel("THIS EXTENSION IS STILL UNDER CONSTRUCTION. Feel free to test it and to give Feedback.")));
        lbl.setForeground(LAFOptions.getInstance().getColorForErrorForeground());
        add(new AdvancedTable(model = new AdvancedConfigTableModel("FolderWatchConfig") {
            @Override
            public void refresh(String filterText) {
                _fireTableStructureChanged(register(), true);
            }
        }));
        model.refresh("FolderWatch");
        try {
            JTextArea txt = new JTextArea();
            txt.setOpaque(false);
            URL url = Application.getRessourceURL("org/jdownloader/extensions/folderwatchV2/explain.txt");
            final ClassCache cc = ClassCache.getClassCache(CrawlJobStorable.class);
            CrawlJobStorable example = new CrawlJobStorable();
            StringBuilder sb = new StringBuilder();
            sb.append(new String(IO.readStream(-1, url.openStream()), "UTF-8"));
            sb.append("\r\n\r\n### Available Fields:\r\n");
            for (Setter s : cc.getSetter()) {
                try {
                    Getter getter = cc.getGetter(s.getKey());
                    sb.append("\r\n# " + s.getKey() + " Type: " + s.getMethod().getParameterTypes()[0].getSimpleName());
                    if (getter != null) {

                        sb.append("\r\n# " + s.getKey() + " = " + getter.getMethod().invoke(example, new Object[] {}));
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            txt.setText(sb.toString());

            add(txt);
        } catch (Throwable e) {
            getExtension().getLogger().log(e);
        }
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {

    }

}
