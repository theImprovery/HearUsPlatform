# -- Add user roles

# -- !Ups
alter table users add column roles INTEGER;

update users set roles=1;

# -- !Downs
alter table users drop column roles;
