package fr.miage.collectivit.usersservice.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QrCodeServiceTest {

  private QrCodeService qrCodeService;

  @BeforeEach
  void setUp() {
    qrCodeService = new QrCodeService();
  }

  @Test
  @DisplayName("generateQrCodePng should generate valid non-empty PNG bytes for valid content")
  void shouldGenerateValidPngBytes() {
    byte[] pngBytes = qrCodeService.generateQrCodePng("C123456");

    assertThat(pngBytes).isNotNull().isNotEmpty();
    // Standard PNG header: 0x89 0x50 0x4E 0x47
    assertThat(pngBytes[0]).isEqualTo((byte) 0x89);
    assertThat(pngBytes[1]).isEqualTo((byte) 0x50);
    assertThat(pngBytes[2]).isEqualTo((byte) 0x4E);
    assertThat(pngBytes[3]).isEqualTo((byte) 0x47);
  }

  @Test
  @DisplayName(
      "generateQrCodePng should throw IllegalArgumentException when content is null or blank")
  void shouldThrowWhenContentIsBlank() {
    assertThatThrownBy(() -> qrCodeService.generateQrCodePng(null))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> qrCodeService.generateQrCodePng("   "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
