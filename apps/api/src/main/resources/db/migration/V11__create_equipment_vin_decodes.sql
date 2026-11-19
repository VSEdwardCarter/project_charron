
CREATE TABLE equipment_vin_decodes (
                                       id UUID PRIMARY KEY,

                                       equipment_id UUID NOT NULL
                                           REFERENCES equipment(id),

                                       vin VARCHAR(17) NOT NULL,

                                       decoder_source VARCHAR(100) NOT NULL,

                                       decoder_version VARCHAR(100),

                                       decoded_at TIMESTAMPTZ NOT NULL
                                           DEFAULT CURRENT_TIMESTAMP,

                                       decoded_vehicle JSONB NOT NULL,

                                       raw_response JSONB NOT NULL,

                                       CONSTRAINT chk_vin_decode_length
                                           CHECK (length(vin) = 17),

                                       CONSTRAINT chk_vin_decode_raw_array
                                           CHECK (jsonb_typeof(raw_response) = 'array'),

                                       CONSTRAINT chk_vin_decode_vehicle_object
                                           CHECK (jsonb_typeof(decoded_vehicle) = 'object')
);

CREATE INDEX idx_equipment_vin_decodes_equipment
    ON equipment_vin_decodes (equipment_id, decoded_at DESC);
