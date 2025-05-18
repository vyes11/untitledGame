package thegame.utils;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailSender {
    private static final String FROM_EMAIL = "levianoxwijaya@gmail.com"; // Change to your email
    private static final String FROM_PASSWORD = "iavh ggpe kfex rvtl"; // Use app password for better security
    
    public static boolean sendVerificationEmail(String toEmail, String verificationCode) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com"); // Use your SMTP server
            props.put("mail.smtp.port", "587");
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Color Match Puzzle - Email Verification");
            
            String emailContent = 
                "Hello,\n\n" +
                "Thank you for registering with Color Match Puzzle!\n\n" +
                "Your verification code is: " + verificationCode + "\n\n" +
                "Please enter this code in the game to complete your registration.\n\n" +
                "If you did not request this, please ignore this email.";
            
            message.setText(emailContent);
            
            Transport.send(message);
            System.out.println("Verification email sent to " + toEmail);
            return true;
        } catch (MessagingException e) {
            System.err.println("Failed to send verification email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
