# -- Create parties table

# --- !Ups
create TABLE parties (
  id          serial PRIMARY KEY,
  name        VARCHAR(64),
  web_page    VARCHAR(128)
);

# --- !Downs
DROP TABLE parties;