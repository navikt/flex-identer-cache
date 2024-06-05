DROP TABLE identer;

CREATE TABLE aktor (
                       aktor_id VARCHAR(255) PRIMARY KEY
);

CREATE TABLE identifikator (
                               idnummer VARCHAR(255) PRIMARY KEY,
                               type VARCHAR(255),
                               gjeldende BOOLEAN,
                               oppdatert TIMESTAMP,
                               aktor_id VARCHAR(255),
                               FOREIGN KEY (aktor_id) REFERENCES aktor(aktor_id)
);
