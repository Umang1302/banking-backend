package com.nedbank.banking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    /**
     * Send bank statement via email
     * Note: This is a placeholder implementation. In production, integrate with
     * email service providers like SendGrid, AWS SES, or SMTP
     */
    public void sendStatementEmail(String toEmail, String customerName, String accountNumber,
                                   LocalDate startDate, LocalDate endDate, byte[] pdfAttachment) {
        log.info("Sending statement email to: {}", toEmail);
        
        try {
            // TODO: Implement actual email sending logic
            // Example with JavaMailSender:
            /*
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(toEmail);
            helper.setSubject("Your Bank Statement - " + accountNumber);
            helper.setText(buildEmailBody(customerName, accountNumber, startDate, endDate), true);
            
            // Attach PDF
            helper.addAttachment("statement.pdf", new ByteArrayResource(pdfAttachment));
            
            mailSender.send(message);
            */
            
            log.info("Statement email sent successfully to {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send statement email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Build email body HTML
     */
    private String buildEmailBody(String customerName, String accountNumber, 
                                  LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>Bank Statement</h2>
                <p>Dear %s,</p>
                <p>Please find attached your bank statement for account <strong>%s</strong></p>
                <p>Statement Period: <strong>%s to %s</strong></p>
                <br>
                <p>If you have any questions, please contact our customer service.</p>
                <br>
                <p>Best regards,<br>Nedbank Team</p>
                <hr>
                <small style="color: #666;">
                    This is an automated email. Please do not reply to this message.
                </small>
            </body>
            </html>
            """, 
            customerName, 
            accountNumber, 
            startDate.format(formatter), 
            endDate.format(formatter)
        );
    }

    /**
     * Send transaction notification email
     */
    public void sendTransactionNotification(String toEmail, String customerName, 
                                           String transactionType, String amount, 
                                           String accountNumber) {
        log.info("Sending transaction notification to: {}", toEmail);
        
        try {
            // TODO: Implement email sending
            log.info("Transaction notification sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send transaction notification: {}", e.getMessage());
        }
    }
}

