# Notes About the Classes

## Package `Knesset`
Contains all things that model the Knesset's current state. Note that we do not manage Knesset history, so there's no need for managing memberships. If an MK is here, they are in the Knesset currenty. There's an `isActive` field on `KnessetMember`s, so allow edge cases such as sickness or such.

### class `ContactOption`

* `platform` is the platform in which the contact exists. It can be Facebook or Twitter, but also phone, fax, etc.

### class `MemberGroup`
A group of `KnessetMember`s. Normally a committee or a lobby. Can be managed as part of a campaign, or globally.

## Package `Users`

### class `User`
* `role` can be a `admin` or a `campaigner`. Admins can create new campaigns and assign campaigners to campaigns. They also manage the Knesset status.


## Package `campaigns`

### class `Campaign`

* `themeData` needs to allow at least colors for titles, texts, links, and background. Probably better to just allow a CSS snippet there, and have some nice editing capabilities for it in the UI front (and allow people that want to edit it raw to edit it raw. But add a warning to it).
* `cannedMessage` Messages user can mail to, or tweet at, MKs. These messages are different according to position and gender.
* `labelText` Labels on the MK's images, pages, and filter. Vary by position and gender.

### class `MKAction`
A relevant action of a Knesset member for the given campaign. Can have a type of (initial list):

* Tweet
* FB Post
* Media
* Vote
