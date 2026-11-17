
CREATE TABLE equipment_specifications (
                                          equipment_id UUID PRIMARY KEY
                                              REFERENCES equipment(id),

    -- Exterior dimensions, stored in millimeters
                                          length_mm BIGINT,
                                          width_mm BIGINT,
                                          height_mm BIGINT,

    -- Preserve whether weight specifications exist
                                          weight_specifications_present BOOLEAN NOT NULL,

    -- Weight measurements, stored in grams
                                          empty_weight_grams BIGINT,
                                          gross_weight_rating_grams BIGINT,
                                          rated_payload_capacity_grams BIGINT,

    -- Dimensions must be complete or entirely absent
                                          CONSTRAINT chk_equipment_dimensions
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
                                                  ),

    -- At least one specification group must exist
                                          CONSTRAINT chk_equipment_specifications_present
                                              CHECK (
                                                  length_mm IS NOT NULL
                                                      OR weight_specifications_present
                                                  ),

    -- No weight values without weight specifications
                                          CONSTRAINT chk_equipment_weight_presence
                                              CHECK (
                                                  weight_specifications_present
                                                      OR (
                                                      empty_weight_grams IS NULL
                                                          AND gross_weight_rating_grams IS NULL
                                                          AND rated_payload_capacity_grams IS NULL
                                                      )
                                                  ),

    -- Weight validation
                                          CONSTRAINT chk_equipment_weights_nonnegative
                                              CHECK (
                                                  (empty_weight_grams IS NULL
                                                      OR empty_weight_grams >= 0)
                                                      AND
                                                  (gross_weight_rating_grams IS NULL
                                                      OR gross_weight_rating_grams > 0)
                                                      AND
                                                  (rated_payload_capacity_grams IS NULL
                                                      OR rated_payload_capacity_grams >= 0)
                                                  ),

    -- Empty weight cannot exceed rated gross weight
                                          CONSTRAINT chk_equipment_empty_vs_gross
                                              CHECK (
                                                  empty_weight_grams IS NULL
                                                      OR gross_weight_rating_grams IS NULL
                                                      OR empty_weight_grams <= gross_weight_rating_grams
                                                  ),

    -- Payload must fit within the gross weight allowance
                                          CONSTRAINT chk_equipment_payload_capacity
                                              CHECK (
                                                  rated_payload_capacity_grams IS NULL
                                                      OR empty_weight_grams IS NULL
                                                      OR gross_weight_rating_grams IS NULL
                                                      OR rated_payload_capacity_grams
                                                      <= gross_weight_rating_grams
                                                             - empty_weight_grams
                                                  )
);
