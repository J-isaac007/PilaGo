package com.queupid.pilago.web.qr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * One job: turn a piece of text into a QR code PNG.
 * Nothing outside this class needs to know about ZXing.
 */
public final class QrCodes {

    private QrCodes() {
    }

    public static byte[] png(String text, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter()
                    .encode(text, BarcodeFormat.QR_CODE, size, size, Map.of(EncodeHintType.MARGIN, 2));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Could not create the QR code", e);
        }
    }
}
