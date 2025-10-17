package com.nedbank.banking.service;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for QR code generation and parsing using ZXing
 */
@Service
@Slf4j
public class QRCodeService {

    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;
    private static final String QR_CODE_FORMAT = "PNG";

    /**
     * Generate QR code from text/data
     * Returns Base64 encoded PNG image
     */
    public String generateQRCode(String data) throws WriterException, IOException {
        return generateQRCode(data, QR_CODE_WIDTH, QR_CODE_HEIGHT);
    }

    /**
     * Generate QR code with custom dimensions
     */
    public String generateQRCode(String data, int width, int height) throws WriterException, IOException {
        try {
            log.info("Generating QR code for data length: {}", data.length());

            // Configure QR code parameters
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            // Generate QR code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);

            // Convert to BufferedImage
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            // Convert to Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(qrImage, QR_CODE_FORMAT, outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            log.info("QR code generated successfully, size: {} bytes", imageBytes.length);
            return base64Image;

        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR code: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Parse/decode QR code from Base64 image
     */
    public String parseQRCode(String base64Image) throws NotFoundException, IOException {
        try {
            log.info("Parsing QR code from Base64 image");

            // Remove data URL prefix if present
            String imageData = base64Image;
            if (base64Image.contains(",")) {
                imageData = base64Image.split(",")[1];
            }

            // Decode Base64 to byte array
            byte[] imageBytes = Base64.getDecoder().decode(imageData);

            // Convert to BufferedImage
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            BufferedImage bufferedImage = ImageIO.read(inputStream);

            if (bufferedImage == null) {
                throw new IOException("Failed to read image from Base64 data");
            }

            // Decode QR code
            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, java.util.Collections.singletonList(BarcodeFormat.QR_CODE));

            MultiFormatReader reader = new MultiFormatReader();
            Result result = reader.decode(bitmap, hints);

            String decodedText = result.getText();
            log.info("QR code parsed successfully, data length: {}", decodedText.length());

            return decodedText;

        } catch (NotFoundException e) {
            log.error("QR code not found in image: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("Failed to parse QR code: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Validate if a string is valid Base64 image data
     */
    public boolean isValidBase64Image(String base64String) {
        try {
            // Remove data URL prefix if present
            String imageData = base64String;
            if (base64String.contains(",")) {
                imageData = base64String.split(",")[1];
            }

            // Try to decode
            byte[] decoded = Base64.getDecoder().decode(imageData);
            
            // Try to read as image
            ByteArrayInputStream inputStream = new ByteArrayInputStream(decoded);
            BufferedImage image = ImageIO.read(inputStream);
            
            return image != null;
        } catch (Exception e) {
            log.debug("Invalid Base64 image: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate QR code with custom payment data
     * For internal QR format (BOP_PAY)
     */
    public String generatePaymentQRCode(
            String requestId,
            String accountNumber,
            String amount,
            String description
    ) throws WriterException, IOException {
        // Create structured payment data
        String paymentData = String.format(
                "BOP_PAY|%s|%s|%s|%s",
                requestId,
                accountNumber,
                amount,
                description != null ? description : ""
        );
        
        log.info("Generating structured payment QR code for request: {}", requestId);
        return generateQRCode(paymentData);
    }

    /**
     * Parse payment QR code (custom format)
     */
    public Map<String, String> parsePaymentQRCode(String base64Image) throws NotFoundException, IOException {
        String qrData = parseQRCode(base64Image);
        
        Map<String, String> paymentData = new HashMap<>();
        
        // Check if it's our custom format
        if (qrData.startsWith("BOP_PAY|")) {
            String[] parts = qrData.split("\\|");
            if (parts.length >= 4) {
                paymentData.put("type", "BOP");
                paymentData.put("requestId", parts[1]);
                paymentData.put("accountNumber", parts[2]);
                paymentData.put("amount", parts[3]);
                if (parts.length > 4) {
                    paymentData.put("description", parts[4]);
                }
            }
        } else if (qrData.startsWith("http")) {
            // It's a payment link
            paymentData.put("type", "PAYMENT_LINK");
            paymentData.put("paymentLink", qrData);
        } else {
            // Generic data
            paymentData.put("type", "GENERIC");
            paymentData.put("data", qrData);
        }
        
        return paymentData;
    }

    /**
     * Create data URL from Base64 image (for direct browser display)
     */
    public String createDataURL(String base64Image) {
        if (base64Image.startsWith("data:image")) {
            return base64Image;
        }
        return "data:image/png;base64," + base64Image;
    }

    /**
     * Extract Base64 data from data URL
     */
    public String extractBase64FromDataURL(String dataURL) {
        if (dataURL.contains(",")) {
            return dataURL.split(",")[1];
        }
        return dataURL;
    }
}

