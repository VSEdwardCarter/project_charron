
CREATE TABLE loading_plan_arrangements (
                                           loading_plan_id UUID PRIMARY KEY
                                               REFERENCES loading_plans(id)
                                                   ON DELETE CASCADE,

                                           format_version INTEGER NOT NULL DEFAULT 1,

                                           request_snapshot JSONB NOT NULL,

                                           preview_snapshot JSONB NOT NULL,

                                           status VARCHAR(30) NOT NULL,

                                           generated_at TIMESTAMPTZ NOT NULL,

                                           saved_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                           updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                           CONSTRAINT chk_arrangement_format_version
                                               CHECK (format_version > 0),

                                           CONSTRAINT chk_arrangement_status
                                               CHECK (
                                                   status IN (
                                                              'VALID',
                                                              'INVALID',
                                                              'INDETERMINATE',
                                                              'NOT_PLACED'
                                                       )
                                                   ),

                                           CONSTRAINT chk_arrangement_request_object
                                               CHECK (
                                                   jsonb_typeof(request_snapshot) = 'object'
                                                   ),

                                           CONSTRAINT chk_arrangement_preview_object
                                               CHECK (
                                                   jsonb_typeof(preview_snapshot) = 'object'
                                                   ),

                                           CONSTRAINT chk_arrangement_timestamps
                                               CHECK (
                                                   updated_at >= saved_at
                                                   )
);

CREATE INDEX idx_loading_plan_arrangements_status
    ON loading_plan_arrangements(status);

CREATE INDEX idx_loading_plan_arrangements_generated
    ON loading_plan_arrangements(generated_at);
