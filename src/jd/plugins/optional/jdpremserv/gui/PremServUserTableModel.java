package jd.plugins.optional.jdpremserv.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import jd.plugins.optional.jdpremserv.controlling.UserController;
import jd.plugins.optional.jdpremserv.model.PremServHoster;
import jd.plugins.optional.jdpremserv.model.PremServUser;

import org.appwork.utils.event.BasicEvent;
import org.appwork.utils.event.BasicListener;
import org.appwork.utils.formatter.SizeFormater;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtTextColumn;

public class PremServUserTableModel extends ExtTableModel<PremServUser> {

    /**
     * 
     */
    private static final long serialVersionUID = -161419710915552207L;
    private static final PremServUserTableModel INSTANCE = new PremServUserTableModel();

    /**
     * s
     * 
     * @return the {@link PremServUserTableModel#instance}
     * @see PremServUserTableModel#instance
     */
    protected static PremServUserTableModel getInstance() {
        return INSTANCE;
    }

    private PremServUserTableModel() {
        super("PremServUserTableModel");
        UserController.getInstance().getEventSender().addListener(new BasicListener<PremServUser>() {

            public void onEvent(BasicEvent<PremServUser> event) {
                switch (event.getEventID()) {

                case UserController.LOADED:
                case UserController.USER_ADDED:
                case UserController.REMOVED_USER:
                case UserController.UPDATE:
                    fillData(UserController.getInstance().getPremServUsers());

                }

            }

        });
        fillData(UserController.getInstance().getPremServUsers());
    }

    protected void fillData(final ArrayList<PremServUser> transferables) {

        final ArrayList<PremServUser> tmp = new ArrayList<PremServUser>(transferables);

        final ArrayList<PremServUser> selection = PremServUserTableModel.this.getSelectedObjects();
        tableData = tmp;
        refreshSort();

        fireTableStructureChanged();

        setSelectedObjects(selection);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.rapidshare.rsmanager.gui.components.table.JDTableModel#initColumns()
     */
    @Override
    protected void initColumns() {

        this.addColumn(new ExtTextColumn<PremServUser>("Username", this) {

            @Override
            protected String getStringValue(PremServUser value) {

                return value.getUsername();
            }

        });
        this.addColumn(new ExtTextColumn<PremServUser>("Password", this) {

            @Override
            protected String getStringValue(PremServUser value) {

                return value.getPassword();
            }

        });

        this.addColumn(new ExtTextColumn<PremServUser>("Enabled", this) {

            @Override
            protected String getStringValue(PremServUser value) {

                return value.isEnabled() ? "YES" : "NO";
            }

        });

        this.addColumn(new ExtTextColumn<PremServUser>("Limited Hosters", this) {
            public String getToolTip(PremServUser value) {
                StringBuilder sb = new StringBuilder();
                sb.append("<html>");
                sb.append("<h1>Limit hosters</h1>");
                Entry<String, PremServHoster> next;
                for (Iterator<Entry<String, PremServHoster>> it = value.getHosters().entrySet().iterator(); it.hasNext();) {
                    next = it.next();
                    sb.append("<b>");
                    sb.append(next.getKey());
                    sb.append("</b>: ");
                    sb.append(SizeFormater.formatBytes(next.getValue().getTraffic()));
                    sb.append("<br>");

                }
                sb.append("</html>");
                return sb.toString();
            }

            @Override
            protected String getStringValue(PremServUser value) {

                return value.getHosters() + "";
            }

        });

        this.addColumn(new ExtTextColumn<PremServUser>("Used Traffic", this) {

            public String getToolTip(PremServUser value) {
                StringBuilder sb = new StringBuilder();
                sb.append("<html>");
                sb.append("<h1>Total");
                sb.append(SizeFormater.formatBytes(value.calculateTotalTraffic()));
                sb.append("</h1>");
                Entry<String, Long> next;
                for (Iterator<Entry<String, Long>> it = value.createTrafficStats().entrySet().iterator(); it.hasNext();) {
                    next = it.next();
                    sb.append("<b>");
                    sb.append(next.getKey());
                    sb.append("</b>: ");
                    sb.append(SizeFormater.formatBytes(next.getValue()));
                    sb.append("<br>");

                }
                sb.append("</html>");
                return sb.toString();
            }

            @Override
            protected String getStringValue(PremServUser value) {

                return SizeFormater.formatBytes(value.calculateTotalTraffic());
            }

        });

        this.addColumn(new ExtTextColumn<PremServUser>("Traffic/Month", this) {

            @Override
            protected String getStringValue(PremServUser value) {

                return SizeFormater.formatBytes(value.getAllowedTrafficPerMonth());
            }

        });
    }
}
