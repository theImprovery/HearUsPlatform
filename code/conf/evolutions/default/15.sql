# -- add messages table

# --- !Ups
create TABLE system_events (
  id        serial PRIMARY KEY,
  user_id   INTEGER,
  date      TIMESTAMP,
  message   VARCHAR(512),
  details   text
);

# --- !Downs
drop TABLE system_events;