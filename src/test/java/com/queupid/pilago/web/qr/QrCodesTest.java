package com.queupid.pilago.web.qr;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

class QrCodesTest {

    @Test
    void producesAPng() {
        byte[] png = QrCodes.png("https://example.com/", 300);
        assertArrayEquals(new byte[] {(byte) 0x89, 'P', 'N', 'G'}, Arrays.copyOf(png, 4));
    }

    @Test
    void theCodeReadsBackAsTheSameText() throws Exception {
        String text = "https://example.com/ticket/abc";
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(QrCodes.png(text, 300)));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        assertEquals(text, new MultiFormatReader().decode(bitmap).getText());
    }
}
