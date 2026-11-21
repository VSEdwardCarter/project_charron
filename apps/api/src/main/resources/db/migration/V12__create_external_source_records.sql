
CREATE TABLE external_source_records (
                                         id UUID PRIMARY KEY,

                                         source_system VARCHAR(100) NOT NULL,

                                         entity_type VARCHAR(100) NOT NULL,

                                         external_id VARCHAR(255) NOT NULL,

                                         payload JSONB NOT NULL,

                                         first_ingested_at TIMESTAMPTZ NOT NULL
                                             DEFAULT CURRENT_TIMESTAMP,

                                         last_ingested_at TIMESTAMPTZ NOT NULL
                                             DEFAULT CURRENT_TIMESTAMP,

                                         CONSTRAINT uq_external_source_record
                                             UNIQUE (
                                                     source_system,
                                                     entity_type,
                                                     external_id
                                                 ),

                                         CONSTRAINT chk_external_source_system
                                             CHECK (length(trim(source_system)) > 0),

                                         CONSTRAINT chk_external_entity_type
                                             CHECK (length(trim(entity_type)) > 0),

                                         CONSTRAINT chk_external_record_id
                                             CHECK (length(trim(external_id)) > 0),

                                         CONSTRAINT chk_external_payload_object
                                             CHECK (jsonb_typeof(payload) = 'object')
);

CREATE INDEX idx_external_source_records_type
    ON external_source_records (
                                source_system,
                                entity_type
        );
