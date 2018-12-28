# -- Create campaign table

# --- !Ups
create TABLE campaigns (
  id            serial PRIMARY KEY,
  title         VARCHAR(64),
  subtitle      text,
  website       VARCHAR(128),
  theme_data    text,
  contact_email VARCHAR(64)
);
alter table contact_options add constraint campaign_fkey FOREIGN KEY (campaign_id) references campaigns (id);

# --- !Downs
alter table contact_options drop constraint campaign_fkey;
drop table campaigns;