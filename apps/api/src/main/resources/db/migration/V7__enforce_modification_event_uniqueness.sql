
CREATE UNIQUE INDEX uq_modification_event_type
    ON equipment_modification_events (
                                      modification_id,
                                      type
        );
