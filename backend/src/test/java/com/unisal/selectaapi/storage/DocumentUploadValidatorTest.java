package com.unisal.selectaapi.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.unisal.selectaapi.exception.BusinessException;

class DocumentUploadValidatorTest {

    private final DocumentUploadValidator validator = new DocumentUploadValidator();

    @Test
    void aceitaPdfPngEJpegPelaAssinaturaDoArquivo() {
        var pdf = validator.validate(file("a.pdf", new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D}));
        var png = validator.validate(file("a.png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0}));
        var jpg = validator.validate(file("a.jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}));

        assertThat(pdf.contentType()).isEqualTo("application/pdf");
        assertThat(png.extension()).isEqualTo("png");
        assertThat(jpg.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void ignoraOContentTypeInformadoPeloCliente() {
        var executavel = new MockMultipartFile("file", "virus.pdf", "application/pdf", new byte[] {'M', 'Z', 0, 0});

        assertThatThrownBy(() -> validator.validate(executavel)).isInstanceOf(BusinessException.class);
    }

    @Test
    void rejeitaArquivoVazioEMaiorQueOLimite() {
        assertThatThrownBy(() -> validator.validate(file("a.pdf", new byte[0])))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> validator.validate(file("a.pdf", new byte[5 * 1024 * 1024 + 1])))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void limpaCaminhoEAspasDoNomeOriginal() {
        var doc = validator.validate(file("C:\\fakepath\\..\\meu \"certificado\".pdf",
                new byte[] {0x25, 0x50, 0x44, 0x46}));

        assertThat(doc.originalName()).isEqualTo("meu certificado.pdf");
    }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/octet-stream", content);
    }
}
