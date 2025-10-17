package com.nedbank.banking.service;

import com.nedbank.banking.dto.StatementRequest;
import com.nedbank.banking.dto.TransactionResponse;
import com.nedbank.banking.entity.Account;
import com.nedbank.banking.entity.Customer;
import com.nedbank.banking.entity.Transaction;
import com.nedbank.banking.entity.User;
import com.nedbank.banking.repository.AccountRepository;
import com.nedbank.banking.repository.TransactionRepository;
import com.nedbank.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Generate bank statement as PDF
     */
    @Transactional(readOnly = true)
    public byte[] generatePdfStatement(StatementRequest request) {
        User currentUser = getCurrentUser();
        
        // Use eager loading to fetch account with customer
        Account account = accountRepository.findByAccountNumberWithCustomer(request.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + request.getAccountNumber()));

        // Check access
        if (!hasAccessToAccount(currentUser, account)) {
            throw new SecurityException("You don't have permission to generate statement for this account");
        }

        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().atTime(23, 59, 59);

        // First, check how many total transactions exist for this account
        List<Transaction> allTransactions = transactionRepository.findByAccountId(account.getId());
        log.info("Account {} (ID: {}) has {} total transactions in database", 
                account.getAccountNumber(), account.getId(), allTransactions.size());
        
        if (!allTransactions.isEmpty()) {
            Transaction firstTxn = allTransactions.get(0);
            Transaction lastTxn = allTransactions.get(allTransactions.size() - 1);
            log.info("Transaction date range in DB: {} to {}", 
                    lastTxn.getTransactionDate(), firstTxn.getTransactionDate());
        }

        // Now fetch transactions for the requested date range
        List<Transaction> transactions = transactionRepository.findByAccountIdAndDateRange(
                account.getId(), startDateTime, endDateTime);

        log.info("Generating PDF statement for account {} with {} transactions (Requested period: {} to {})", 
                account.getAccountNumber(), transactions.size(), startDateTime, endDateTime);
        
        if (transactions.isEmpty() && !allTransactions.isEmpty()) {
            log.warn("Date range mismatch! Account has {} total transactions but 0 in requested range {} to {}", 
                    allTransactions.size(), startDateTime, endDateTime);
            log.warn("Suggestion: Try using date range from {} to {}", 
                    allTransactions.get(allTransactions.size() - 1).getTransactionDate().toLocalDate(),
                    allTransactions.get(0).getTransactionDate().toLocalDate());
        }

        byte[] pdfBytes = createPdfStatement(account, transactions, request.getStartDate(), request.getEndDate());

        // Send email if requested
        if (request.isSendEmail() && request.getEmailAddress() != null) {
            emailService.sendStatementEmail(
                    request.getEmailAddress(),
                    account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName(),
                    account.getAccountNumber(),
                    request.getStartDate(),
                    request.getEndDate(),
                    pdfBytes
            );
            log.info("Statement emailed to {}", request.getEmailAddress());
        }

        return pdfBytes;
    }

    /**
     * Create PDF document using OpenPDF
     */
    private byte[] createPdfStatement(Account account, List<Transaction> transactions, 
                                     LocalDate startDate, LocalDate endDate) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();
            
            Customer customer = account.getCustomer();
            
            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
            
            // Title
            Paragraph title = new Paragraph("BANKOFPEOPLE ACCOUNT STATEMENT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);
            
            // Customer Information Section
            Paragraph customerInfoHeader = new Paragraph("Customer Information", headerFont);
            customerInfoHeader.setSpacingAfter(10f);
            document.add(customerInfoHeader);
            
            document.add(new Paragraph("Name: " + customer.getFirstName() + " " + customer.getLastName(), normalFont));
            document.add(new Paragraph("Customer Number: " + customer.getCustomerNumber(), normalFont));
            document.add(new Paragraph("Account Number: " + account.getAccountNumber(), normalFont));
            document.add(new Paragraph("Account Type: " + account.getAccountType(), normalFont));
            document.add(new Paragraph("Statement Period: " + startDate + " to " + endDate, normalFont));
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), normalFont));
            
            Paragraph spacing1 = new Paragraph(" ");
            spacing1.setSpacingAfter(15f);
            document.add(spacing1);
            
            // Account Summary Section
            Paragraph summaryHeader = new Paragraph("Account Summary", headerFont);
            summaryHeader.setSpacingAfter(10f);
            document.add(summaryHeader);
            
            document.add(new Paragraph("Current Balance: " + account.getCurrency() + " " + 
                    String.format("%.2f", account.getBalance()), normalFont));
            document.add(new Paragraph("Available Balance: " + account.getCurrency() + " " + 
                    String.format("%.2f", account.getAvailableBalance()), normalFont));
            document.add(new Paragraph("Total Transactions: " + transactions.size(), normalFont));
            
            // Calculate totals
            BigDecimal totalDebits = transactions.stream()
                    .filter(Transaction::isDebit)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalCredits = transactions.stream()
                    .filter(Transaction::isCredit)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            document.add(new Paragraph("Total Debits: " + account.getCurrency() + " " + 
                    String.format("%.2f", totalDebits), normalFont));
            document.add(new Paragraph("Total Credits: " + account.getCurrency() + " " + 
                    String.format("%.2f", totalCredits), normalFont));
            
            Paragraph spacing2 = new Paragraph(" ");
            spacing2.setSpacingAfter(15f);
            document.add(spacing2);
            
            // Transaction History Section
            Paragraph transactionHeader = new Paragraph("Transaction History", headerFont);
            transactionHeader.setSpacingAfter(10f);
            document.add(transactionHeader);
            
            log.info("Adding transactions to PDF. Count: {}", transactions.size());
            
            if (!transactions.isEmpty()) {
                try {
                    // Create table with 5 columns
                    PdfPTable table = new PdfPTable(5);
                    table.setWidthPercentage(100);
                    table.setSpacingBefore(10f);
                    table.setSpacingAfter(10f);
                    
                    // Set column widths
                    float[] columnWidths = {15f, 15f, 15f, 15f, 40f};
                    table.setWidths(columnWidths);
                    
                    // Table headers
                    PdfPCell headerCell;
                    String[] headers = {"Date", "Type", "Amount", "Balance", "Description"};
                    for (String header : headers) {
                        headerCell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
                        headerCell.setBackgroundColor(new Color(200, 200, 200));
                        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        headerCell.setPadding(5);
                        table.addCell(headerCell);
                    }
                    
                    log.info("Table headers added. Adding {} transaction rows...", transactions.size());
                    
                    // Table data
                    int rowCount = 0;
                    for (Transaction txn : transactions) {
                        // Date
                        PdfPCell cell = new PdfPCell(new Phrase(
                                txn.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), 
                                smallFont));
                        cell.setPadding(5);
                        table.addCell(cell);
                        
                        // Type
                        cell = new PdfPCell(new Phrase(txn.getTransactionType(), smallFont));
                        cell.setPadding(5);
                        table.addCell(cell);
                        
                        // Amount
                        String amountStr = (txn.isDebit() ? "-" : "+") + String.format("%.2f", txn.getAmount());
                        Font amountFont = new Font(smallFont);
                        if (txn.isDebit()) {
                            amountFont.setColor(Color.RED);
                        } else {
                            amountFont.setColor(new Color(0, 128, 0));
                        }
                        cell = new PdfPCell(new Phrase(amountStr, amountFont));
                        cell.setPadding(5);
                        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        table.addCell(cell);
                        
                        // Balance
                        cell = new PdfPCell(new Phrase(String.format("%.2f", txn.getBalanceAfter()), smallFont));
                        cell.setPadding(5);
                        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        table.addCell(cell);
                        
                        // Description
                        String descStr = txn.getDescription() != null ? txn.getDescription() : "";
                        cell = new PdfPCell(new Phrase(descStr, smallFont));
                        cell.setPadding(5);
                        table.addCell(cell);
                        
                        rowCount++;
                    }
                    
                    log.info("Added {} transaction rows to table. Now adding table to document...", rowCount);
                    document.add(table);
                    log.info("Transaction table added to PDF successfully");
                    
                } catch (Exception e) {
                    log.error("Error creating transaction table in PDF", e);
                    document.add(new Paragraph("Error displaying transactions: " + e.getMessage(), normalFont));
                }
            } else {
                log.warn("No transactions to display in PDF");
                document.add(new Paragraph("No transactions found for the selected period.", normalFont));
            }
            
            // Footer
            Paragraph spacing3 = new Paragraph(" ");
            spacing3.setSpacingAfter(20f);
            document.add(spacing3);
            
            Paragraph footer = new Paragraph("END OF STATEMENT", 
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
            
            document.close();
            return outputStream.toByteArray();
            
        } catch (DocumentException e) {
            log.error("Error generating PDF statement: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF statement", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found: " + username));
    }

    private boolean hasAccessToAccount(User user, Account account) {
        if (user.hasPermission("ACCOUNT_READ")) {
            return true;
        }
        if (user.getCustomer() != null) {
            return account.getCustomer().getId().equals(user.getCustomer().getId());
        }
        return false;
    }
}

