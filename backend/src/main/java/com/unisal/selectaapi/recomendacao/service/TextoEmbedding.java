package com.unisal.selectaapi.recomendacao.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.unisal.selectaapi.candidate.entity.CandidateProfile;
import com.unisal.selectaapi.vagas.entity.Vaga;

/**
 * Monta o texto que representa uma vaga ou um candidato antes de virar vetor.
 * Benefícios, salário e dados de contato ficam de fora: não descrevem o trabalho
 * e só adicionariam ruído à comparação.
 */
public final class TextoEmbedding {

    static final int LIMITE_CARACTERES = 12_000;

    private TextoEmbedding() {
    }

    public static String daVaga(Vaga vaga) {
        List<String> partes = new ArrayList<>();
        adicionar(partes, "Cargo", vaga.getTitulo());
        adicionar(partes, "Área", vaga.getCategoria() == null ? null : formatarEnum(vaga.getCategoria().name()));
        adicionar(partes, "Modalidade", vaga.getModalidade() == null ? null : formatarEnum(vaga.getModalidade().name()));
        adicionar(partes, "Palavras-chave", juntar(vaga.getMetaTags()));
        adicionar(partes, "Descrição", vaga.getDescricao());
        return limitar(String.join("\n", partes));
    }

    public static String doCandidato(CandidateProfile perfil) {
        List<String> partes = new ArrayList<>();
        adicionar(partes, "Objetivo", perfil.getHeadline());
        adicionar(partes, "Resumo", perfil.getSummary());
        adicionar(partes, "Curso", perfil.getCourse());
        adicionar(partes, "Habilidades", juntar(perfil.getSkills()));
        perfil.getExperiences().forEach(experiencia ->
                adicionar(partes, "Experiência", juntarComTraco(experiencia.getRole(), experiencia.getDescription())));
        perfil.getProjects().forEach(projeto ->
                adicionar(partes, "Projeto", juntarComTraco(projeto.getTitle(), projeto.getDescription())));
        adicionar(partes, "Currículo", perfil.getResumeText());
        return limitar(String.join("\n", partes));
    }

    private static void adicionar(List<String> partes, String rotulo, String valor) {
        if (valor != null && !valor.isBlank()) {
            partes.add(rotulo + ": " + valor.strip());
        }
    }

    private static String juntar(List<String> valores) {
        if (valores == null) {
            return null;
        }
        return String.join(", ", valores.stream().filter(Objects::nonNull).toList());
    }

    private static String juntarComTraco(String titulo, String descricao) {
        if (titulo == null || titulo.isBlank()) {
            return descricao;
        }
        if (descricao == null || descricao.isBlank()) {
            return titulo;
        }
        return titulo.strip() + " - " + descricao.strip();
    }

    private static String formatarEnum(String nome) {
        return nome.replace('_', ' ').toLowerCase();
    }

    private static String limitar(String texto) {
        return texto.length() > LIMITE_CARACTERES ? texto.substring(0, LIMITE_CARACTERES) : texto;
    }
}
