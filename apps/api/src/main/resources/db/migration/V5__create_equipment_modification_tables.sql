
CREATE TABLE equipment_modifications (
                                         id UUID PRIMARY KEY,

                                         equipment_id UUID NOT NULL
                                             REFERENCES equipment(id),

                                         configuration_id UUID
                                             REFERENCES equipment_configurations(id),

                                         type VARCHAR(50) NOT NULL,

                                         description TEXT NOT NULL,

                                         effective_at TIMESTAMPTZ NOT NULL,

                                         recorded_at TIMESTAMPTZ NOT NULL,

                                         removed_at TIMESTAMPTZ,

                                         CONSTRAINT chk_equipment_modification_description
                                             CHECK (length(btrim(description)) > 0),

                                         CONSTRAINT chk_equipment_modification_dates
                                             CHECK (
                                                 removed_at IS NULL
                                                     OR removed_at >= effective_at
                                                 )
);

CREATE INDEX idx_equipment_modifications_equipment
    ON equipment_modifications(equipment_id);

CREATE INDEX idx_equipment_modifications_configuration
    ON equipment_modifications(configuration_id);

CREATE TABLE equipment_modification_events (
                                               id UUID PRIMARY KEY,

                                               modification_id UUID NOT NULL
                                                   REFERENCES equipment_modifications(id),

                                               type VARCHAR(50) NOT NULL,

                                               effective_at TIMESTAMPTZ NOT NULL,

                                               recorded_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_equipment_modification_events_modification
    ON equipment_modification_events(
                                     modification_id,
                                     recorded_at
        );
