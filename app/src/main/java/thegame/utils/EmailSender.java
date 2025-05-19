package thegame.utils;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Utility class for sending emails.
 * Used primarily for account verification.
 */
public class EmailSender {
    private static final String FROM_EMAIL = "levianoxwijaya@gmail.com"; // Change to your email
    private static final String FROM_PASSWORD = "ycvf mskg yrwb cinn"; // Use app password for better security
    
    /**
     * Sends a verification email with a code to the specified address.
     *
     * @param toEmail The email address to send the verification code to
     * @param verificationCode The verification code to include in the email
     * @return true if the email was sent successfully, false otherwise
     */
    public static boolean sendVerificationEmail(String toEmail, String verificationCode) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            props.put("mail.smtp.timeout", "10000");
            
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
            return true;
        } catch (MessagingException e) {
            if (e.getCause() instanceof java.net.ConnectException) {
                System.err.println("Network error: Unable to connect to email server. Please check your internet connection or firewall.");
            } else {
                System.err.println("Failed to send verification email: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }
}
