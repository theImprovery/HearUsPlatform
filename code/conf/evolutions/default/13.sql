# -- Create campaigns' related table

# --- !Ups
create TABLE label_texts (
  cam_id     INTEGER,
  position   VARCHAR(16),
  gender     VARCHAR(8),
  text       text,
  PRIMARY KEY (cam_id, position, gender),
  FOREIGN KEY (cam_id) REFERENCES campaigns(id) ON DELETE CASCADE
);

create TABLE relevant_groups (
  cam_id     INTEGER,
  group_id   INTEGER,

  PRIMARY KEY (cam_id, group_id),
  FOREIGN KEY (cam_id) REFERENCES campaigns(id) ON DELETE CASCADE,
  FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE
);

create TABLE canned_messages (
  cam_id     INTEGER,
  position   VARCHAR(16),
  gender     VARCHAR(8),
  platform   VARCHAR(64),
  text       text,
  PRIMARY KEY (cam_id, position, gender, platform),
  FOREIGN KEY (cam_id) REFERENCES campaigns(id) ON DELETE CASCADE
);

create TABLE social_media (
  id        serial PRIMARY KEY,
  cam_id    INTEGER,
  name      VARCHAR(64),
  service   VARCHAR(64),
  FOREIGN KEY (cam_id) REFERENCES campaigns(id) ON DELETE CASCADE
);

create TABLE km_positions (
  km_id     INTEGER,
  cam_id    INTEGER,
  position  VARCHAR(16),
  PRIMARY KEY (km_id, cam_id),
  FOREIGN KEY (km_id) REFERENCES knesset_members(id) ON DELETE CASCADE,
  FOREIGN KEY (cam_id) REFERENCES campaigns(id) ON DELETE CASCADE
);

create TABLE km_actions (
  id        serial PRIMARY KEY,
  cam_id    INTEGER,
  km_id     INTEGER,
  type      VARCHAR(16),
  date      TIMESTAMP,
  title     VARCHAR(64),
  details   text,
  link      VARCHAR(128),
  FOREIGN KEY (cam_id) REFERENCES campaigns(id) ON DELETE CASCADE,
  FOREIGN KEY (km_id) REFERENCES knesset_members(id) ON DELETE CASCADE
);

# --- !Downs
drop table label_texts;
drop table relevant_groups;
drop table canned_messages;
drop table social_media;
drop table km_positions;
drop table km_actions;