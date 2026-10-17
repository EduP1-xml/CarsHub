-- =====================================================================
-- CarHub - localizacao espacial das solicitacoes de servico
--
-- Executar UMA VEZ, depois que a aplicacao criar as tabelas.
--
-- Pre-requisitos:
--   1. application.yaml com:
--      spring.jpa.hibernate.naming.physical-strategy:
--        org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
--   2. banco recriado e aplicacao subida ao menos uma vez (as colunas de
--      coordenada do entusiasta ja existem como NOT NULL, via @Column(nullable = false))
--
-- O que o Hibernate NAO consegue criar (e por isso esta aqui):
--   CHECK de faixa  +  coluna POINT gerada  +  SPATIAL INDEX
-- =====================================================================

-- ---------------------------------------------------------------- MECANICO

ALTER TABLE ServicoMecanico
  ADD CONSTRAINT chk_servicoMecanico_faixa
      CHECK (latitudeAtualEntusiasta_ServicoMecanico  BETWEEN  -90 AND  90
         AND longitudeAtualEntusiasta_ServicoMecanico BETWEEN -180 AND 180);

ALTER TABLE ServicoMecanico
  ADD CONSTRAINT chk_servicoMecanico_prestador_coordenadas
      CHECK ((latitudeAtualMecanico_ServicoMecanico IS NULL AND longitudeAtualMecanico_ServicoMecanico IS NULL)
          OR (latitudeAtualMecanico_ServicoMecanico IS NOT NULL AND longitudeAtualMecanico_ServicoMecanico IS NOT NULL));

-- Coluna POINT gerada do par latitude/longitude (SRID 0 = plano).
-- O app NUNCA escreve nela: o MySQL recalcula a cada insert/update.
ALTER TABLE ServicoMecanico
  ADD COLUMN localizacao_ServicoMecanico POINT
      GENERATED ALWAYS AS (POINT(latitudeAtualEntusiasta_ServicoMecanico,
                                 longitudeAtualEntusiasta_ServicoMecanico)) STORED NOT NULL;

ALTER TABLE ServicoMecanico
  ADD SPATIAL INDEX idx_servicoMecanico_localizacao (localizacao_ServicoMecanico);

-- ----------------------------------------------------------------- GUINCHO

ALTER TABLE ServicoGuincho
  ADD CONSTRAINT chk_servicoGuincho_faixa
      CHECK (latitudeAtualEntusiasta_ServicoGuincho  BETWEEN  -90 AND  90
         AND longitudeAtualEntusiasta_ServicoGuincho BETWEEN -180 AND 180);

ALTER TABLE ServicoGuincho
  ADD CONSTRAINT chk_servicoGuincho_prestador_coordenadas
      CHECK ((latitudeAtualGuincho_ServicoGuincho IS NULL AND longitudeAtualGuincho_ServicoGuincho IS NULL)
          OR (latitudeAtualGuincho_ServicoGuincho IS NOT NULL AND longitudeAtualGuincho_ServicoGuincho IS NOT NULL));

ALTER TABLE ServicoGuincho
  ADD COLUMN localizacao_ServicoGuincho POINT
      GENERATED ALWAYS AS (POINT(latitudeAtualEntusiasta_ServicoGuincho,
                                 longitudeAtualEntusiasta_ServicoGuincho)) STORED NOT NULL;

ALTER TABLE ServicoGuincho
  ADD SPATIAL INDEX idx_servicoGuincho_localizacao (localizacao_ServicoGuincho);

-- =====================================================================
-- SE O MYSQL RECUSAR "STORED NOT NULL"
-- (a gramatica aceita; algumas versoes validam diferente), rode o ADD
-- COLUMN sem o NOT NULL e depois:
--
--   ALTER TABLE ServicoMecanico
--     MODIFY localizacao_ServicoMecanico POINT NOT NULL;
--   ALTER TABLE ServicoGuincho
--     MODIFY localizacao_ServicoGuincho POINT NOT NULL;
-- =====================================================================