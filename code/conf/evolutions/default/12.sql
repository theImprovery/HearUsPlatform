# -- Create campaign table

# --- !Ups
create TABLE campaigns (
  id            INTEGER PRIMARY KEY,
  title         VARCHAR(64),
  subtitle      text,
  website       VARCHAR(128),
  theme_data    text,
  contact_email VARCHAR(64)
);


# --- !Downs
drop table campaigns;