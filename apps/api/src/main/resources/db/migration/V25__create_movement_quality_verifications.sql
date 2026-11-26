CREATE TABLE movement_quality_verifications (

                                                id UUID PRIMARY KEY,

                                                manifest_id UUID NOT NULL
                                                    REFERENCES movement_manifests(id),

                                                reviewer_id UUID NOT NULL,

                                                equipment_assignment_verified BOOLEAN NOT NULL,

                                                equipment_inspection_completed BOOLEAN NOT NULL,

                                                cargo_reconciliation_verified BOOLEAN NOT NULL,

                                                loading_arrangement_verified BOOLEAN NOT NULL,

                                                cargo_securement_verified BOOLEAN NOT NULL,

                                                documentation_verified BOOLEAN NOT NULL,

                                                verified_at TIMESTAMPTZ NOT NULL,

                                                remarks TEXT,

                                                CONSTRAINT chk_quality_verification_complete
                                                    CHECK (
                                                        equipment_assignment_verified
                                                            AND equipment_inspection_completed
                                                            AND cargo_reconciliation_verified
                                                            AND loading_arrangement_verified
                                                            AND cargo_securement_verified
                                                            AND documentation_verified
                                                        ),

                                                CONSTRAINT chk_quality_verification_remarks
                                                    CHECK (
                                                        remarks IS NULL
                                                            OR length(btrim(remarks)) > 0
                                                        )
);


CREATE INDEX idx_quality_verification_manifest
    ON movement_quality_verifications (
                                       manifest_id,
                                       verified_at DESC
        );


CREATE INDEX idx_quality_verification_reviewer
    ON movement_quality_verifications (
                                       reviewer_id,
                                       verified_at DESC
        );


CREATE OR REPLACE FUNCTION prevent_quality_verification_mutation()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'Completed quality verification records are immutable';
END;
$$;


CREATE TRIGGER trg_prevent_quality_verification_update
    BEFORE UPDATE ON movement_quality_verifications
    FOR EACH ROW
EXECUTE FUNCTION prevent_quality_verification_mutation();


CREATE TRIGGER trg_prevent_quality_verification_delete
    BEFORE DELETE ON movement_quality_verifications
    FOR EACH ROW
EXECUTE FUNCTION prevent_quality_verification_mutation();