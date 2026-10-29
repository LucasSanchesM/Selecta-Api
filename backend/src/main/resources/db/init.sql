CREATE UNIQUE INDEX uk_tipo_etapa_codigo_sistema
    ON tipos_de_etapas (codigo)
    WHERE company_profile_id IS NULL
  AND codigo IS NOT NULL;