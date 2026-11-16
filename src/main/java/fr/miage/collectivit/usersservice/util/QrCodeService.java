package fr.miage.collectivit.usersservice.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QrCodeService {

  private static final int DEFAULT_SIZE = 220;

  public byte[] generateQrCodePng(String content) {
    return generateQrCodePng(content, DEFAULT_SIZE, DEFAULT_SIZE);
  }

  public byte[] generateQrCodePng(String content, int width, int height) {
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("Content cannot be null or blank for QR code generation");
    }
    try {
      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      BitMatrix bitMatrix =
          qrCodeWriter.encode(content.trim(), BarcodeFormat.QR_CODE, width, height);
      ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
      return pngOutputStream.toByteArray();
    } catch (WriterException | IOException e) {
      log.error("Failed to generate QR code for content {}: {}", content, e.getMessage());
      throw new IllegalStateException("Failed to generate QR code image", e);
    }
  }
}
