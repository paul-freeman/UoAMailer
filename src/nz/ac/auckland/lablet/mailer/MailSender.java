package nz.ac.auckland.lablet.mailer;


import java.util.Date;
import java.util.Properties;
import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


public class MailSender {
    private String _user;
    private String _pass;

    private String[] _to;
    private String _from;

    private String _port;

    private String _host;

    private String _subject;
    private String _body;

    private boolean _debuggable;

    private Multipart _multipart;

    public MailSender() {
        _host = "smtp.googlemail.com"; // default smtp server
        _port = "465"; // default smtp port

        _user = ""; // username
        _pass = ""; // password
        _from = ""; // email sent from
        _subject = ""; // email subject
        _body = ""; // email body

        _debuggable = true; // debug mode on or off - default off

        _multipart = new MimeMultipart();

        // There is something wrong with MailCap, javamail can not find a handler for the multipart/mixed part, so this bit needs to be added.
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        CommandMap.setDefaultCommandMap(mc);
    }

    public MailSender(String user, String pass) {
        this();

        _user = user;
        _pass = pass;
    }

    public boolean send() throws Exception {
        Properties props = _setProperties();

        if(!_user.equals("") && !_pass.equals("") && _to.length > 0 && !_from.equals("") && !_subject.equals("") && !_body.equals("")) {
            Session session = Session.getDefaultInstance(props,
                    new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(_user, _pass);
                        }
                    });

            MimeMessage msg = new MimeMessage(session);

            msg.setFrom(new InternetAddress(_from));

            InternetAddress[] addressTo = new InternetAddress[_to.length];
            for (int i = 0; i < _to.length; i++) {
                addressTo[i] = new InternetAddress(_to[i]);
            }
            msg.setRecipients(MimeMessage.RecipientType.TO, addressTo);

            msg.setSubject(_subject);
            msg.setSentDate(new Date());

            // setup message body
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(_body);
            _multipart.addBodyPart(messageBodyPart);

            // Put parts in message
            msg.setContent(_multipart);

            // send email
            Transport transport = session.getTransport("smtps");
            transport.connect(_host, _user, _pass);
            transport.sendMessage(msg, addressTo);

            //Transport.send(msg);

            return true;
        } else {
            return false;
        }
    }

    public void addAttachment(String filename) throws Exception {
        BodyPart messageBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(filename);
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(filename);

        _multipart.addBodyPart(messageBodyPart);
    }

    private Properties _setProperties() {
        Properties props = new Properties();

        props.put("mail.smtps.host", _host);

        if(_debuggable) {
            props.put("mail.debug", "true");
        }

        props.put("mail.smtps.auth.mechanisms", "PLAIN");
        props.put("mail.smtp.auth.mechanisms", "PLAIN");
        props.put("mail.smtps.auth.login.disable", true);
        props.put("mail.smtps.ssl.enable", true);
        props.put("mail.smtps.ssl.trust", "*");
        //props.put("mail.smtps.auth", "true");
        props.put("mail.smtps.port", _port);
        props.put("mail.smtps.socketFactory.port", _port);
        props.put("mail.smtps.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");

        return props;
    }

    // the getters and setters
    public String getBody() {
        return _body;
    }

    public void setBody(String _body) {
        this._body = _body;
    }

    public String getUser() {
        return _user;
    }

    public void setUser(String _user) {
        this._user = _user;
    }

    public void setPassword(String _pass) {
        this._pass = _pass;
    }

    public String[] getTo() {
        return _to;
    }

    public void setTo(String[] _to) {
        this._to = _to;
    }

    public String getFrom() {
        return _from;
    }

    public void setFrom(String _from) {
        this._from = _from;
    }

    public String getPort() {
        return _port;
    }

    public void setPort(String _port) {
        this._port = _port;
    }

    public String getHost() {
        return _host;
    }

    public void setHost(String _host) {
        this._host = _host;
    }

    public String getSubject() {
        return _subject;
    }

    public void setSubject(String _subject) {
        this._subject = _subject;
    }
}