-- Migrate existing weight values from kg-numeric to grams.
-- Before this fix, the panen frontend sent raw kg values (e.g. 50 for 50 kg),
-- so rows with weight < 10000 are assumed to be kg-numeric and need ×1000.
UPDATE panen SET weight = weight * 1000 WHERE weight < 10000;
UPDATE payrolls SET weight = weight * 1000 WHERE weight < 10000;
