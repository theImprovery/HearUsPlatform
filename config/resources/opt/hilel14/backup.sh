#!/bin/sh

# backup pgsql database
export PGPASSWORD=$passwords.pgsql.hear-us-platform.password
export TARGET=/var/opt/iceberg/db
rm -rf $TARGET
pg_dump --host=127.0.0.1 --dbname=hear_us_platform --username=? --format=directory --file=/var/opt/iceberg/db
/opt/hilel14/iceberg/bin/0.backup.sh --job-name=db --upload
