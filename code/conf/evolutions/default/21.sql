-- Change isPublished to campaignStatus
# -- !Ups
alter table campaigns rename column is_published to status;
alter table campaigns alter column status TYPE int USING status::integer;
update campaigns set status=0;

# -- !Downs
alter table campaigns rename column status to is_published;
alter table campaigns alter column is_published TYPE BOOLEAN USING is_published::boolean;
update campaigns set is_published=false;