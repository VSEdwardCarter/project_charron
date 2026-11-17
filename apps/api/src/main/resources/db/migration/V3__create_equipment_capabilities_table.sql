
CREATE TABLE equipment_capabilities (
                                        equipment_id UUID NOT NULL
                                            REFERENCES equipment(id),

                                        capability VARCHAR(100) NOT NULL,

                                        CONSTRAINT equipment_capabilities_pkey
                                            PRIMARY KEY (equipment_id, capability),

                                        CONSTRAINT chk_equipment_capability_not_blank
                                            CHECK (length(trim(capability)) > 0)
);
