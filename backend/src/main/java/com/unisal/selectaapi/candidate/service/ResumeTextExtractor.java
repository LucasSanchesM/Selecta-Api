package com.unisal.selectaapi.candidate.service;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResumeTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(ResumeTextExtractor.class);

    static final int LIMITE_CARACTERES = 20_000;

    private ResumeTextExtractor() {
    }

    /**
     * Retorna o texto do PDF ou {@code null} quando não há texto legível
     * (PDF escaneado, protegido ou corrompido). Nunca lança exceção para não
     * impedir o upload do currículo.
     */
    public static String extrair(byte[] pdf) {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(documento)
                    .replaceAll("\\s+", " ")
                    .strip();
            if (texto.isEmpty()) {
                return null;
            }
            return texto.length() > LIMITE_CARACTERES ? texto.substring(0, LIMITE_CARACTERES) : texto;
        } catch (IOException | RuntimeException exception) {
            log.warn("Não foi possível extrair o texto do currículo: {}", exception.getMessage());
            return null;
        }
    }
}
