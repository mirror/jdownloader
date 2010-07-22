package jd.plugins.optional.jdpremserv.gui;

import java.util.ArrayList;

import jd.plugins.optional.jdpremserv.controlling.UserController;
import jd.plugins.optional.jdpremserv.model.PremServUser;

import org.appwork.utils.event.BasicEvent;
import org.appwork.utils.event.BasicListener;
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

        this.addColumn(new ExtTextColumn<PremServUser>("Allowed Hosters", this) {

            @Override
            protected String getStringValue(PremServUser value) {

                return value.getHosters() + "";
            }

        });
    }
}
