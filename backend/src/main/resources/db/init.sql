CREATE UNIQUE INDEX IF NOT EXISTS uk_tipo_etapa_codigo_sistema
    ON tipos_de_etapas (codigo)
    WHERE company_profile_id IS NULL
    AND codigo IS NOT NULL;
INSERT INTO tipos_de_etapas (
    id,
    codigo,
    nome,
    descricao,
    company_profile_id,
    ja_utilizada,
    disponibilidade,
    created_at
)
VALUES
    (gen_random_uuid(), 'INSCRICAO', 'Inscrição', 'Candidatura recebida.', NULL, false, true, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'TRIAGEM', 'Triagem', 'Análise inicial dos candidatos.', NULL, false, true, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'ENTREVISTA_RH', 'Entrevista com RH', 'Entrevista para avaliar perfil e alinhamento.', NULL, false, true, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'TESTE_TECNICO', 'Teste técnico', 'Avaliação de conhecimentos técnicos.', NULL, false, true, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'ENTREVISTA_TECNICA', 'Entrevista técnica', 'Entrevista para aprofundar conhecimentos técnicos.', NULL, false, true, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'PROPOSTA', 'Proposta', 'Apresentação da proposta de contratação.', NULL, false, true, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'ENCERRADA', 'Encerrada', 'Processo seletivo finalizado.', NULL, false, true, CURRENT_TIMESTAMP)
    ON CONFLICT DO NOTHING;