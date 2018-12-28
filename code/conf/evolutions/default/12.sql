# -- Create campaign table

# --- !Ups
create TABLE campaigns (
  id            serial PRIMARY KEY,
  title         VARCHAR(128),
  slogan        text,
  slug          VARCHAR(128),
  website       VARCHAR(256),
  theme_data    text,
  contact_email VARCHAR(64)
);
CREATE UNIQUE INDEX lowercase_slugs ON campaigns (lower(slug));

alter table contact_options add constraint campaign_fkey FOREIGN KEY (campaign_id) references campaigns (id);

# --- !Downs
alter table contact_options drop constraint campaign_fkey;
drop table campaigns;