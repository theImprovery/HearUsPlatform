# -- Add published/unpublished field

# -- !Ups
alter TABLE campaigns add column is_published BOOLEAN;

# -- !Downs
alter table campaigns drop column is_published;