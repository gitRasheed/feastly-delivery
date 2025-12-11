CREATE TABLE driver_status (
    driver_id UUID PRIMARY KEY,
    is_available BOOLEAN NOT NULL DEFAULT FALSE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_driver_status_available ON driver_status(is_available) WHERE is_available = TRUE;
