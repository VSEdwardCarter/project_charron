CREATE OR REPLACE FUNCTION prevent_movement_manifest_mutation()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'Movement manifests are immutable';
END;
$$;

CREATE TRIGGER trg_prevent_movement_manifest_update
    BEFORE UPDATE ON movement_manifests
    FOR EACH ROW
EXECUTE FUNCTION prevent_movement_manifest_mutation();

CREATE TRIGGER trg_prevent_movement_manifest_delete
    BEFORE DELETE ON movement_manifests
    FOR EACH ROW
EXECUTE FUNCTION prevent_movement_manifest_mutation();