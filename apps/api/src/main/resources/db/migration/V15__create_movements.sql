
CREATE TABLE movements (
                           id UUID PRIMARY KEY,

                           reference VARCHAR(100) NOT NULL UNIQUE,
                           origin VARCHAR(255) NOT NULL,
                           destination VARCHAR(255) NOT NULL,

                           planned_departure TIMESTAMPTZ NOT NULL,
                           planned_arrival TIMESTAMPTZ NOT NULL,

                           status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',

                           created_at TIMESTAMPTZ NOT NULL,
                           updated_at TIMESTAMPTZ NOT NULL,

                           CONSTRAINT chk_movement_reference
                               CHECK (length(btrim(reference)) > 0),

                           CONSTRAINT chk_movement_locations
                               CHECK (
                                   length(btrim(origin)) > 0
                                       AND length(btrim(destination)) > 0
                                   ),

                           CONSTRAINT chk_movement_schedule
                               CHECK (planned_arrival > planned_departure),

                           CONSTRAINT chk_movement_status
                               CHECK (
                                   status IN (
                                              'PLANNED',
                                              'DISPATCHED',
                                              'IN_TRANSIT',
                                              'COMPLETED',
                                              'CANCELLED'
                                       )
                                   ),

                           CONSTRAINT chk_movement_timestamps
                               CHECK (updated_at >= created_at)
);

CREATE INDEX idx_movements_status
    ON movements (status);

CREATE INDEX idx_movements_departure
    ON movements (planned_departure);
