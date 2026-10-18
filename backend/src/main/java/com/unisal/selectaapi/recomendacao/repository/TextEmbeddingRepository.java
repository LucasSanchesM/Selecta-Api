package com.unisal.selectaapi.recomendacao.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unisal.selectaapi.recomendacao.entity.TextEmbedding;

public interface TextEmbeddingRepository extends JpaRepository<TextEmbedding, UUID> {
}
