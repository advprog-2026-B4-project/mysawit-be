CREATE TABLE harvest_reports (
    id UUID PRIMARY KEY,
    buruh_id UUID NOT NULL REFERENCES users(id),
    kebun_id UUID NOT NULL REFERENCES gardens(id),
    weight int NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    rejection_reason TEXT,
    created_at TIMESTAMP default CURRENT_TIMESTAMP,
    harvest_date DATE NOT NULL,
    CONSTRAINT daily_harvest UNIQUE (buruh_id, harvest_date)
);

CREATE TABLE harvest_photos (
    id UUID PRIMARY KEY,
    harvest_id UUID NOT NULL REFERENCES harvest_reports(id) ON DELETE CASCADE,
    photo_url VARCHAR(255) NOT NULL,
    created_at TIMESTAMP default CURRENT_TIMESTAMP
);
