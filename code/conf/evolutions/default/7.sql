# -- Create contact options table

# --- !Ups
create TABLE contact_options (
  km_id       INTEGER,
  platform    VARCHAR(64),
  title       VARCHAR(8),
  note        text,
  details     VARCHAR(128),
  PRIMARY KEY (km_id, platform),
  FOREIGN KEY (km_id) REFERENCES knesset_members(id)
);

# --- !Downs
DROP TABLE contact_options;