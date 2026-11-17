
CREATE TABLE equipment_configurations (
                                          id UUID PRIMARY KEY,

                                          equipment_id UUID NOT NULL
                                              REFERENCES equipment(id),

                                          name VARCHAR(255) NOT NULL,

                                          active BOOLEAN NOT NULL,

                                          weight_adjustment_grams BIGINT,

                                          CONSTRAINT chk_equipment_configuration_name
                                              CHECK (length(trim(name)) > 0)
);

CREATE INDEX idx_equipment_configurations_equipment_id
    ON equipment_configurations(equipment_id);


CREATE TABLE equipment_configuration_capabilities (
                                                      configuration_id UUID NOT NULL
                                                          REFERENCES equipment_configurations(id),

                                                      capability VARCHAR(100) NOT NULL,

                                                      CONSTRAINT equipment_configuration_capabilities_pkey
                                                          PRIMARY KEY (configuration_id, capability),

                                                      CONSTRAINT chk_configuration_capability_not_blank
                                                          CHECK (length(trim(capability)) > 0)
);
