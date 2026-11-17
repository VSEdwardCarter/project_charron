
CREATE TABLE equipment (
                           id UUID PRIMARY KEY,
                           name VARCHAR(255) NOT NULL,
                           category VARCHAR(50) NOT NULL,
                           vin VARCHAR(17)
);
