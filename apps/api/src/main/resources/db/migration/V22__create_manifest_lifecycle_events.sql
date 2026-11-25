CREATE TABLE manifest_lifecycle_events (
                                           id UUID PRIMARY KEY,

                                           manifest_id UUID NOT NULL
                                               REFERENCES movement_manifests(id),

                                           previous_status VARCHAR(30) NOT NULL,

                                           new_status VARCHAR(30) NOT NULL,

                                           actor_id UUID NOT NULL,

                                           recorded_at TIMESTAMPTZ NOT NULL,

                                           remarks TEXT,

                                           CONSTRAINT chk_manifest_lifecycle_transition
                                               CHECK (
                                                   (previous_status = 'DRAFT'
                                                       AND new_status = 'REVIEWED')
                                                       OR
                                                   (previous_status = 'REVIEWED'
                                                       AND new_status = 'ISSUED')
                                                       OR
                                                   (previous_status = 'ISSUED'
                                                       AND new_status = 'SUPERSEDED')
                                                   ),

                                           CONSTRAINT uq_manifest_lifecycle_transition
                                               UNIQUE (manifest_id, new_status),

                                           CONSTRAINT chk_manifest_lifecycle_remarks
                                               CHECK (
                                                   remarks IS NULL
                                                       OR length(btrim(remarks)) > 0
                                                   )
);

CREATE INDEX idx_manifest_lifecycle_chronology
    ON manifest_lifecycle_events (
                                  manifest_id,
                                  recorded_at
        );


CREATE OR REPLACE FUNCTION prevent_manifest_lifecycle_event_mutation()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'Manifest lifecycle events are immutable';
END;
$$;


CREATE TRIGGER trg_prevent_manifest_lifecycle_event_update
    BEFORE UPDATE ON manifest_lifecycle_events
    FOR EACH ROW
EXECUTE FUNCTION prevent_manifest_lifecycle_event_mutation();


CREATE TRIGGER trg_prevent_manifest_lifecycle_event_delete
    BEFORE DELETE ON manifest_lifecycle_events
    FOR EACH ROW
EXECUTE FUNCTION prevent_manifest_lifecycle_event_mutation();