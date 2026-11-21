
CREATE TABLE cargo (
                       id UUID PRIMARY KEY,

                       name VARCHAR(255) NOT NULL,
                       reference VARCHAR(255),
                       description TEXT,

                       weight_grams BIGINT,

                       length_mm BIGINT,
                       width_mm BIGINT,
                       height_mm BIGINT,

                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                       CONSTRAINT cargo_name_not_blank
                           CHECK (length(btrim(name)) > 0),

                       CONSTRAINT cargo_weight_positive
                           CHECK (
                               weight_grams IS NULL
                                   OR weight_grams > 0
                               ),

                       CONSTRAINT cargo_dimensions_complete
                           CHECK (
                               (
                                   length_mm IS NULL
                                       AND width_mm IS NULL
                                       AND height_mm IS NULL
                                   )
                                   OR
                               (
                                   length_mm > 0
                                       AND width_mm > 0
                                       AND height_mm > 0
                                   )
                               )
);

CREATE INDEX idx_cargo_name
    ON cargo (name);

CREATE INDEX idx_cargo_reference
    ON cargo (reference);
