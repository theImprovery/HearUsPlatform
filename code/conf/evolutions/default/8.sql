# -- Create km groups tables

# --- !Ups
create TABLE groups (
  id          serial PRIMARY KEY,
  name       VARCHAR(256)
);

create TABLE km_group (
  group_id  INTEGER,
  km_id     INTEGER,

  PRIMARY KEY (group_id, km_id),
  FOREIGN KEY (km_id) REFERENCES knesset_members(id) ON DELETE CASCADE,
  FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE
);

# --- !Downs
DROP TABLE km_group;
DROP TABLE groups;

