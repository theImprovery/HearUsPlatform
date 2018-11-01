# Server setup

Use files in `resources` folder. Replace $passwords place-holders with real passwords.

0. Install [CentOS](https://www.centos.org/).
0. Install and configure HTTPd
   * Install HTTPd
   * Configure HTTPd to serve as Play proxy
   * Enable HTTPS with [Let’s Encrypt](https://letsencrypt.org/).  
0. Install and configure PostgreSQL
   * Install PostgreSQL
   * Configure PostgreSQL security
   * Create PostgreSQL database and user
0. Install Java
0. Deploy Play app
   * Install Play app service
0. Install and configure IceBerg
   * Pull lastes version with Git
   * Build with Maven
   * Deploy
   * Configure backup jobs for database
