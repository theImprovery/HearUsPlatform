# -- Add link table between users and campaigns

# -- !Ups
create table user_campaign (
  user_id INTEGER,
  campaign_id INTEGER,

  primary key (user_id, campaign_id),
  foreign key (user_id) references users(id),
  foreign key (campaign_id) references campaigns(id)
);

# -- !Downs
drop table user_campaign;
