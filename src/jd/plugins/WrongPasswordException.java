package jd.plugins;

import org.jdownloader.translate._JDT;

public class WrongPasswordException extends PluginException {
    /*
     * This is just an idea for the ability to add the type of password in order to have nicer UI translation such as
     * "Wrong folder password" or "Wrong decryption key".
     */
    enum PasswordType {
        FILE,
        FOLDER,
        DECRYPT_KEY
    }

    /*
     * The kind of failure e.g. user entered invalid password or we expect a password with a certain length or structure but what the user
     * has entered doesn't match that criteria.
     */
    enum FailureType {
        INVALID_PASSWORD,
        INVALID_PATTERN
    }

    private Boolean clearExistingPassword = null;
    private Boolean allowRetry            = null;

    public Boolean getClearExistingPassword() {
        return clearExistingPassword;
    }

    public void setClearExistingPassword(Boolean clearExistingPassword) {
        this.clearExistingPassword = clearExistingPassword;
    }

    /**
     * Defines whether or not upper system is allowed to retry when this exception is thrown. </br>
     * Typically if a plugin throws this exception and does not allow a retry this means that the plugin aleady asked the user multiple
     * times for the password in-place.
     */
    public Boolean getAllowRetry() {
        return allowRetry;
    }

    public void setAllowRetry(Boolean allowRetry) {
        this.allowRetry = allowRetry;
    }

    /**
     * Throw this Exception whenever a previously entered download- or crawler password turned out to be wrong. </br>
     * Examples: </br>
     * - Password to access a cloud folder </br>
     * - password to download a single file </br>
     * - password to decrypt a single file </br>
     * By default, this may delete the currently stored password! </br>
     * <b> Do not use this in context of extraction of password protected archives! </b> </br>
     * Tags: Wrong password, invalid password, folderpassword, filepassword
     */
    public WrongPasswordException() {
        this(null);
        // super(LinkStatus.ERROR_RETRY);
    }

    protected WrongPasswordException(final Boolean clearExistingPassword) {
        // TODO: Add translation
        super(LinkStatus.ERROR_RETRY, _JDT.T.plugins_errors_wrongpassword());
        this.setClearExistingPassword(clearExistingPassword);
    }
}
