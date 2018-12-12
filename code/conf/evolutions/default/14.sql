# -- Alter images table

# --- !Ups
alter table images drop constraint images_pkey;
alter table images add column id serial PRIMARY KEY;
alter table images
  add column cam_id integer,
  add constraint fk_cam_id
  foreign key (cam_id)
  references campaigns (id) on delete cascade;
alter table images alter column km_id drop not null;

# --- !Downs
alter table images drop column cam_id;
alter table images drop column id;
alter table images add primary key (km_id);
alter table images alter column km_id set NOT NULL ;
