-- Un análisis por fila. Los datos de la página se guardan ya extraídos y
-- acotados (jsonb); nunca el HTML crudo.
CREATE TABLE analysis (
    id             uuid          PRIMARY KEY,
    requested_url  varchar(2048) NOT NULL,
    final_url      varchar(2048),
    status         varchar(30)   NOT NULL,
    snapshot       jsonb,
    issues         jsonb,
    ai_result      jsonb,
    ai_status      varchar(20),
    error_code     varchar(50),
    created_at     timestamptz   NOT NULL,
    completed_at   timestamptz,
    expires_at     timestamptz   NOT NULL,
    version        bigint        NOT NULL DEFAULT 0,
    CONSTRAINT analysis_status_check CHECK (status IN (
        'PENDING', 'FETCHING', 'ANALYZING', 'INTERPRETING',
        'COMPLETED', 'COMPLETED_WITHOUT_AI', 'FAILED')),
    CONSTRAINT analysis_ai_status_check CHECK (ai_status IS NULL OR ai_status IN (
        'AVAILABLE', 'UNAVAILABLE', 'INVALID_OUTPUT'))
);

CREATE INDEX analysis_expires_at_idx ON analysis (expires_at);
CREATE INDEX analysis_status_idx ON analysis (status);
