-- Create email change
# -- !Ups
create table email_changes (
    user_id          integer,
    previous_address varchar(255),
    new_address      varchar(255),
    change_date      timestamp,

    PRIMARY KEY (user_id, change_date)
);



# -- !Downs
drop table email_changes;
