# -- Create knesset member table

# --- !Ups
create TABLE knesset_members (
  id          serial PRIMARY KEY,
  name        VARCHAR(64),
  gender      VARCHAR(8),
  is_active   Boolean,
  web_page    VARCHAR(128),
  party_id    INTEGER,
  FOREIGN KEY (party_id) REFERENCES parties(id) ON DELETE RESTRICT
);

# --- !Downs
DROP TABLE knesset_members;