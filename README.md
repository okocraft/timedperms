## TimedPerms
TimedPerms, plugin to implements in-game time temporary permission using LuckPerms.

LuckPerms only supports realtime temporary permission. So this plugin supports in-game temporary permission.

The time of temporary permission managed by this plugin will be decreased by 1 on every second only when a player is online.

### Install
build and drop jar in plugins folder. (wip)

### Commands
These commands require permission `timedperms.use`.
* `/timedperms show <player> <permission [context=value...]>`
* `/timedperms add <player> <permission [context=value...]> <time-in-seconds>`
* `/timedperms set <player> <permission [context=value...]> <time-in-seconds>`
* `/timedperms remove <player> <permission [context=value...]> [time-in-seconds]`

### Placeholders (PlaceholderAPI)
Format of placeholders which return time of permission are:
* `%timedperms[_time|_seconds][_<player>]_<permission>_<context=value>_...%`
* `%timedperms[,time|,seconds][,<player>],<permission>,<context=value>,...%`

For example:
* `%timedperms_time_test.command.use_world=lobby_server=survival%`
* `%timedperms,time,player_name,test_plugin.command.use,world=lobby,server=survival%`

_time placeholder returns HHh:MMm:SSs format time.
_seconds placeholder returns time in seconds.
If invalid argument is specified, it treated as _seconds.

If permission contains underscore, use comma to retrieve data.
Even if player name contains underscore, you can use both format.

If permission is not set or no data can be retrieved, placeholder return "-1".

### Languages
Edit `plugins/TimedPerms/languages/en.properties` or `ja.properties` to customize messages.
Legacy YAML language files are no longer loaded; transfer custom translations manually
using MiniMessage formatting and named placeholders from the generated files.

### For Developers
First, retrieve LocalPlayer from LocalPlayerFactory#get(). Then you can do some stuff with it. Methods which operate time of permission returns calculated new value. Those time operation will fire event below.

TimedPerms have some events:
* `TimedPermissionRegisteredEvent`: Fired when timed permission is registered via adding time.
* `TimedPermissionUnregisteredEvent`: Fired when timed permission is unregistered via removing time or counting.
* `TimedPermissionCountEvent`: Fired when time of permission is decreased by 1 every second.
* `TimedPermissionExpireEvent`: Fired when timed permission is expired via counting. Expire event will be fired after count event.
* `TimedPermissionAddEvent`: Fired when time of permission is increased via adding time.
* `TimedPermissionRemoveEvent`: Fired when time of permission is decreased via removing time.
* `TimedPermissionSetEvent`: Fired when time of permission is set.
