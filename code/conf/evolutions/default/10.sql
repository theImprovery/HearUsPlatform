# --- !Ups
alter table knesset_members add column knesset_key INTEGER;
alter table parties add column is_active Boolean;
DROP VIEW kms_parties;
CREATE VIEW kms_parties AS
  SELECT knesset_members.*, parties.name as party_name, parties.web_page as party_web_page, parties.is_active as party_is_active
  FROM knesset_members INNER JOIN parties
           ON knesset_members.party_id = parties.id;



# --- !Downs
alter table knesset_members drop column knesset_key;
alter table parties drop column is_active;
DROP VIEW kms_parties;
CREATE VIEW kms_parties AS
  SELECT knesset_members.*, parties.name as party_name, parties.web_page as party_web_page
  FROM knesset_members INNER JOIN parties
           ON knesset_members.party_id = parties.id;