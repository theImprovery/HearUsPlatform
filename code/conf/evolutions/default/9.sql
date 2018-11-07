# -- create view for kms and parties

# --- !Ups
CREATE VIEW kms_parties AS
  SELECT knesset_members.*, parties.name as party_name, parties.web_page as party_web_page
  FROM knesset_members INNER JOIN parties
      ON knesset_members.party_id = parties.id;

# ---!Downs
DROP VIEW kms_parties;