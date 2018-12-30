# -- Add published/unpublished field

# -- !Ups
alter TABLE campaigns add column is_published BOOLEAN;
alter TABLE campaigns add column analytics_code TEXT;

update campaigns set is_published=false, analytics_code='';

# -- !Downs
alter table campaigns drop column is_published;
alter table campaigns drop column analytics_code;