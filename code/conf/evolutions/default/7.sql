# -- Create contact options table

# --- !Ups
create TABLE contact_options (
  km_id       INTEGER,
  platform    VARCHAR(64),
  title       VARCHAR(256),
  note        text,
  details     VARCHAR(256),
  PRIMARY KEY (km_id, platform),
  FOREIGN KEY (km_id) REFERENCES knesset_members(id) ON DELETE CASCADE
);

# --- !Downs
DROP TABLE contact_options;