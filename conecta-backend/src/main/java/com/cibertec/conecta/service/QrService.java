package com.cibertec.conecta.service;

import com.cibertec.conecta.exception.ReglaNegocioException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QrService {
    private final ObjectMapper objectMapper;

    public String extraerToken(String contenido) {
        String token = contenido.strip();
        if (token.startsWith("{")) {
            try {
                var json = objectMapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(token);
                var version = json.path("v");
                if (!version.isIntegralNumber() || !version.canConvertToInt() || version.intValue() != 1
                        || !json.path("token").isTextual()) {
                    throw qrInvalido();
                }
                token = json.path("token").textValue();
            } catch (JsonProcessingException ex) {
                throw qrInvalido();
            }
        }
        if (!token.matches("[A-Za-z0-9-]{8,64}")) {
            throw qrInvalido();
        }
        return token;
    }

    private ReglaNegocioException qrInvalido() {
        return new ReglaNegocioException("El código QR no tiene un formato válido", "QR_INVALIDO");
    }

    public byte[] generarPng(String codigo, String token) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (var salida = new MemoryCacheImageOutputStream(bytes)) {
            String contenido = objectMapper.writeValueAsString(Map.of("v", 1, "codigo", codigo, "token", token));
            var matriz = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 480, 480,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
            ImageIO.write(MatrixToImageWriter.toBufferedImage(matriz), "PNG", salida);
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("No se pudo generar la imagen QR", ex);
        }
        return bytes.toByteArray();
    }
}
