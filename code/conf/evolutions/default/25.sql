# --- !Ups

CREATE VIEW interaction_summary AS
    SELECT campaign_id, medium, count(*) as count
    FROM   interactions
    GROUP BY medium, campaign_id;

CREATE VIEW interaction_details AS
SELECT icn.campaign_id, icn.time, icn.medium, km.id km_id, km.name as km_name, p.id as party_id, p.name as party_name
  FROM interactions icn inner join knesset_members km on icn.km_id = km.id
       inner join parties p on km.party_id = p.id;

# --- !Downs
drop view interaction_details;
drop view interaction_summary;