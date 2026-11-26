
-- ============================================================
-- V26: Operational Revision Tracking
-- Project CHARRON
--
-- Purpose:
-- Track operational changes affecting movement quality
-- verification eligibility while preserving immutable
-- historical verification records.
-- ============================================================


-- ============================================================
-- 1. Operational revision columns
-- ============================================================

ALTER TABLE movements
    ADD COLUMN operational_revision BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_movement_operational_revision
        CHECK (operational_revision >= 0);

ALTER TABLE movement_quality_verifications
    ADD COLUMN operational_revision BIGINT,
    ADD CONSTRAINT chk_verification_operational_revision
        CHECK (operational_revision >= 0);

CREATE INDEX idx_quality_verification_revision
    ON movement_quality_verifications (
                                       manifest_id,
                                       operational_revision
        );


-- ============================================================
-- 2. Capture operational revision during QA/QC completion
-- ============================================================

CREATE OR REPLACE FUNCTION capture_quality_verification_revision()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    current_revision BIGINT;
BEGIN

    SELECT m.operational_revision
    INTO current_revision
    FROM movements m
             JOIN movement_manifests mm
                  ON mm.movement_id = m.id
    WHERE mm.id = NEW.manifest_id
        FOR UPDATE OF m;

    IF NOT FOUND THEN

        RAISE EXCEPTION
            'Referenced manifest does not exist: %',
            NEW.manifest_id
            USING ERRCODE = '23503';

    END IF;

    NEW.operational_revision := current_revision;

    RETURN NEW;

END;
$$;
CREATE TRIGGER trg_capture_quality_verification_revision
    BEFORE INSERT ON movement_quality_verifications
    FOR EACH ROW
EXECUTE FUNCTION capture_quality_verification_revision();


-- ============================================================
-- 3. Track movement-level operational changes
-- ============================================================

CREATE OR REPLACE FUNCTION track_movement_operational_change()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN

    IF ROW(
        OLD.reference,
        OLD.origin,
        OLD.destination,
        OLD.planned_departure,
        OLD.planned_arrival
        ) IS DISTINCT FROM ROW(
        NEW.reference,
        NEW.origin,
        NEW.destination,
        NEW.planned_departure,
        NEW.planned_arrival
        ) THEN

        NEW.operational_revision :=
                OLD.operational_revision + 1;

    END IF;

    RETURN NEW;

END;
$$;

CREATE TRIGGER trg_track_movement_operational_change
    BEFORE UPDATE ON movements
    FOR EACH ROW
EXECUTE FUNCTION track_movement_operational_change();


-- ============================================================
-- 4. Track direct movement dependencies
--
-- Tables:
--   movement_equipment_assignments
--   loading_plans
-- ============================================================

CREATE OR REPLACE FUNCTION track_direct_movement_change()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    affected_movement_id UUID;
BEGIN

    IF TG_OP = 'UPDATE'
        AND OLD IS NOT DISTINCT FROM NEW THEN

        RETURN NEW;

    END IF;

    IF TG_OP = 'DELETE' THEN

        affected_movement_id := OLD.movement_id;

    ELSE

        affected_movement_id := NEW.movement_id;

    END IF;

    UPDATE movements
    SET operational_revision = operational_revision + 1
    WHERE id = affected_movement_id;

    IF TG_OP = 'UPDATE' THEN

        IF OLD.movement_id IS DISTINCT FROM NEW.movement_id THEN

            UPDATE movements
            SET operational_revision = operational_revision + 1
            WHERE id = OLD.movement_id;

        END IF;

    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;

    RETURN NEW;

END;
$$;

CREATE TRIGGER trg_track_equipment_assignment_change
    AFTER INSERT OR UPDATE OR DELETE
    ON movement_equipment_assignments
    FOR EACH ROW
EXECUTE FUNCTION track_direct_movement_change();

CREATE TRIGGER trg_track_loading_plan_change
    AFTER INSERT OR UPDATE OR DELETE
    ON loading_plans
    FOR EACH ROW
EXECUTE FUNCTION track_direct_movement_change();


-- ============================================================
-- 5. Track loading plan child dependencies
--
-- Tables:
--   loading_plan_cargo
--   loading_plan_arrangements
-- ============================================================

CREATE OR REPLACE FUNCTION track_loading_plan_child_change()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    affected_plan_id UUID;
BEGIN

    IF TG_OP = 'UPDATE'
        AND OLD IS NOT DISTINCT FROM NEW THEN

        RETURN NEW;

    END IF;

    IF TG_OP = 'DELETE' THEN

        affected_plan_id := OLD.loading_plan_id;

    ELSE

        affected_plan_id := NEW.loading_plan_id;

    END IF;

    UPDATE movements
    SET operational_revision = operational_revision + 1
    WHERE id IN (
        SELECT movement_id
        FROM loading_plans
        WHERE id = affected_plan_id
    );

    IF TG_OP = 'UPDATE' THEN

        IF OLD.loading_plan_id
            IS DISTINCT FROM NEW.loading_plan_id THEN

            UPDATE movements
            SET operational_revision = operational_revision + 1
            WHERE id IN (
                SELECT movement_id
                FROM loading_plans
                WHERE id = OLD.loading_plan_id
            );

        END IF;

    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;

    RETURN NEW;

END;
$$;

CREATE TRIGGER trg_track_loading_plan_cargo_change
    AFTER INSERT OR UPDATE OR DELETE
    ON loading_plan_cargo
    FOR EACH ROW
EXECUTE FUNCTION track_loading_plan_child_change();

CREATE TRIGGER trg_track_loading_arrangement_change
    AFTER INSERT OR UPDATE OR DELETE
    ON loading_plan_arrangements
    FOR EACH ROW
EXECUTE FUNCTION track_loading_plan_child_change();


-- ============================================================
-- 6. Shared equipment dependency resolution
--
-- Increment revisions for movements associated with the
-- affected equipment through assignments or loading plans.
-- ============================================================

CREATE OR REPLACE FUNCTION increment_equipment_movement_revisions(
    affected_equipment_id UUID
)
    RETURNS VOID
    LANGUAGE plpgsql
AS $$
BEGIN

    UPDATE movements
    SET operational_revision = operational_revision + 1
    WHERE id IN (

        SELECT movement_id
        FROM movement_equipment_assignments
        WHERE equipment_id = affected_equipment_id

        UNION

        SELECT movement_id
        FROM loading_plans
        WHERE equipment_id = affected_equipment_id

    );

END;
$$;


-- ============================================================
-- 7. Track direct equipment catalog dependencies
--
-- Tables:
--   equipment
--   equipment_specifications
--   equipment_capabilities
--   equipment_configurations
--   equipment_modifications
--   equipment_cargo_spaces
-- ============================================================

CREATE OR REPLACE FUNCTION track_equipment_catalog_change()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    old_equipment_id UUID;
    new_equipment_id UUID;
BEGIN

    IF TG_OP = 'UPDATE'
        AND OLD IS NOT DISTINCT FROM NEW THEN

        RETURN NEW;

    END IF;

    IF TG_OP <> 'INSERT' THEN

        IF TG_TABLE_NAME = 'equipment' THEN

            old_equipment_id := OLD.id;

        ELSE

            old_equipment_id := OLD.equipment_id;

        END IF;

    END IF;

    IF TG_OP <> 'DELETE' THEN

        IF TG_TABLE_NAME = 'equipment' THEN

            new_equipment_id := NEW.id;

        ELSE

            new_equipment_id := NEW.equipment_id;

        END IF;

    END IF;

    IF old_equipment_id IS NOT NULL THEN

        PERFORM increment_equipment_movement_revisions(
                old_equipment_id
                );

    END IF;

    IF new_equipment_id IS NOT NULL
        AND new_equipment_id IS DISTINCT FROM old_equipment_id THEN

        PERFORM increment_equipment_movement_revisions(
                new_equipment_id
                );

    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;

    RETURN NEW;

END;
$$;

CREATE TRIGGER trg_track_equipment_catalog
    AFTER INSERT OR UPDATE OR DELETE
    ON equipment
    FOR EACH ROW
EXECUTE FUNCTION track_equipment_catalog_change();

CREATE TRIGGER trg_track_equipment_specifications
    AFTER INSERT OR UPDATE OR DELETE
    ON equipment_specifications
    FOR EACH ROW
EXECUTE FUNCTION track_equipment_catalog_change();

CREATE TRIGGER trg_track_equipment_capabilities
    AFTER INSERT OR UPDATE OR DELETE
    ON equipment_capabilities
    FOR EACH ROW
EXECUTE FUNCTION track_equipment_catalog_change();

CREATE TRIGGER trg_track_equipment_configurations
    AFTER INSERT OR UPDATE OR DELETE
    ON equipment_configurations
    FOR EACH ROW
EXECUTE FUNCTION track_equipment_catalog_change();

CREATE TRIGGER trg_track_equipment_modifications
    AFTER INSERT OR UPDATE OR DELETE
    ON equipment_modifications
    FOR EACH ROW
EXECUTE FUNCTION track_equipment_catalog_change();

CREATE TRIGGER trg_track_equipment_cargo_spaces
    AFTER INSERT OR UPDATE OR DELETE
    ON equipment_cargo_spaces
    FOR EACH ROW
EXECUTE FUNCTION track_equipment_catalog_change();


-- ============================================================
-- 8. Track indirect equipment catalog dependencies
--
-- Tables:
--   equipment_configuration_capabilities
--   equipment_modification_events
-- ============================================================

CREATE OR REPLACE FUNCTION track_indirect_equipment_change()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    old_equipment_id UUID;
    new_equipment_id UUID;
BEGIN

    IF TG_OP = 'UPDATE'
        AND OLD IS NOT DISTINCT FROM NEW THEN

        RETURN NEW;

    END IF;

    IF TG_TABLE_NAME = 'equipment_configuration_capabilities' THEN

        IF TG_OP <> 'INSERT' THEN

            SELECT equipment_id
            INTO old_equipment_id
            FROM equipment_configurations
            WHERE id = OLD.configuration_id;

        END IF;

        IF TG_OP <> 'DELETE' THEN

            SELECT equipment_id
            INTO new_equipment_id
            FROM equipment_configurations
            WHERE id = NEW.configuration_id;

        END IF;

    ELSE

        IF TG_OP <> 'INSERT' THEN

            SELECT equipment_id
            INTO old_equipment_id
            FROM equipment_modifications
            WHERE id = OLD.modification_id;

        END IF;

        IF TG_OP <> 'DELETE' THEN

            SELECT equipment_id
            INTO new_equipment_id
            FROM equipment_modifications
            WHERE id = NEW.modification_id;

        END IF;

    END IF;

    IF old_equipment_id IS NOT NULL THEN

        PERFORM increment_equipment_movement_revisions(
                old_equipment_id
                );

    END IF;

    IF new_equipment_id IS NOT NULL
        AND new_equipment_id IS DISTINCT FROM old_equipment_id THEN

        PERFORM increment_equipment_movement_revisions(
                new_equipment_id
                );

    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;

    RETURN NEW;

END;
$$;

CREATE TRIGGER trg_track_configuration_capabilities
    AFTER INSERT OR UPDATE OR DELETE
    ON equipment_configuration_capabilities
    FOR EACH ROW
EXECUTE FUNCTION track_indirect_equipment_change();

CREATE TRIGGER trg_track_modification_events
    AFTER INSERT OR UPDATE OR DELETE
    ON equipment_modification_events
    FOR EACH ROW
EXECUTE FUNCTION track_indirect_equipment_change();


-- ============================================================
-- 9. Track shared cargo catalog dependencies
-- ============================================================

CREATE OR REPLACE FUNCTION track_cargo_catalog_change()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN

    IF OLD IS NOT DISTINCT FROM NEW THEN
        RETURN NEW;
    END IF;

    UPDATE movements
    SET operational_revision = operational_revision + 1
    WHERE id IN (

        SELECT lp.movement_id
        FROM loading_plans lp

                 JOIN loading_plan_cargo lpc
                      ON lpc.loading_plan_id = lp.id

        WHERE lpc.cargo_id IN (OLD.id, NEW.id)

    );

    RETURN NEW;

END;
$$;

CREATE TRIGGER trg_track_cargo_catalog_change
    AFTER UPDATE ON cargo
    FOR EACH ROW
EXECUTE FUNCTION track_cargo_catalog_change();


-- ============================================================
-- End V26
-- ============================================================
