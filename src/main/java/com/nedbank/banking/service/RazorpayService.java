package com.nedbank.banking.service;

import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Map;

/**
 * Service wrapper for Razorpay SDK operations
 * Handles payment orders, QR codes, and payment verification
 */
@Service
@Slf4j
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    
    @Value("${razorpay.key.id}")
    private String razorpayKeyId;
    
    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;
    
    @Value("${razorpay.currency:INR}")
    private String defaultCurrency;
    
    @Value("${razorpay.payment.callback.url}")
    private String callbackUrl;

    public RazorpayService(
            @Value("${razorpay.key.id}") String keyId,
            @Value("${razorpay.key.secret}") String keySecret
    ) throws RazorpayException {
        this.razorpayClient = new RazorpayClient(keyId, keySecret);
        log.info("Razorpay client initialized successfully");
    }

    /**
     * Create a Razorpay order for payment
     */
    public Order createOrder(BigDecimal amount, String currency, String receiptId, String description) throws RazorpayException {
        try {
            // Convert amount to paise (Razorpay uses smallest currency unit)
            int amountInPaise = amount.multiply(new BigDecimal("100")).intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", currency != null ? currency : defaultCurrency);
            orderRequest.put("receipt", receiptId);
            
            if (description != null) {
                JSONObject notes = new JSONObject();
                notes.put("description", description);
                orderRequest.put("notes", notes);
            }

            Order order = razorpayClient.orders.create(orderRequest);
            log.info("Razorpay order created: {} for amount: {} {}", order.get("id"), amount, currency);
            
            return order;
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Create a payment order with specific options
     */
    public Order createPaymentOrder(
            BigDecimal amount,
            String currency,
            String receiptId,
            String description,
            Map<String, Object> notes
    ) throws RazorpayException {
        try {
            int amountInPaise = amount.multiply(new BigDecimal("100")).intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", currency != null ? currency : defaultCurrency);
            orderRequest.put("receipt", receiptId);
            orderRequest.put("payment_capture", 1); // Auto capture

            if (notes != null && !notes.isEmpty()) {
                orderRequest.put("notes", new JSONObject(notes));
            }

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id");
            log.info("Payment order created: {}", orderId);
            
            return order;
        } catch (RazorpayException e) {
            log.error("Failed to create payment order: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Fetch payment details from Razorpay
     */
    public Payment fetchPayment(String paymentId) throws RazorpayException {
        try {
            Payment payment = razorpayClient.payments.fetch(paymentId);
            log.info("Fetched payment details for: {}", paymentId);
            return payment;
        } catch (RazorpayException e) {
            log.error("Failed to fetch payment {}: {}", paymentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Fetch order details from Razorpay
     */
    public Order fetchOrder(String orderId) throws RazorpayException {
        try {
            Order order = razorpayClient.orders.fetch(orderId);
            log.info("Fetched order details for: {}", orderId);
            return order;
        } catch (RazorpayException e) {
            log.error("Failed to fetch order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Verify payment signature
     */
    public boolean verifyPaymentSignature(
            String orderId,
            String paymentId,
            String signature
    ) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);
            
            if (isValid) {
                log.info("Payment signature verified successfully for payment: {}", paymentId);
            } else {
                log.warn("Payment signature verification failed for payment: {}", paymentId);
            }
            
            return isValid;
        } catch (RazorpayException e) {
            log.error("Error verifying payment signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate payment signature (for testing/validation)
     */
    public String generateSignature(String orderId, String paymentId) throws SignatureException {
        try {
            String payload = orderId + "|" + paymentId;
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating signature: {}", e.getMessage(), e);
            throw new SignatureException("Failed to generate signature", e);
        }
    }

    /**
     * Create payment link for QR/UPI payments
     */
    public String createPaymentLink(String orderId, String description) {
        // For local development, we'll use Razorpay checkout link format
        // In production, you might want to use Razorpay Payment Links API
        return String.format(
                "https://api.razorpay.com/v1/checkout/embedded?order_id=%s&key_id=%s",
                orderId,
                razorpayKeyId
        );
    }

    /**
     * Check payment status
     */
    public String getPaymentStatus(String paymentId) throws RazorpayException {
        try {
            Payment payment = fetchPayment(paymentId);
            String status = payment.get("status");
            log.info("Payment {} status: {}", paymentId, status);
            return status;
        } catch (RazorpayException e) {
            log.error("Failed to get payment status: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Check if payment is captured/successful
     */
    public boolean isPaymentCaptured(String paymentId) throws RazorpayException {
        String status = getPaymentStatus(paymentId);
        return "captured".equalsIgnoreCase(status);
    }

    /**
     * Get order status
     */
    public String getOrderStatus(String orderId) throws RazorpayException {
        try {
            Order order = fetchOrder(orderId);
            String status = order.get("status");
            log.info("Order {} status: {}", orderId, status);
            return status;
        } catch (RazorpayException e) {
            log.error("Failed to get order status: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Convert amount from paise to rupees
     */
    public BigDecimal convertPaiseToRupees(int amountInPaise) {
        return new BigDecimal(amountInPaise).divide(new BigDecimal("100"));
    }

    /**
     * Convert amount from rupees to paise
     */
    public int convertRupeesToPaise(BigDecimal amountInRupees) {
        return amountInRupees.multiply(new BigDecimal("100")).intValue();
    }

    /**
     * Get Razorpay key ID (for frontend)
     */
    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    /**
     * Get callback URL
     */
    public String getCallbackUrl() {
        return callbackUrl;
    }
}

