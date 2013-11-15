/*
 * Source: http://deathbycaptcha.eu/user/api
 * Slightly modified to work without json and base64 dependencies
 */
package org.jdownloader.captcha.v2.solver.dbc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Base Death by Captcha API client.
 * 
 */
abstract public class Client {
    final static public String API_VERSION        = "DBC/Java v4.1.0";
    final static public int    SOFTWARE_VENDOR_ID = 0;

    final static public int    DEFAULT_TIMEOUT    = 60;
    final static public int    POLLS_INTERVAL     = 5;

    /**
     * Client verbosity flag.
     * 
     * When it's set to true, the client will dump API calls for debug purpose.
     */
    public boolean             isVerbose          = false;

    protected String           _username          = "";
    protected String           _password          = "";

    protected void log(String call, String msg) {
        if (this.isVerbose) {
            System.out.println((System.currentTimeMillis() / 1000) + " " + call + (null != msg ? ": " + msg : ""));
        }
    }

    protected void log(String call) {
        this.log(call, null);
    }

    protected DataObject getCredentials() {

        return new DataObject().put("username", this._username).put("password", this._password);

    }

    protected byte[] load(InputStream st) throws IOException {
        int n = 0, offset = 0;
        byte[] img = new byte[0];
        while (true) {
            try {
                n = st.available();
            } catch (IOException e) {
                n = 0;
            }
            if (0 < n) {
                if (offset + n > img.length) {
                    img = java.util.Arrays.copyOf(img, img.length + n);
                }
                offset += st.read(img, offset, n);
            } else {
                break;
            }
        }
        return img;
    }

    protected byte[] load(File f) throws IOException, FileNotFoundException {
        InputStream st = new FileInputStream(f);
        try {
            return this.load(st);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            st.close();
        }
    }

    protected byte[] load(String fn) throws IOException, FileNotFoundException {
        return this.load(new File(fn));
    }

    /**
     * Closes opened connections (if any), cleans up resources.
     */
    abstract public void close();

    /**
     * Opens API-specific connection if not opened yet.
     * 
     * @return true on success
     */
    abstract public boolean connect() throws IOException;

    /**
     * @param username
     *            DBC account username
     * @param password
     *            DBC account password
     */
    public Client(String username, String password) {
        this._username = username;
        this._password = password;
    }

    /**
     * Fetches user details.
     * 
     * @return user details object
     */
    abstract public User getUser() throws IOException, Exception;

    /**
     * Fetches user balance (in US cents).
     * 
     * @return user balance
     */
    public double getBalance() throws IOException, Exception {
        return this.getUser().balance;
    }

    /**
     * Uploads a CAPTCHA to the service.
     * 
     * @param img
     *            CAPTCHA image byte vector
     * @return CAPTCHA object on success, null otherwise
     */
    abstract public Captcha upload(byte[] img) throws IOException, Exception;

    /**
     * @see org.jdownloader.captcha.v2.solver.dbc.Client#upload
     * @param st
     *            CAPTCHA image stream
     */
    public Captcha upload(InputStream st) throws IOException, Exception {
        return this.upload(this.load(st));
    }

    /**
     * @see org.jdownloader.captcha.v2.solver.dbc.Client#upload
     * @param f
     *            CAPTCHA image file
     */
    public Captcha upload(File f) throws IOException, FileNotFoundException, Exception {
        return this.upload(this.load(f));
    }

    /**
     * @see org.jdownloader.captcha.v2.solver.dbc.Client#upload
     * @param fn
     *            CAPTCHA image file name
     */
    public Captcha upload(String fn) throws IOException, FileNotFoundException, Exception {
        return this.upload(this.load(fn));
    }

    /**
     * Fetches an uploaded CAPTCHA details.
     * 
     * @param id
     *            CAPTCHA ID
     * @return CAPTCHA object if found, null otherwise
     */
    abstract public Captcha getCaptcha(int id) throws IOException, Exception;

    /**
     * @see org.jdownloader.captcha.v2.solver.dbc.Client#getCaptcha
     * @param captcha
     *            CAPTCHA object
     */
    public Captcha getCaptcha(Captcha captcha) throws IOException, Exception {
        return this.getCaptcha(captcha.id);
    }

    /**
     * Fetches an uploaded CAPTCHA text.
     * 
     * @param id
     *            CAPTCHA ID
     * @return CAPTCHA text if solved, null otherwise
     */
    public String getText(int id) throws IOException, Exception {
        return this.getCaptcha(id).text;
    }

    /**
     * @see org.jdownloader.captcha.v2.solver.dbc.Client#getText
     * @param captcha
     *            CAPTCHA object
     */
    public String getText(Captcha captcha) throws IOException, Exception {
        return this.getText(captcha.id);
    }

    /**
     * Reports an incorrectly solved CAPTCHA
     * 
     * @param id
     *            CAPTCHA ID
     * @return true on success
     */
    abstract public boolean report(int id) throws IOException, Exception;

    /**
     * @see org.jdownloader.captcha.v2.solver.dbc.Client#report
     * @param captcha
     *            CAPTCHA object
     */
    public boolean report(Captcha captcha) throws IOException, Exception {
        return this.report(captcha.id);
    }

    /**
     * Tries to solve a CAPTCHA by uploading it and polling for its status and text with arbitrary timeout.
     * 
     * @param img
     *            CAPTCHA image byte vector
     * @param timeout
     *            Solving timeout (in seconds)
     * @return CAPTCHA object if uploaded and correctly solved, null otherwise
     */
    public Captcha decode(byte[] img, int timeout) throws IOException, Exception, InterruptedException {
        long deadline = System.currentTimeMillis() + (0 < timeout ? timeout : Client.DEFAULT_TIMEOUT) * 1000;
        Captcha captcha = this.upload(img);
        if (null != captcha) {
            while (deadline > System.currentTimeMillis() && !captcha.isSolved()) {
                Thread.sleep(Client.POLLS_INTERVAL * 1000);
                captcha = this.getCaptcha(captcha.id);
            }
            if (captcha.isSolved() && captcha.isCorrect()) { return captcha; }
        }
        return null;
    }

    /**
     * @see org.jdownloader.captcha.v2.solver.dbc.Client#decode(byte[], int)
     */
    public Captcha decode(byte[] img) throws IOException, Exception, InterruptedException {
        return this.decode(img, 0);
    }

    /**
     * @see org.jdownloader.captcha.v2.solver.dbc.Client#decode
     * @param st
     *            CAPTCHA image stream
     * @param timeout
     *            Solving timeout (in seconds)
     */
    public Captcha decode(InputStream st, int timeout) throws IOException, Exception, InterruptedException {
        return this.decode(this.load(st), timeout);
    }

    /**
     * @see org.jdownloader.captcha.v2.solver.dbc.Client#decode(InputStream, int)
     */
    public Captcha decode(InputStream st) throws IOException, Exception, InterruptedException {
        return this.decode(st, 0);
    }

    /**
     * @see org.jdownloader.captcha.v2.solver.dbc.Client#decode
     * @param f
     *            CAPTCHA image file
     * @param timeout
     *            Solving timeout (in seconds)
     */
    public Captcha decode(File f, int timeout) throws IOException, FileNotFoundException, Exception, InterruptedException {
        return this.decode(this.load(f), timeout);
    }

    /**
     * @see org.jdownloader.captcha.v2.solver.dbc.Client#decode(File, int)
     */
    public Captcha decode(File f) throws IOException, FileNotFoundException, Exception, InterruptedException {
        return this.decode(f, 0);
    }

    /**
     * @see org.jdownloader.captcha.v2.solver.dbc.Client#decode
     * @param fn
     *            CAPTCHA image file name
     * @param timeout
     *            Solving timeout (in seconds)
     */
    public Captcha decode(String fn, int timeout) throws IOException, FileNotFoundException, Exception, InterruptedException {
        return this.decode(this.load(fn), timeout);
    }

    /**
     * @see org.jdownloader.captcha.v2.solver.dbc.Client#decode(String, int)
     */
    public Captcha decode(String fn) throws IOException, FileNotFoundException, Exception, InterruptedException {
        return this.decode(fn, 0);
    }
}
