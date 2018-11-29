# --- !Ups
alter table groups add column knesset_key INTEGER;

# --- !Downs
alter table groups drop column knesset_key;