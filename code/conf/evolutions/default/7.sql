# -- Create contact options table

# --- !Ups
create TABLE contact_options (
  id          SERIAL,
  km_id       INTEGER,
  campaign_id INTEGER,
  platform    VARCHAR(64),
  title       VARCHAR(256),
  note        text,
  details     VARCHAR(512),
  FOREIGN KEY (km_id) REFERENCES knesset_members(id) ON DELETE CASCADE
);

# --- !Downs
DROP TABLE contact_options;