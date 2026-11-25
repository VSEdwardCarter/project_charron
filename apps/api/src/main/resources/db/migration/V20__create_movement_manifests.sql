CREATE TABLE movement_manifests (
                                    id UUID PRIMARY KEY,

                                    movement_id UUID NOT NULL
                                        REFERENCES movements(id),

                                    version INTEGER NOT NULL,

                                    snapshot JSONB NOT NULL,

                                    generated_at TIMESTAMPTZ NOT NULL,

                                    CONSTRAINT chk_movement_manifest_version
                                        CHECK (version > 0),

                                    CONSTRAINT chk_movement_manifest_snapshot
                                        CHECK (jsonb_typeof(snapshot) = 'object'),

                                    CONSTRAINT uq_movement_manifest_version
                                        UNIQUE (movement_id, version)
);