/*
 * $Id: JXLoginPane.java,v 1.16 2008/02/28 07:32:43 rah003 Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.jdesktop.swingx.action.AbstractActionExt;
import org.jdesktop.swingx.auth.DefaultUserNameStore;
import org.jdesktop.swingx.auth.LoginAdapter;
import org.jdesktop.swingx.auth.LoginEvent;
import org.jdesktop.swingx.auth.LoginListener;
import org.jdesktop.swingx.auth.LoginService;
import org.jdesktop.swingx.auth.PasswordStore;
import org.jdesktop.swingx.auth.UserNameStore;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.plaf.LoginPaneAddon;
import org.jdesktop.swingx.plaf.LoginPaneUI;
import org.jdesktop.swingx.plaf.LookAndFeelAddons;
import org.jdesktop.swingx.plaf.UIManagerExt;
import org.jdesktop.swingx.util.WindowUtils;

/**
 *  <p>JXLoginPane is a specialized JPanel that implements a Login dialog with
 *  support for saving passwords supplied for future use in a secure
 *  manner. <strong>LoginService</strong> is invoked to perform authentication
 *  and optional <strong>PasswordStore</strong> can be provided to store the user 
 *  login information.</p>
 *
 *  <p> In order to perform the authentication, <strong>JXLoginPane</strong>
 *  calls the <code>authenticate</code> method of the <strong>LoginService
 *  </strong>. In order to perform the persistence of the password,
 *  <strong>JXLoginPane</strong> calls the put method of the
 *  <strong>PasswordStore</strong> object that is supplied. If
 *  the <strong>PasswordStore</strong> is <code>null</code>, then the password
 *  is not saved. Similarly, if a <strong>PasswordStore</strong> is
 *  supplied and the password is null, then the <strong>PasswordStore</strong>
 *  will be queried for the password using the <code>get</code> method.
 *  
 *  Example:
 *  <code><pre>
 *         final JXLoginPane panel = new JXLoginPane(new LoginService() {
 *                      public boolean authenticate(String name, char[] password,
 *                                      String server) throws Exception {
 *                              // perform authentication and return true on success.
 *                              return false;
 *                      }});
 *      final JFrame frame = JXLoginPane.showLoginFrame(panel);
 * </pre></code>
 * 
 * @author Bino George
 * @author Shai Almog
 * @author rbair
 * @author Karl Schaefer
 * @author rah003
 */
public class JXLoginPane extends JXPanel {
    
	/**
	 * The Logger
	 */
    private static final Logger LOG = Logger.getLogger(JXLoginPane.class.getName());
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3544949969896288564L;
    /**
     * UI Class ID
     */
    public final static String uiClassID = "LoginPaneUI";
    /**
     * Action key for an Action in the ActionMap that initiates the Login
     * procedure
     */
    public static final String LOGIN_ACTION_COMMAND = "login";
    /** 
     * Action key for an Action in the ActionMap that cancels the Login
     * procedure
     */
    public static final String CANCEL_LOGIN_ACTION_COMMAND = "cancel-login";
    /**
     * The JXLoginPane can attempt to save certain user information such as
     * the username, password, or both to their respective stores.
     * This type specifies what type of save should be performed.
     */
    public static enum SaveMode {NONE, USER_NAME, PASSWORD, BOTH}
    /**
     * Returns the status of the login process
     */
    public enum Status {NOT_STARTED, IN_PROGRESS, FAILED, CANCELLED, SUCCEEDED}
    /**
     * Used as a prefix when pulling data out of UIManager for i18n
     */
    private static String CLASS_NAME = JXLoginPane.class.getSimpleName();

    /**
     * The current login status for this panel
     */
    private Status status = Status.NOT_STARTED;
    /**
     * An optional banner at the top of the panel
     */
    private JXImagePanel banner;
    /**
     * Text that should appear on the banner
     */
    private String bannerText;
    /**
     * Custom label allowing the developer to display some message to the user
     */
    private JLabel messageLabel;
    /**
     * Shows an error message such as "user name or password incorrect" or
     * "could not contact server" or something like that if something
     * goes wrong
     */
    private JXLabel errorMessageLabel;
    /**
     * A Panel containing all of the input fields, check boxes, etc necessary
     * for the user to do their job. The items on this panel change whenever
     * the SaveMode changes, so this panel must be recreated at runtime if the
     * SaveMode changes. Thus, I must maintain this reference so I can remove
     * this panel from the content panel at runtime.
     */
    private JXPanel loginPanel;
    /**
     * The panel on which the input fields, messageLabel, and errorMessageLabel
     * are placed. While the login thread is running, this panel is removed
     * from the dialog and replaced by the progressPanel
     */
    private JXPanel contentPanel;
    /**
     * This is the area in which the name field is placed. That way it can toggle on the fly
     * between text field and a combo box depending on the situation, and have a simple
     * way to get the user name
     */
    private NameComponent namePanel;
    /**
     * The password field presented allowing the user to enter their password
     */
    private JPasswordField passwordField;
    /**
     * A combo box presenting the user with a list of servers to which they
     * may log in. This is an optional feature, which is only enabled if
     * the List of servers supplied to the JXLoginPane has a length greater
     * than 1.
     */
    private JComboBox serverCombo;
    /**
     * Check box presented if a PasswordStore is used, allowing the user to decide whether to
     * save their password
     */
    private JCheckBox saveCB;
    /**
     * Label displayed whenever caps lock is on.
     */
    private JLabel capsOn;
    /**
     * A special panel that displays a progress bar and cancel button, and
     * which notify the user of the login process, and allow them to cancel
     * that process.
     */
    private JXPanel progressPanel;
    /**
     * A JLabel on the progressPanel that is used for informing the user
     * of the status of the login procedure (logging in..., canceling login...)
     */
    private JLabel progressMessageLabel;
    /**
     * The LoginService to use. This must be specified for the login dialog to operate.
     * If no LoginService is defined, a default login service is used that simply
     * allows all users access. This is useful for demos or prototypes where a proper login
     * server is not available.
     */
    private LoginService loginService;
    /**
     * Optional: a PasswordStore to use for storing and retrieving passwords for a specific
     * user.
     */
    private PasswordStore passwordStore;
    /**
     * Optional: a UserNameStore to use for storing user names and retrieving them
     */
    private UserNameStore userNameStore;
    /**
     * A list of servers where each server is represented by a String. If the
     * list of Servers is greater than 1, then a combo box will be presented to
     * the user to choose from. If any servers are specified, the selected one
     * (or the only one if servers.size() == 1) will be passed to the LoginService
     */
    private List<String> servers;
    /**
     *  Whether to save password or username or both
     */
    private SaveMode saveMode;
    /**
     * Tracks the cursor at the time that authentication was started, and restores to that
     * cursor after authentication ends, or is cancelled;
     */
    private Cursor oldCursor;
    
    /**
     * The default login listener used by this panel.
     */
    private LoginListener defaultLoginListener;
    private CapsOnTest capsOnTest = new CapsOnTest();
    private boolean caps;
    private boolean isTestingCaps;
    private KeyEventDispatcher capsOnListener = new KeyEventDispatcher() {
        public boolean dispatchKeyEvent(KeyEvent e) {
            if (e.getID() != KeyEvent.KEY_PRESSED) {
                return false;
            }
            if (e.getKeyCode() == 20) {
                setCapsLock(!isCapsLockOn());
            }
            return false;
		}};
	/**
	 * Caps lock detection support
	 */
	private boolean capsLockSupport = true;

	
	/**
	 * Login/cancel control pane;
	 */
	private JXBtnPanel buttonPanel;
	/**
	 * Window event listener responsible for triggering caps lock test on vindow activation and 
	 * focus changes.
	 */
	private CapsOnWinListener capsOnWinListener = new CapsOnWinListener(capsOnTest);
    
    /**
     * Creates a default JXLoginPane instance
     */
    static {
        LookAndFeelAddons.contribute(new LoginPaneAddon());
    }

    /**
     * Populates UIDefaults with the localizable Strings we will use
     * in the Login panel.
     */
    private void reinitLocales(Locale l) {
        // PENDING: JW - use the locale given as parameter
        // as this probably (?) should be called before super.setLocale
        setBannerText(UIManagerExt.getString(CLASS_NAME + ".bannerString", getLocale()));
        banner.setImage(createLoginBanner());
        // TODO: Can't change the error message since it might have been already changed by the user!
        //errorMessageLabel.setText(UIManager.getString(CLASS_NAME + ".errorMessage", getLocale()));
        progressMessageLabel.setText(UIManagerExt.getString(CLASS_NAME + ".pleaseWait", getLocale()));
        recreateLoginPanel();
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof JXLoginFrame) {
            JXLoginFrame f = (JXLoginFrame) w;
            f.setTitle(UIManagerExt.getString(CLASS_NAME + ".titleString", getLocale()));
            if (buttonPanel != null) {
                buttonPanel.getOk().setText(UIManagerExt.getString(CLASS_NAME + ".loginString", getLocale()));
                buttonPanel.getCancel().setText(UIManagerExt.getString(CLASS_NAME + ".cancelString", getLocale()));
            }
        }
        JLabel lbl = (JLabel) passwordField.getClientProperty("labeledBy");
        if (lbl != null) {
            lbl.setText(UIManagerExt.getString(CLASS_NAME + ".passwordString", getLocale()));
        }
        lbl = (JLabel) namePanel.getComponent().getClientProperty("labeledBy");
        if (lbl != null) {
            lbl.setText(UIManagerExt.getString(CLASS_NAME + ".nameString", getLocale()));
        }
        if (serverCombo != null) {
            lbl = (JLabel) serverCombo.getClientProperty("labeledBy");
            if (lbl != null) {
                lbl.setText(UIManagerExt.getString(CLASS_NAME + ".serverString", getLocale()));
            }
        }
        saveCB.setText(UIManagerExt.getString(CLASS_NAME + ".rememberPasswordString", getLocale()));
        // by default, caps is initialized in off state - i.e. without warning. Setting to 
        // whitespace preserves formatting of the panel.
        capsOn.setText(isCapsLockOn() ? UIManagerExt.getString(CLASS_NAME + ".capsOnWarning", getLocale()) : " ");
        
        getActionMap().get(LOGIN_ACTION_COMMAND).putValue(Action.NAME, UIManagerExt.getString(CLASS_NAME + ".loginString", getLocale()));
        getActionMap().get(CANCEL_LOGIN_ACTION_COMMAND).putValue(Action.NAME, UIManagerExt.getString(CLASS_NAME + ".cancelString", getLocale()));

    }
    
    //--------------------------------------------------------- Constructors
    /**
     * Create a {@code JXLoginPane} that always accepts the user, never stores
     * passwords or user ids, and has no target servers.
     * <p>
     * This constructor should <i>NOT</i> be used in a real application. It is
     * provided for compliance to the bean specification and for use with visual
     * editors.
     */
    public JXLoginPane() {
        this(null);
    }
    
    /**
     * Create a {@code JXLoginPane} with the specified {@code LoginService}
     * that does not store user ids or passwords and has no target servers.
     * 
     * @param service
     *            the {@code LoginService} to use for logging in
     */
    public JXLoginPane(LoginService service) {
        this(service, null, null);
    }
    
    /**
     * Create a {@code JXLoginPane} with the specified {@code LoginService},
     * {@code PasswordStore}, and {@code UserNameStore}, but without a server
     * list.
     * <p>
     * If you do not want to store passwords or user ids, those parameters can
     * be {@code null}. {@code SaveMode} is autoconfigured from passed in store
     * parameters.
     * 
     * @param service
     *            the {@code LoginService} to use for logging in
     * @param passwordStore
     *            the {@code PasswordStore} to use for storing password
     *            information
     * @param userStore
     *            the {@code UserNameStore} to use for storing user information
     */
    public JXLoginPane(LoginService service, PasswordStore passwordStore, UserNameStore userStore) {
        this(service, passwordStore, userStore, null);
    }
    
    /**
     * Create a {@code JXLoginPane} with the specified {@code LoginService},
     * {@code PasswordStore}, {@code UserNameStore}, and server list.
     * <p>
     * If you do not want to store passwords or user ids, those parameters can
     * be {@code null}. {@code SaveMode} is autoconfigured from passed in store
     * parameters.
     * <p>
     * Setting the server list to {@code null} will unset all of the servers.
     * The server list is guaranteed to be non-{@code null}.
     * 
     * @param service
     *            the {@code LoginService} to use for logging in
     * @param passwordStore
     *            the {@code PasswordStore} to use for storing password
     *            information
     * @param userStore
     *            the {@code UserNameStore} to use for storing user information
     * @param servers
     *            a list of servers to authenticate against
     */
    public JXLoginPane(LoginService service, PasswordStore passwordStore, UserNameStore userStore, List<String> servers) {
        setLoginService(service);
        setPasswordStore(passwordStore);
        setUserNameStore(userStore);
        setServers(servers);
        
        
        //create the login and cancel actions, and add them to the action map
        getActionMap().put(LOGIN_ACTION_COMMAND, createLoginAction());
        getActionMap().put(CANCEL_LOGIN_ACTION_COMMAND, createCancelAction());
        
        //initialize the save mode
        if (passwordStore != null && userStore != null) {
            saveMode = SaveMode.BOTH;
        } else if (passwordStore != null) {
            saveMode = SaveMode.PASSWORD;
        } else if (userStore != null) {
            saveMode = SaveMode.USER_NAME;
        } else {
            saveMode = SaveMode.NONE;
        }

//        if (!initDone) {
        	initComponents();
//        }
//        updateUI();
    }
    
    /**
     * Sets current state of the caps lock key as detected by the component.
     * @param b True when caps lock is turned on, false otherwise.
     */
    private void setCapsLock(boolean b) {
        caps = b;
        capsOn.setText(caps ? UIManagerExt.getString(CLASS_NAME + ".capsOnWarning", getLocale()) : " ");
    }
    
    /**
     * Gets current state of the caps lock as seen by the login panel. The state seen by the login 
     * panel and therefore returned by this method can be delayed in comparison to the real caps 
     * lock state and displayed by the keyboard light. This is usually the case when component or 
     * its text fields are not focused.
     * 
     * @return True when caps lock is on, false otherwise. Returns always false when 
     * <code>isCapsLockDetectionSupported()</code> returns false.
     */
    public boolean isCapsLockOn() {
        return caps;
    }

    /**
     * Check current state of the caps lock state detection. Note that the value can change after 
     * component have been made visible. Due to current problems in locking key state detection by 
     * core java detection of the changes in caps lock can be always reliably determined. When 
     * component can't guarantee reliable detection it will switch it off. This is usually the case 
     * for unsigned applets and webstart invoked application. Since your users are going to pass 
     * their password in the component you should always sign it when distributing application over 
     * the network.
     * @return True if changes in caps lock state can be monitored by the component, false otherwise.
     */
    public boolean isCapsLockDetectionSupported() {
        return capsLockSupport;
    }
    
    //------------------------------------------------------------- UI Logic
    
    /**
     * {@inheritDoc}
     */
    public LoginPaneUI getUI() {
        return (LoginPaneUI) super.getUI();
    }
    
    /**
     * Sets the look and feel (L&F) object that renders this component.
     *
     * @param ui the LoginPaneUI L&F object
     * @see javax.swing.UIDefaults#getUI
     */
    public void setUI(LoginPaneUI ui) {
        // initialized here due to implicit updateUI call from JPanel
        if (banner == null) {
            banner = new JXImagePanel();
        }
        if (errorMessageLabel == null) {
            errorMessageLabel = new JXLabel(UIManagerExt.getString(CLASS_NAME + ".errorMessage", getLocale())); 
        }
        super.setUI(ui);
        banner.setImage(createLoginBanner());
    }
    
    /**
     * Notification from the <code>UIManager</code> that the L&F has changed.
     * Replaces the current UI object with the latest version from the
     * <code>UIManager</code>.
     *
     * @see javax.swing.JComponent#updateUI
     */
    public void updateUI() {
        setUI((LoginPaneUI) LookAndFeelAddons.getUI(this, LoginPaneUI.class));
    }

    /**
     * Returns the name of the L&F class that renders this component.
     *
     * @return the string {@link #uiClassID}
     * @see javax.swing.JComponent#getUIClassID
     * @see javax.swing.UIDefaults#getUI
     */
    public String getUIClassID() {
        return uiClassID;
    }

    /**
     * Recreates the login panel, and replaces the current one with the new one
     */
    protected void recreateLoginPanel() {
        contentPanel.remove(loginPanel);
        loginPanel = createLoginPanel();
        loginPanel.setBorder(BorderFactory.createEmptyBorder(0, 36, 7, 11));
        contentPanel.add(loginPanel, 1);
    }
    
    /**
     * Creates and returns a new LoginPanel, based on the SaveMode state of
     * the login panel. Whenever the SaveMode changes, the panel is recreated.
     * I do this rather than hiding/showing components, due to a cleaner
     * implementation (no invisible components, components are not sharing
     * locations in the LayoutManager, etc).
     */
    private JXPanel createLoginPanel() {
        JXPanel loginPanel = new JXPanel();
        
        //create the NameComponent
        if (saveMode == SaveMode.NONE) {
            namePanel = new SimpleNamePanel();
        } else {
            namePanel = new ComboNamePanel(userNameStore);
        }
        JLabel nameLabel = new JLabel(UIManagerExt.getString(CLASS_NAME + ".nameString", getLocale()));
        nameLabel.setLabelFor(namePanel.getComponent());
        
        //create the password component
        passwordField = new JPasswordField("", 15);
        JLabel passwordLabel = new JLabel(UIManagerExt.getString(CLASS_NAME + ".passwordString", getLocale()));
        passwordLabel.setLabelFor(passwordField);
        
        //create the server combo box if necessary
        JLabel serverLabel = new JLabel(UIManagerExt.getString(CLASS_NAME + ".serverString", getLocale()));
        if (servers.size() > 1) {
            serverCombo = new JComboBox(servers.toArray());
            serverLabel.setLabelFor(serverCombo);
        } else {
            serverCombo = null;
        }
        
        //create the save check box. By default, it is not selected
        saveCB = new JCheckBox(UIManagerExt.getString(CLASS_NAME + ".rememberPasswordString", getLocale()));
        saveCB.setIconTextGap(10);
        saveCB.setSelected(false); //TODO should get this from prefs!!! And, it should be based on the user
        //determine whether to show/hide the save check box based on the SaveMode
        saveCB.setVisible(saveMode == SaveMode.PASSWORD || saveMode == SaveMode.BOTH);
        
        capsOn = new JLabel(" ");
        // don't show by default. We perform test when login panel gets focus.
        
        int lShift = 3;// lShift is used to align all other components with the checkbox
        GridLayout grid = new GridLayout(2,1);
        grid.setVgap(5);
        JPanel fields = new JPanel(grid);
        fields.add(namePanel.getComponent());
        fields.add(passwordField);

        loginPanel.setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(4, lShift, 5, 11);
        loginPanel.add(nameLabel, gridBagConstraints);
        
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        loginPanel.add(fields, gridBagConstraints);
        
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(5, lShift, 5, 11);
        loginPanel.add(passwordLabel, gridBagConstraints);
        
        if (serverCombo != null) {
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.anchor = GridBagConstraints.LINE_START;
            gridBagConstraints.insets = new Insets(0, lShift, 5, 11);
            loginPanel.add(serverLabel, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = GridBagConstraints.LINE_START;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 0, 5, 0);
            loginPanel.add(serverCombo, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.LINE_START;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 0, 4, 0);
            loginPanel.add(saveCB, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 4;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.LINE_START;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, lShift, 0, 11);
            loginPanel.add(capsOn, gridBagConstraints);
        } else {
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.LINE_START;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 0, 4, 0);
            loginPanel.add(saveCB, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.LINE_START;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, lShift, 0, 11);
            loginPanel.add(capsOn, gridBagConstraints);
        }
        return loginPanel;
    }
    
    /**
     * This method adds functionality to support bidi languages within this 
     * component
     */
    public void setComponentOrientation(ComponentOrientation orient) {
        // this if is used to avoid needless creations of the image
        if(orient != super.getComponentOrientation()) {
            super.setComponentOrientation(orient);
            banner.setImage(createLoginBanner());
            progressPanel.applyComponentOrientation(orient);
        }
    }
    
    /**
     * Create all of the UI components for the login panel
     */
    private void initComponents() {
        //create the default banner
        banner.setImage(createLoginBanner());

        //create the default label
        messageLabel = new JLabel(" ");
        messageLabel.setOpaque(true);
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD));

        //create the main components
        loginPanel = createLoginPanel();
        
        //create the message and hyperlink and hide them
        errorMessageLabel.setIcon(UIManager.getIcon(CLASS_NAME + ".errorIcon", getLocale()));
        errorMessageLabel.setVerticalTextPosition(SwingConstants.TOP);
        errorMessageLabel.setLineWrap(true);
        errorMessageLabel.setPaintBorderInsets(false);
        errorMessageLabel.setBackgroundPainter(new MattePainter(UIManager.getColor(CLASS_NAME + ".errorBackground", getLocale()), true));
        errorMessageLabel.setMaxLineSpan(320);
        errorMessageLabel.setVisible(false);
        
        //aggregate the optional message label, content, and error label into
        //the contentPanel
        contentPanel = new JXPanel(new VerticalLayout());
        messageLabel.setBorder(BorderFactory.createEmptyBorder(12, 12, 7, 11));
        contentPanel.add(messageLabel);
        loginPanel.setBorder(BorderFactory.createEmptyBorder(0, 36, 7, 11));
        contentPanel.add(loginPanel);
        errorMessageLabel.setBorder(UIManager.getBorder(CLASS_NAME + ".errorBorder", getLocale()));
        contentPanel.add(errorMessageLabel);
        
        //create the progress panel
        progressPanel = new JXPanel(new GridBagLayout());
        progressMessageLabel = new JLabel(UIManagerExt.getString(CLASS_NAME + ".pleaseWait", getLocale()));
        progressMessageLabel.setFont(UIManager.getFont(CLASS_NAME +".pleaseWaitFont", getLocale()));
        JProgressBar pb = new JProgressBar();
        pb.setIndeterminate(true);
        JButton cancelButton = new JButton(getActionMap().get(CANCEL_LOGIN_ACTION_COMMAND));
        progressPanel.add(progressMessageLabel, new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(12, 12, 11, 11), 0, 0));
        progressPanel.add(pb, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 24, 11, 7), 0, 0));
        progressPanel.add(cancelButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 11, 11), 0, 0));
        
        //layout the panel
        setLayout(new BorderLayout());
        add(banner, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        
    }

    /**
     * Create and return an image to use for the Banner. This may be overridden
     * to return any image you like
     */
    protected Image createLoginBanner() {
        return getUI() == null ? null : getUI().getBanner();
    }
    
    /**
     * Create and return an Action for logging in
     */
    protected Action createLoginAction() {
        return new LoginAction(this);
    }
    
    /**
     * Create and return an Action for canceling login
     */
    protected Action createCancelAction() {
        return new CancelAction(this);
    }
    
    //------------------------------------------------------ Bean Properties
    //TODO need to fire property change events!!!
    /**
     * @return Returns the saveMode.
     */
    public SaveMode getSaveMode() {
        return saveMode;
    }
    
    /**
     * The save mode indicates whether the "save" password is checked by default. This method
     * makes no difference if the passwordStore is null.
     *
     * @param saveMode The saveMode to set either SAVE_NONE, SAVE_PASSWORD or SAVE_USERNAME
     */
    public void setSaveMode(SaveMode saveMode) {
        if (this.saveMode != saveMode) {
            SaveMode oldMode = getSaveMode();
            this.saveMode = saveMode;
            recreateLoginPanel();
            firePropertyChange("saveMode", oldMode, getSaveMode());
        }
    }
    
    /**
     * @return the List of servers
     */
    public List<String> getServers() {
        return Collections.unmodifiableList(servers);
    }
    
    /**
     * Sets the list of servers. See the servers field javadoc for more info
     */
    public void setServers(List<String> servers) {
        //only at startup
        if (this.servers == null) {
            this.servers = servers == null ? new ArrayList<String>() : servers;
        } else if (this.servers != servers) {
            List<String> old = getServers();
            this.servers = servers == null ? new ArrayList<String>() : servers;
            recreateLoginPanel();
            firePropertyChange("servers", old, getServers());
        }
    }
    
    private LoginListener getDefaultLoginListener() {
        if (defaultLoginListener == null) {
            defaultLoginListener = new LoginListenerImpl();
        }
        
        return defaultLoginListener;
    }
    
    /**
     * Sets the {@code LoginService} for this panel. Setting the login service
     * to {@code null} will actually set the service to use
     * {@code NullLoginService}.
     * 
     * @param service
     *            the service to set. If {@code service == null}, then a
     *            {@code NullLoginService} is used.
     */
    public void setLoginService(LoginService service) {
        LoginService oldService = getLoginService();
        LoginService newService = service == null ? new NullLoginService() : service;
        
        //newService is guaranteed to be nonnull
        if (!newService.equals(oldService)) {
            if (oldService != null) {
                oldService.removeLoginListener(getDefaultLoginListener());
            }
            
            loginService = newService;
            this.loginService.addLoginListener(getDefaultLoginListener());
            
            firePropertyChange("loginService", oldService, getLoginService());
        }
    }
    
    /**
     * Gets the <strong>LoginService</strong> for this panel.
     *
     * @return service service
     */
    public LoginService getLoginService() {
        return loginService;
    }
    
    /**
     * Sets the <strong>PasswordStore</strong> for this panel.
     *
     * @param store PasswordStore
     */
    public void setPasswordStore(PasswordStore store) {
        PasswordStore oldStore = getPasswordStore();
        PasswordStore newStore = store == null ? new NullPasswordStore() : store;
        
        //newStore is guaranteed to be nonnull
        if (!newStore.equals(oldStore)) {
            passwordStore = newStore;
            
            firePropertyChange("passwordStore", oldStore, getPasswordStore());
        }
    }
    
    /**
     * Gets the {@code UserNameStore} for this panel.
     * 
     * @return the {@code UserNameStore}
     */
    public UserNameStore getUserNameStore() {
        return userNameStore;
    }

    /**
     * Sets the user name store for this panel.
     * @param store
     */
    public void setUserNameStore(UserNameStore store) {
        UserNameStore oldStore = getUserNameStore();
        UserNameStore newStore = store == null ? new DefaultUserNameStore() : store;
        
        //newStore is guaranteed to be nonnull
        if (!newStore.equals(oldStore)) {
            userNameStore = newStore;
            
            firePropertyChange("userNameStore", oldStore, getUserNameStore());
        }
    }

    /**
     * Gets the <strong>PasswordStore</strong> for this panel.
     *
     * @return store PasswordStore
     */
    public PasswordStore getPasswordStore() {
        return passwordStore;
    }
    
    /**
     * Sets the <strong>User name</strong> for this panel.
     *
     * @param username User name
     */
    public void setUserName(String username) {
        if (namePanel != null) {
            namePanel.setUserName(username);
        }
    }
    
    /**
     * Gets the <strong>User name</strong> for this panel.
     * @return the user name
     */
    public String getUserName() {
        return namePanel == null ? null : namePanel.getUserName();
    }
    
    /**
     * Sets the <strong>Password</strong> for this panel.
     *
     * @param password Password
     */
    public void setPassword(char[] password) {
        passwordField.setText(new String(password));
    }
    
    /**
     * Gets the <strong>Password</strong> for this panel.
     *
     * @return password Password
     */
    public char[] getPassword() {
        return passwordField.getPassword();
    }
    
    /**
     * Return the image used as the banner
     */
    public Image getBanner() {
        return banner.getImage();
    }
    
    /**
     * Set the image to use for the banner. If the {@code img} is {@code null},
     * then no image will be displayed.
     * 
     * @param img
     *            the image to display
     */
    public void setBanner(Image img) {
        // we do not expose the ImagePanel, so we will produce property change
        // events here
        Image oldImage = getBanner();
        
        if (oldImage != img) {
            banner.setImage(img);
            firePropertyChange("banner", oldImage, getBanner());
        }
    }
    
    /**
     * Set the text to use when creating the banner. If a custom banner image is
     * specified, then this is ignored. If {@code text} is {@code null}, then
     * no text is displayed.
     * 
     * @param text
     *            the text to display
     */
    public void setBannerText(String text) {
        if (text == null) {
            text = "";
        }

        if (!text.equals(this.bannerText)) {
            String oldText = this.bannerText;
            this.bannerText = text;
            //fix the login banner
            this.banner.setImage(createLoginBanner());
            firePropertyChange("bannerText", oldText, text);
        }
    }

    /**
     * Returns text used when creating the banner
     */
    public String getBannerText() {
        return bannerText;
    }

    /**
     * Returns the custom message for this login panel
     */
    public String getMessage() {
        return messageLabel.getText();
    }
    
    /**
     * Sets a custom message for this login panel
     */
    public void setMessage(String message) {
        messageLabel.setText(message);
    }
    
    /**
     * Returns the error message for this login panel
     */
    public String getErrorMessage() {
        return errorMessageLabel.getText();
    }
    
    /**
     * Sets the error message for this login panel
     */
    public void setErrorMessage(String errorMessage) {
        errorMessageLabel.setText(errorMessage);
    }
    
    /**
     * Returns the panel's status
     */
    public Status getStatus() {
        return status;
    }
    
    /**
     * Change the status
     */
    protected void setStatus(Status newStatus) {
        if (status != newStatus) {
            Status oldStatus = status;
            status = newStatus;
            firePropertyChange("status", oldStatus, newStatus);
        }
    }
    
    public void setLocale(Locale l) {
        super.setLocale(l);
        reinitLocales(l);
    }
    //-------------------------------------------------------------- Methods
    
    /**
     * Initiates the login procedure. This method is called internally by
     * the LoginAction. This method handles cursor management, and actually
     * calling the LoginService's startAuthentication method. Method will return 
     * immediately if asynchronous login is enabled or will block until 
     * authentication finishes if <code>getSynchronous()</code> returns true.
     */
    protected void startLogin() {
        oldCursor = getCursor();
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            progressMessageLabel.setText(UIManagerExt.getString(CLASS_NAME + ".pleaseWait", getLocale()));
            String name = getUserName();
            char[] password = getPassword();
            String server = servers.size() == 1 ? servers.get(0) : serverCombo == null ? null : (String)serverCombo.getSelectedItem();
            loginService.startAuthentication(name, password, server);
        } catch(Exception ex) {
        //The status is set via the loginService listener, so no need to set
        //the status here. Just log the error.
        LOG.log(Level.WARNING, "Authentication exception while logging in", ex);
        } finally {
            setCursor(oldCursor);
        }
    }
    
    /**
     * Cancels the login procedure. Handles cursor management and interfacing
     * with the LoginService's cancelAuthentication method. Calling this method 
     * has an effect only when authentication is still in progress (i.e. after 
     * previous call to <code>startAuthentications()</code> and only when 
     * authentication is performed asynchronously (<code>getSynchronous()</code> 
     * returns false).
     */
    protected void cancelLogin() {
        progressMessageLabel.setText(UIManagerExt.getString(CLASS_NAME + ".cancelWait", getLocale()));
        getActionMap().get(CANCEL_LOGIN_ACTION_COMMAND).setEnabled(false);
        loginService.cancelAuthentication();
        setCursor(oldCursor);
    }
    
    /**
     * Puts the password into the password store. If password store is not set, method will do 
     * nothing.
     */
    protected void savePassword() {
        if (saveCB.isSelected() 
            && (saveMode == SaveMode.BOTH || saveMode == SaveMode.PASSWORD)
            && passwordStore != null) {
            passwordStore.set(getUserName(),getLoginService().getServer(),getPassword());
        }
    }
    
    public void removeNotify() {
    	try {
	    	// TODO: keep it here until all ui stuff is moved to uidelegate.
    		if (capsLockSupport)
    			KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(capsOnListener);
    	    Container c = JXLoginPane.this;
    	    while (c.getParent() != null) {
    	    	c = c.getParent();
    	    }
    	    if (c instanceof Window) {
    	    	Window w = (Window) c;
    	    	w.removeWindowFocusListener(capsOnWinListener );
    	    	w.removeWindowListener(capsOnWinListener );
    	    }
    	} catch (Exception e) {
    		// bail out probably in unsigned app distributed over web
    	}
    	super.removeNotify();
    }
    
    public void addNotify() {
    	try {
    		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(
    				capsOnListener);
    	    Container c = JXLoginPane.this;
    	    while (c.getParent() != null) {
    	    	c = c.getParent();
    	    }
    	    if (c instanceof Window) {
    	    	Window w = (Window) c;
    	    	w.addWindowFocusListener(capsOnWinListener );
    	    	w.addWindowListener(capsOnWinListener);
    	    }
    	} catch (Exception e) {
    		// probably unsigned app over web, disable capslock support and bail out
    		capsLockSupport = false;
    	}
    	super.addNotify();
    }
    //--------------------------------------------- Listener Implementations
    /*
     
     For Login (initiated in LoginAction):
        0) set the status
        1) Immediately disable the login action
        2) Immediately disable the close action (part of enclosing window)
        3) initialize the progress pane
          a) enable the cancel login action
          b) set the message text
        4) hide the content pane, show the progress pane
     
     When cancelling (initiated in CancelAction):
         0) set the status
         1) Disable the cancel login action
         2) Change the message text on the progress pane
     
     When cancel finishes (handled in LoginListener):
         0) set the status
         1) hide the progress pane, show the content pane
         2) enable the close action (part of enclosing window)
         3) enable the login action
     
     When login fails (handled in LoginListener):
         0) set the status
         1) hide the progress pane, show the content pane
         2) enable the close action (part of enclosing window)
         3) enable the login action
         4) Show the error message
         5) resize the window (part of enclosing window)
     
     When login succeeds (handled in LoginListener):
         0) set the status
         1) close the dialog/frame (part of enclosing window)
     */
    /**
     * Listener class to track state in the LoginService
     */
    protected class LoginListenerImpl extends LoginAdapter {
        public void loginSucceeded(LoginEvent source) {
            //save the user names and passwords
            String userName = namePanel.getUserName();
            savePassword();
            if ((getSaveMode() == SaveMode.USER_NAME || getSaveMode() == SaveMode.BOTH)
                    && userName != null && !userName.trim().equals("")) {
                userNameStore.addUserName(userName);
                userNameStore.saveUserNames();
            }
            setStatus(Status.SUCCEEDED);
        }
            
        public void loginStarted(LoginEvent source) {
            getActionMap().get(LOGIN_ACTION_COMMAND).setEnabled(false);
            getActionMap().get(CANCEL_LOGIN_ACTION_COMMAND).setEnabled(true);
            remove(contentPanel);
            add(progressPanel, BorderLayout.CENTER);
            revalidate();
            repaint();
            setStatus(Status.IN_PROGRESS);
        }

        public void loginFailed(LoginEvent source) {
            remove(progressPanel);
            add(contentPanel, BorderLayout.CENTER);
            getActionMap().get(LOGIN_ACTION_COMMAND).setEnabled(true);
            errorMessageLabel.setVisible(true);
            revalidate();
            repaint();
            setStatus(Status.FAILED);
        }

        public void loginCanceled(LoginEvent source) {
            remove(progressPanel);
            add(contentPanel, BorderLayout.CENTER);
            getActionMap().get(LOGIN_ACTION_COMMAND).setEnabled(true);
            errorMessageLabel.setVisible(false);
            revalidate();
            repaint();
            setStatus(Status.CANCELLED);
        }
    }
    
    //---------------------------------------------- Default Implementations
    /**
     * Action that initiates a login procedure. Delegates to JXLoginPane.startLogin
     */
    private static final class LoginAction extends AbstractActionExt {
    private JXLoginPane panel;
    public LoginAction(JXLoginPane p) {
        super(UIManagerExt.getString(CLASS_NAME + ".loginString", p.getLocale()), LOGIN_ACTION_COMMAND); 
        this.panel = p;
    }
    public void actionPerformed(ActionEvent e) {
        panel.startLogin();
    }
    public void itemStateChanged(ItemEvent e) {}
    }
    
    /**
     * Action that cancels the login procedure. 
     */
    private static final class CancelAction extends AbstractActionExt {
        private JXLoginPane panel;
        public CancelAction(JXLoginPane p) {
            //TODO localize
            super(UIManagerExt.getString(CLASS_NAME + ".cancelLogin", p.getLocale()), CANCEL_LOGIN_ACTION_COMMAND); 
            this.panel = p;
            this.setEnabled(false);
        }
        public void actionPerformed(ActionEvent e) {
            panel.cancelLogin();
        }
        public void itemStateChanged(ItemEvent e) {}
    }
    
    /**
     * Simple login service that allows everybody to login. This is useful in demos and allows
     * us to avoid having to check for LoginService being null
     */
    private static final class NullLoginService extends LoginService {
        public boolean authenticate(String name, char[] password, String server) throws Exception {
            return true;
        }

        public boolean equals(Object obj) {
            return obj instanceof NullLoginService;
        }

        public int hashCode() {
            return 7;
        }
    }
    
    /**
     * Simple PasswordStore that does not remember passwords
     */
    private static final class NullPasswordStore extends PasswordStore {
        private static final char[] EMPTY = new char[0];
        public boolean set(String username, String server, char[] password) {
            //null op
            return false;
        }
        public char[] get(String username, String server) {
            return EMPTY;
        }

        public boolean equals(Object obj) {
            return obj instanceof NullPasswordStore;
        }

        public int hashCode() {
            return 7;
        }
    }
    
    //--------------------------------- Default NamePanel Implementations
    public static interface NameComponent {
        public String getUserName();
        public void setUserName(String userName);
        public JComponent getComponent();
    }
    
    /**
     * If a UserNameStore is not used, then this text field is presented allowing the user
     * to simply enter their user name
     */
    public static final class SimpleNamePanel extends JTextField implements NameComponent {
        public SimpleNamePanel() {
            super("", 15);
        }
        public String getUserName() {
            return getText();
        }
        public void setUserName(String userName) {
            setText(userName);
        }
        public JComponent getComponent() {
            return this;
        }
    }
    
    /**
     * If a UserNameStore is used, then this combo box is presented allowing the user
     * to select a previous login name, or type in a new login name
     */
    public static final class ComboNamePanel extends JComboBox implements NameComponent {
        private UserNameStore userNameStore;
        public ComboNamePanel(UserNameStore userNameStore) {
            super();
            this.userNameStore = userNameStore;
            setModel(new NameComboBoxModel());
            setEditable(true);

        }
        public String getUserName() {
            Object item = getModel().getSelectedItem();
            return item == null ? null : item.toString();
        }
        public void setUserName(String userName) {
            getModel().setSelectedItem(userName);
        }
        public void setUserNames(String[] names) {
            setModel(new DefaultComboBoxModel(names));
        }
        public JComponent getComponent() {
            return this;
        }
        private final class NameComboBoxModel extends AbstractListModel implements ComboBoxModel {
            private Object selectedItem;
            public void setSelectedItem(Object anItem) {
                selectedItem = anItem;
                fireContentsChanged(this, -1, -1);
            }
            public Object getSelectedItem() {
                return selectedItem;
            }
            public Object getElementAt(int index) {
                return userNameStore.getUserNames()[index];
            }
            public int getSize() {
                return userNameStore.getUserNames().length;
            }
        }
    }

    //------------------------------------------ Static Construction Methods
    /**
     * Shows a login dialog. This method blocks.
     * @return The status of the login operation
     */
    public static Status showLoginDialog(Component parent, LoginService svc) {
        return showLoginDialog(parent, svc, null, null);
    }

    /**
     * Shows a login dialog. This method blocks.
     * @return The status of the login operation
     */
    public static Status showLoginDialog(Component parent, LoginService svc, PasswordStore ps, UserNameStore us) {
        return showLoginDialog(parent, svc, ps, us, null);
    }
    
    /**
     * Shows a login dialog. This method blocks.
     * @return The status of the login operation
     */
    public static Status showLoginDialog(Component parent, LoginService svc, PasswordStore ps, UserNameStore us, List<String> servers) {
        JXLoginPane panel = new JXLoginPane(svc, ps, us, servers);
        return showLoginDialog(parent, panel);
    }
    
    /**
     * Shows a login dialog. This method blocks.
     * @return The status of the login operation
     */
    public static Status showLoginDialog(Component parent, JXLoginPane panel) {
        Window w = WindowUtils.findWindow(parent);
        JXLoginDialog dlg =  null;
        if (w == null) {
            dlg = new JXLoginDialog((Frame)null, panel);
        } else if (w instanceof Dialog) {
            dlg = new JXLoginDialog((Dialog)w, panel);
        } else if (w instanceof Frame) {
            dlg = new JXLoginDialog((Frame)w, panel);
        } else {
            throw new AssertionError("Shouldn't be able to happen");
        }
        dlg.setVisible(true);
        return dlg.getStatus();
    }
    
    /**
     * Shows a login frame. A JFrame is not modal, and thus does not block
     */
    public static JXLoginFrame showLoginFrame(LoginService svc) {
        return showLoginFrame(svc, null, null);
    }

    /**
     */
    public static JXLoginFrame showLoginFrame(LoginService svc, PasswordStore ps, UserNameStore us) {
        return showLoginFrame(svc, ps, us, null);
    }
    
    /**
     */
    public static JXLoginFrame showLoginFrame(LoginService svc, PasswordStore ps, UserNameStore us, List<String> servers) {
        JXLoginPane panel = new JXLoginPane(svc, ps, us, servers);
        return showLoginFrame(panel);
    }

    /**
     */
    public static JXLoginFrame showLoginFrame(JXLoginPane panel) {
        return new JXLoginFrame(panel);
    }

    public static final class JXLoginDialog extends JDialog {
        private JXLoginPane panel;
        
        public JXLoginDialog(Frame parent, JXLoginPane p) {
            super(parent, true);
            init(p);
        }
        
        public JXLoginDialog(Dialog parent, JXLoginPane p) {
            super(parent, true);
            init(p);
        }
        
    protected void init(JXLoginPane p) {
        setTitle(UIManagerExt.getString(CLASS_NAME + ".titleString", getLocale())); 
        this.panel = p;
        initWindow(this, panel);
    }
    
    public JXLoginPane.Status getStatus() {
        return panel.getStatus();
    }
    }
    
    public static final class JXLoginFrame extends JFrame {
        private JXLoginPane panel;
    
        public JXLoginFrame(JXLoginPane p) {
            super(UIManagerExt.getString(CLASS_NAME + ".titleString", p.getLocale())); 
            this.panel = p;
            initWindow(this, panel);
        }
        
        public JXLoginPane.Status getStatus() {
            return panel.getStatus();
        }
        
        public JXLoginPane getPanel() {
            return panel;
        }
    }
    
    /**
     * Utility method for initializing a Window for displaying a LoginDialog.
     * This is particularly useful because the differences between JFrame and
     * JDialog are so minor.
     *
     * Note: This method is package private for use by JXLoginDialog (proper, 
     * not JXLoginPane.JXLoginDialog). Change to private if JXLoginDialog is
     * removed.
     */
    static void initWindow(final Window w, final JXLoginPane panel) {
        w.setLayout(new BorderLayout());
        w.add(panel, BorderLayout.CENTER);
        JButton okButton = new JButton(panel.getActionMap().get(LOGIN_ACTION_COMMAND));
        final JButton cancelButton = new JButton(
                UIManagerExt.getString(CLASS_NAME + ".cancelString", panel.getLocale()));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //change panel status to cancelled!
                panel.status = JXLoginPane.Status.CANCELLED;
                w.setVisible(false);
                w.dispose();
            }
        });
        panel.addPropertyChangeListener("status", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                JXLoginPane.Status status = (JXLoginPane.Status)evt.getNewValue();
                switch (status) {
                    case NOT_STARTED:
                        break;
                    case IN_PROGRESS:
                        cancelButton.setEnabled(false);
                        break;
                    case CANCELLED:
                        cancelButton.setEnabled(true);
                        w.pack();
                        break;
                    case FAILED:
                        cancelButton.setEnabled(true);
                        w.pack();
                        break;
                    case SUCCEEDED:
                        w.setVisible(false);
                        w.dispose();
                }
                for (PropertyChangeListener l : w.getPropertyChangeListeners("status")) {
                    PropertyChangeEvent pce = new PropertyChangeEvent(w, "status", evt.getOldValue(), evt.getNewValue());
                    l.propertyChange(pce);
                }
            }
        });
        // FIX for #663 - commented out two lines below. Not sure why they were here in a first place.
        // cancelButton.setText(UIManager.getString(CLASS_NAME + ".cancelString"));
        // okButton.setText(UIManager.getString(CLASS_NAME + ".loginString"));
        JXBtnPanel buttonPanel = new JXBtnPanel(okButton, cancelButton);
        panel.setButtonPanel(buttonPanel);
        JXPanel controls = new JXPanel(new FlowLayout(FlowLayout.RIGHT));
        new BoxLayout(controls, BoxLayout.X_AXIS);
        controls.add(Box.createHorizontalGlue());
        controls.add(buttonPanel);
        w.add(controls, BorderLayout.SOUTH);            
        w.addWindowListener(new WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                panel.cancelLogin();
            }
        });

        if (w instanceof JFrame) {
            final JFrame f = (JFrame)w;
            f.getRootPane().setDefaultButton(okButton);
            f.setResizable(false);
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            ActionListener closeAction = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    f.setVisible(false);
                    f.dispose();
                }
            };
            f.getRootPane().registerKeyboardAction(closeAction, ks, JComponent.WHEN_IN_FOCUSED_WINDOW);
        } else if (w instanceof JDialog) {
            final JDialog d = (JDialog)w;
            d.getRootPane().setDefaultButton(okButton);
            d.setResizable(false);
            KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            ActionListener closeAction = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    d.setVisible(false);
                }
            };
            d.getRootPane().registerKeyboardAction(closeAction, ks, JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        w.pack();
        w.setLocation(WindowUtils.getPointForCentering(w));
    }
    
    private void setButtonPanel(JXBtnPanel buttonPanel) {
		this.buttonPanel = buttonPanel;
	}
	private static class JXBtnPanel extends JXPanel {

        private JButton cancel;
        private JButton ok;

        public JXBtnPanel(JButton okButton, JButton cancelButton) {
        	GridLayout layout = new GridLayout(1,2);
            layout.setHgap(5);
            setLayout(layout);
            this.ok = okButton;
            this.cancel = cancelButton;
            add(okButton);
            add(cancelButton);
            setBorder(new EmptyBorder(0,0,7,11));
        }

        /**
         * @return the cancel
         */
        public JButton getCancel() {
            return cancel;
        }

        /**
         * @return the ok
         */
        public JButton getOk() {
            return ok;
        }
        
    }
    
    private class CapsOnTest {
        
        KeyEventDispatcher ked;

        public void runTest() {
            boolean success = false;
            // there's an issue with this - http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4414164
            // TODO: check the progress from time to time
            //try {
            //     java.awt.Toolkit.getDefaultToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK);
            //     System.out.println("GOTCHA");
            //} catch (Exception ex) {
            //ex.printStackTrace();
            //success = false;
            //}
            if (!success) {
                try {
                    KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                    // #swingx-697
                    // In some cases panel is not focused after the creation leaving bogus dispatcher in place. If found remove it.
                    if (ked != null) {
                        kfm.removeKeyEventDispatcher(ked);
                    }
                    // Temporarily installed listener with auto-uninstall after
                    // test is finished.
                    ked = new KeyEventDispatcher() {
                        public boolean dispatchKeyEvent(KeyEvent e) {
                            if (e.getID() != KeyEvent.KEY_PRESSED) {
                                return true;
                            }
                            if (isTestingCaps && e.getKeyCode() > 64 && e.getKeyCode() < 91) {
                                setCapsLock(!e.isShiftDown() && Character.isUpperCase(e.getKeyChar()));
                            }
                            if (isTestingCaps && (e.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
                                // uninstall
                                isTestingCaps = false;
                                KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
                                if (ked == this) {
                                    ked = null;
                                }
                            }
                            return true;
                        }
                    };
                    kfm.addKeyEventDispatcher(ked);
                    Robot r = new Robot();
                    isTestingCaps = true;
                    r.keyPress(65);
                    r.keyRelease(65);
                    r.keyPress(KeyEvent.VK_BACK_SPACE);
                    r.keyRelease(KeyEvent.VK_BACK_SPACE);
                } catch (Exception e1) {
                    // this can happen for example due to security reasons in unsigned applets
                    // when we can't test caps lock state programatically bail out silently
                }
            }
        }
    }

    /**
     * Window event listener to invoke capslock test when login panel get activated.
     */
    public static class CapsOnWinListener extends WindowAdapter implements
			WindowFocusListener {
		private CapsOnTest cot;
		private long stamp;

		public CapsOnWinListener(CapsOnTest cot) {
			this.cot = cot;
		}

		public void windowActivated(WindowEvent e) {
			cot.runTest();
			stamp = System.currentTimeMillis();
		}

		public void windowGainedFocus(WindowEvent e) {
		    System.out.println("winFocusGained");
			// repeat test only if more then 20ms passed between activation test and now.
			if (stamp + 20 < System.currentTimeMillis()) {
				cot.runTest();
			}
		}

		public void windowLostFocus(WindowEvent e) {
			// ignore
		}

	}
}
