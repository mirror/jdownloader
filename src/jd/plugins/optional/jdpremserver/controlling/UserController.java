package jd.plugins.optional.jdpremserver.controlling;

import java.util.ArrayList;

import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.plugins.optional.jdpremserver.model.PremServUser;
import jd.plugins.optional.jdpremserver.model.UserData;
import jd.utils.JDUtilities;

import org.appwork.storage.ConfigInterface;
import org.appwork.utils.event.BasicEvent;
import org.appwork.utils.event.BasicEventSender;

public class UserController implements ControlListener {

    private static final UserController INSTANCE = new UserController();
    public static final int USER_ADDED = 0;
    public static final int LOADED = 1;
    public static final int REMOVED_USER = 2;
    public static final int UPDATE = 3;

    public static UserController getInstance() {
        // TODO Auto-generated method stub
        return INSTANCE;
    }

    private BasicEventSender<PremServUser> eventSender;
    private ArrayList<PremServUser> premServUsers;

    public BasicEventSender<PremServUser> getEventSender() {
        return eventSender;
    }

    private UserController() {
        this.eventSender = new BasicEventSender<PremServUser>();
        UserData[] data = ConfigInterface.restoreFrom("cfg/premserv/Users.json", null, new UserData[] {});
        premServUsers = new ArrayList<PremServUser>();
        for (UserData d : data) {
            premServUsers.add(d.getPremServUser());
        }

        JDUtilities.getController().addControlListener(this);
        eventSender.fireEvent(new BasicEvent<PremServUser>(this, LOADED, null, null));
    }

    @SuppressWarnings("unchecked")
    public ArrayList<PremServUser> getPremServUsers() {
        return (ArrayList<PremServUser>) premServUsers.clone();
    }

    public void addUser(String username, String password) {
        PremServUser user = new PremServUser(username, password);
        premServUsers.add(user);
        eventSender.fireEvent(new BasicEvent<PremServUser>(this, USER_ADDED, user, null));
    }

    public void controlEvent(ControlEvent event) {
        switch (event.getID()) {
        case ControlEvent.CONTROL_SYSTEM_EXIT:
            save();
        }

    }

    private void save() {

        ArrayList<UserData> list = new ArrayList<UserData>();
        for (PremServUser u : premServUsers) {
            list.add(UserData.create(u));
        }

        ConfigInterface.storeTo("cfg/premserv/Users.json", list);
    }

    public void removeUser(PremServUser obj) {
        premServUsers.remove(obj);
        eventSender.fireEvent(new BasicEvent<PremServUser>(this, REMOVED_USER, obj, null));

    }

    public void setUserEnabled(PremServUser obj, boolean b) {
        obj.setEnabled(b);
        eventSender.fireEvent(new BasicEvent<PremServUser>(this, UPDATE, obj, null));

    }

}
