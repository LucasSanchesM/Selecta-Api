package com.unisal.selectaapi.storage;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.unisal.selectaapi.exception.BusinessException;

/** Valida anexos de certificados: PDF, PNG ou JPEG de até 5 MB, conferidos pela assinatura do arquivo. */
@Component
public class DocumentUploadValidator {

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final int MAX_FILE_NAME = 255;

    public record ValidatedDocument(byte[] content, String contentType, String extension, String originalName) {}

    public ValidatedDocument validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Selecione um arquivo");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(HttpStatus.CONTENT_TOO_LARGE, "O arquivo deve ter no máximo 5 MB");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo enviado");
        }

        String contentType;
        String extension;
        if (isPdf(content)) {
            contentType = "application/pdf";
            extension = "pdf";
        } else if (isPng(content)) {
            contentType = "image/png";
            extension = "png";
        } else if (isJpeg(content)) {
            contentType = "image/jpeg";
            extension = "jpg";
        } else {
            throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Formato inválido. Envie um arquivo PDF, PNG ou JPEG");
        }
        return new ValidatedDocument(content, contentType, extension,
                sanitizeName(file.getOriginalFilename(), extension));
    }

    /** Remove caminho e caracteres de controle/aspas do nome original, que é exibido e usado em cabeçalhos. */
    private static String sanitizeName(String original, String extension) {
        String name = original == null ? "" : original;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[\\p{Cntrl}\"<>:|?*]", "").trim();
        if (name.isEmpty()) {
            name = "certificado." + extension;
        }
        return name.length() > MAX_FILE_NAME ? name.substring(name.length() - MAX_FILE_NAME) : name;
    }

    private static boolean isPdf(byte[] c) {
        return c.length >= 4 && c[0] == 0x25 && c[1] == 0x50 && c[2] == 0x44 && c[3] == 0x46;
    }

    private static boolean isPng(byte[] c) {
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (c.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (c[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isJpeg(byte[] c) {
        return c.length >= 3 && c[0] == (byte) 0xFF && c[1] == (byte) 0xD8 && c[2] == (byte) 0xFF;
    }
}
