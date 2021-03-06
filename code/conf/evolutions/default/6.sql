# -- Create image table

# --- !Ups
create TABLE images (
  km_id       INTEGER PRIMARY KEY,
  suffix      VARCHAR(64),
  mime_type   VARCHAR(64),
  date        TIMESTAMP,
  credit      VARCHAR(64),
  FOREIGN KEY (km_id) REFERENCES knesset_members(id) ON DELETE CASCADE
);

# --- !Downs
DROP TABLE images;