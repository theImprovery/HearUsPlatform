# --- !Ups
create table interactions (
    id          serial PRIMARY KEY,
    campaign_id integer,
    km_id       integer,
    medium      varchar(64),
    link        text,
    time        timestamp,

    FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE
);

CREATE INDEX interactions_by_campaign ON interactions(campaign_id);

# --- !Downs
drop index interactions_by_campaign;
drop table interactions;