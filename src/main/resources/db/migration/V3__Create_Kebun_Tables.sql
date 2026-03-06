CREATE TABLE kebun (
                       kebun_id UUID PRIMARY KEY,
                       nama VARCHAR(255) NOT NULL,
                       kode VARCHAR(64) NOT NULL,
                       luas INT NOT NULL,
                       mandor_id UUID NULL,
                       CONSTRAINT uk_kebun_kode UNIQUE (kode),
                       CONSTRAINT uk_kebun_mandor UNIQUE (mandor_id)
);

CREATE TABLE kebun_coordinate (
                                  kebun_id UUID NOT NULL,
                                  idx INT NOT NULL,
                                  lat INT NOT NULL,
                                  lng INT NOT NULL,
                                  PRIMARY KEY (kebun_id, idx),
                                  CONSTRAINT fk_kebun_coordinate_kebun
                                      FOREIGN KEY (kebun_id) REFERENCES kebun(kebun_id) ON DELETE CASCADE
);

CREATE TABLE kebun_supir (
                             id UUID PRIMARY KEY,
                             kebun_id UUID NOT NULL,
                             supir_id UUID NOT NULL,
                             CONSTRAINT fk_kebun_supir_kebun
                                 FOREIGN KEY (kebun_id) REFERENCES kebun(kebun_id) ON DELETE CASCADE,
                             CONSTRAINT uk_supir_one_kebun UNIQUE (supir_id)
);