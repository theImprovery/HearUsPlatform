# -- Alter images table

# --- !Ups
alter table images drop constraint images_pkey;
alter table images add column id serial PRIMARY KEY;
alter table images add column cam_id integer;

# --- !Downs
alter table images drop column cam_id;
alter table images drop column id;
alter table images add primary key (km_id);
