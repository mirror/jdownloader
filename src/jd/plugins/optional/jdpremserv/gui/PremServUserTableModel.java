package jd.plugins.optional.jdpremserv.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import jd.plugins.optional.jdpremserv.controlling.UserController;
import jd.plugins.optional.jdpremserv.model.PremServHoster;
import jd.plugins.optional.jdpremserv.model.PremServUser;

import org.appwork.utils.event.BasicEvent;
import org.appwork.utils.event.BasicListener;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtCheckColumn;
import org.appwork.utils.swing.table.columns.ExtTextColumn;

public class PremServUserTableModel extends ExtTableModel<PremServUser> {

    private static final long                   serialVersionUID = -161419710915552207L;
    private static final PremServUserTableModel INSTANCE         = new PremServUserTableModel();

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

    @Override
    protected void initColumns() {

        this.addColumn(new ExtTextColumn<PremServUser>("Username", this) {

            private static final long serialVersionUID = 6114502185773645379L;

            @Override
            protected String getStringValue(PremServUser value) {
                return value.getUsername();
            }

        });
        this.addColumn(new ExtTextColumn<PremServUser>("Password", this) {

            private static final long serialVersionUID = -5058123494364026340L;

            @Override
            protected String getStringValue(PremServUser value) {
                return value.getPassword();
            }

        });

        this.addColumn(new ExtCheckColumn<PremServUser>("Enabled", this) {

            private static final long serialVersionUID = -3601520285751677052L;

            @Override
            protected boolean getBooleanValue(PremServUser value) {
                return value.isEnabled();
            }

            @Override
            protected void setBooleanValue(boolean value, PremServUser object) {
            }

        });

        this.addColumn(new ExtTextColumn<PremServUser>("Limited Hosters", this) {

            private static final long serialVersionUID = 9056465387709191600L;

            @Override
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
                    sb.append(SizeFormatter.formatBytes(next.getValue().getTraffic()));
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

            private static final long serialVersionUID = 779114113156820814L;

            @Override
            public String getToolTip(PremServUser value) {
                StringBuilder sb = new StringBuilder();
                sb.append("<html>");
                sb.append("<h1>Total");
                sb.append(SizeFormatter.formatBytes(value.calculateTotalTraffic()));
                sb.append("</h1>");
                Entry<String, Long> next;
                for (Iterator<Entry<String, Long>> it = value.createTrafficStats().entrySet().iterator(); it.hasNext();) {
                    next = it.next();
                    sb.append("<b>");
                    sb.append(next.getKey());
                    sb.append("</b>: ");
                    sb.append(SizeFormatter.formatBytes(next.getValue()));
                    sb.append("<br>");

                }
                sb.append("</html>");
                return sb.toString();
            }

            @Override
            protected String getStringValue(PremServUser value) {
                return SizeFormatter.formatBytes(value.calculateTotalTraffic());
            }

        });

        this.addColumn(new ExtTextColumn<PremServUser>("Traffic/Month", this) {

            private static final long serialVersionUID = 4904311453829598265L;

            @Override
            protected String getStringValue(PremServUser value) {
                return SizeFormatter.formatBytes(value.getAllowedTrafficPerMonth());
            }

        });
    }
}
