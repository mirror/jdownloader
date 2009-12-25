//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
    private final String host;
    private String user;
    private String pass;
    private String senderEmail;
    private String senderName;

    public Email(final String smtpHost) {
        this.host = smtpHost;
    }

    public Email(final String smtpHost, final String user, final String pass) {
        this.host = smtpHost;
        this.user = user;
        this.pass = pass;
    }

    public void setSender(final String email, final String name) {
        this.senderEmail = email;
        this.senderName = name;
    }

    public String getSenderName() {
        return this.senderName;
    }

    public void sendEmail(final String email, final String name, final String subject, final String message) throws MessagingException {
        final Properties props = new Properties();
        props.put("mail.smtp.host", host);
        Session session;
        if (user != null) {
            props.put("mail.smtp.auth", "true");
            session = Session.getDefaultInstance(props, new MailAuthenticator(user, pass));
        } else {
            session = Session.getDefaultInstance(props);
        }

        final Message msg = new MimeMessage(session);

        msg.setFrom(new InternetAddress(senderEmail));

        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
        msg.setSentDate(new Date());
        msg.setSubject(subject);
        msg.setContent(message, "text/plain");
        Transport.send(msg);
    }

    static class MailAuthenticator extends Authenticator {

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
        public MailAuthenticator(final String user, final String password) {
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
