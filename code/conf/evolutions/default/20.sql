-- Add the "admin" flag on the user/campaign relationship
# -- !Ups
alter table user_campaign add column admin boolean;
update user_campaign set admin = true;

# -- !Downs
alter table user_campaign drop column admin;
