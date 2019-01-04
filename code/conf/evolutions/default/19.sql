# -- !Ups
create table campaign_texts (
  campaign_id INT,
  title text,
  subtitle text,
  body_text text,
  footer text,
  group_labels text,
  km_labels text,
  PRIMARY KEY (campaign_id),
  foreign key (campaign_id) references campaigns (id)
);


# -- !Downs
drop table campaign_texts;
