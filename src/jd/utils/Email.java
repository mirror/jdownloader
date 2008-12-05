package jd.utils;

import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Email {
    private String host;
    private String user;
    private String pass;
    private String senderEmail;
    private String senderName;

    public Email(String smtpHost) {
        this.host = smtpHost;
    }

    public Email(String smtpHost, String user, String pass) {
        this.host = smtpHost;
        this.user = user;
        this.pass = pass;
    }

    public void setSender(String email, String name) {
        this.senderEmail = email;
        this.senderName = name;

    }

    public void sendEmail(String email, String name, String subject, String message) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        Session session;
        if (user != null) {
            MailAuthenticator auth = new MailAuthenticator(user, pass);
            props.put("mail.smtp.auth", "true");
            session = Session.getDefaultInstance(props, auth);
        } else {
            session = Session.getDefaultInstance(props);
        }

        Message msg = new MimeMessage(session);
    
        msg.setFrom( new InternetAddress(senderEmail));
       
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
        msg.setSentDate(new Date());
        msg.setSubject(subject);
        msg.setContent(message, "text/plain");
        Transport.send(msg);

    }

    class MailAuthenticator extends Authenticator {

        /**
         * Ein String, der den Usernamen nach der Erzeugung eines Objektes<br>
         * dieser Klasse enthalten wird.
         */
        private final String user;

        /**
         * Ein String, der das Passwort nach der Erzeugung eines Objektes<br>
         * dieser Klasse enthalten wird.
         */
        private final String password;

        /**
         * Der Konstruktor erzeugt ein MailAuthenticator Objekt<br>
         * aus den beiden Parametern user und passwort.
         * 
         * @param user
         *            String, der Username fuer den Mailaccount.
         * @param password
         *            String, das Passwort fuer den Mailaccount.
         */
        public MailAuthenticator(String user, String password) {
            this.user = user;
            this.password = password;
        }

        /**
         * Diese Methode gibt ein neues PasswortAuthentication Objekt zurueck.
         * 
         * @see javax.mail.Authenticator#getPasswordAuthentication()
         */
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(this.user, this.password);
        }
    }
}
