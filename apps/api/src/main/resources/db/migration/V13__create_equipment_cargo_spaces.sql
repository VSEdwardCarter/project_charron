
CREATE TABLE equipment_cargo_spaces (
                                        id UUID PRIMARY KEY,

                                        equipment_id UUID NOT NULL
                                            REFERENCES equipment(id),

                                        name VARCHAR(150) NOT NULL,

                                        space_type VARCHAR(30) NOT NULL,

                                        usable_length_mm BIGINT NOT NULL,
                                        usable_width_mm BIGINT NOT NULL,
                                        usable_height_mm BIGINT,

                                        CONSTRAINT chk_cargo_space_name
                                            CHECK (length(trim(name)) > 0),

                                        CONSTRAINT chk_cargo_space_type
                                            CHECK (space_type IN ('ENCLOSED', 'OPEN_DECK')),

                                        CONSTRAINT chk_cargo_space_length
                                            CHECK (usable_length_mm > 0),

                                        CONSTRAINT chk_cargo_space_width
                                            CHECK (usable_width_mm > 0),

                                        CONSTRAINT chk_cargo_space_height
                                            CHECK (
                                                usable_height_mm IS NULL
                                                    OR usable_height_mm > 0
                                                ),

                                        CONSTRAINT chk_enclosed_cargo_space_height
                                            CHECK (
                                                space_type <> 'ENCLOSED'
                                                    OR usable_height_mm IS NOT NULL
                                                )
);

CREATE INDEX idx_equipment_cargo_spaces_equipment
    ON equipment_cargo_spaces(equipment_id);
