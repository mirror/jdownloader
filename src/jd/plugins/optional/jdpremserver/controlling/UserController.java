package jd.plugins.optional.jdpremserver.controlling;

import java.util.ArrayList;

import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.plugins.optional.jdpremserver.model.PremServUser;
import jd.utils.JDUtilities;

import org.appwork.storage.ConfigInterface;
import org.appwork.utils.event.BasicEvent;
import org.appwork.utils.event.BasicEventSender;
import org.codehaus.jackson.type.TypeReference;

public class UserController implements ControlListener {

    private static final UserController INSTANCE = new UserController();
    public static final int USER_ADDED = 0;
    public static final int LOADED = 1;
    public static final int REMOVED_USER = 2;
    public static final int UPDATE = 3;
    private static final String STORAGEPATH = "cfg/premserv/Users2.json";

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
        premServUsers = ConfigInterface.restoreFrom(STORAGEPATH, new TypeReference<ArrayList<PremServUser>>() {
        }, new ArrayList<PremServUser>());

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

        ConfigInterface.storeTo(STORAGEPATH, premServUsers);
    }

    public void removeUser(PremServUser obj) {
        premServUsers.remove(obj);
        eventSender.fireEvent(new BasicEvent<PremServUser>(this, REMOVED_USER, obj, null));

    }

    public void setUserEnabled(PremServUser obj, boolean b) {
        obj.setEnabled(b);

    }

    public boolean isUserAllowedToDownload(String username, String password, String hoster, long traffic) {
        PremServUser user = getUserByUserName(username);
        hoster = hoster.toLowerCase();
        if (user == null) return false;
        if (!user.getPassword().equals(password)) return false;

        if (user.calculateTrafficLeft(hoster.toLowerCase()) < traffic) return false;
        return true;
    }

    private PremServUser getUserByUserName(String username) {
        username = username.toLowerCase();
        for (PremServUser u : premServUsers) {
            if (u.getUsername().equals(username)) return u;
        }
        return null;
    }

    public void fireUserUpdate(PremServUser obj) {
        eventSender.fireEvent(new BasicEvent<PremServUser>(this, UPDATE, obj, null));
    }

}
