package tw.com.sc.monitor.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Component
public class SendMailService {
    private static final Logger log = LoggerFactory.getLogger(SendMailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${monitor.mail.to}")
    private String[] mailTo;
    
    @Value("${monitor.mail.from}")
    private String mailFrom;
    
    public void sendMail(MailContent content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(mailFrom);
            helper.setTo(mailTo);
            helper.setSubject(content.getSubject());
            helper.setText(content.getContent(), true);
            
            mailSender.send(message);
            log.info("監控指標郵件發送成功");
        } catch (MessagingException e) {
            log.error("發送監控指標郵件時發生錯誤", e);
        }
    }
}
