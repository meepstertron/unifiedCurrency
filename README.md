# Unified Currency
A Simple Currency API/Mod made for fabric, intended to "unify" currency in modded setups. 

The project exposes a Java API and a few administrative commands to manage the currency. 

## Features
- Configurable Currency
- Management Commands
- Lightweight

## Installation
- Grab a JAR from the [releases](https://github.com/meepstertron/unifiedCurrency/releases)
- Use the CI/CD artifacts (Might be Unstable)
- Comming to Modrinth soon!
- use gradle locally to build your own:

1. Navigate to the root folder of the project
2. Run `./gradlew build`
3. Your JAR is located in /lib

> Warning! The Mod should only be used on a fabric server. (i havent tested it client side)

Once acquired place the .jar in your fabric server's mods folder, i also recommend getting [LuckPerms](https://luckperms.net/) for permissions

## Usage

### For Admins
- `/uc balance` - Get your own balance
- `/uc balance get <Player>` - Get a player's Balance
- `/uc balance set <Player> <amount>` - Set a player's Balance
- `/uc balance add <Player> <amount>` - Add to a player's Balance
- `/uc reload` - Reload the Config
- `/uc recalculatebalances` - Uses Transaction logs to recalculate balances 

### For Developers
Please Refer to the javadoc

### For Players
- `/balance`
- `/pay <Player> <Ammount>`

## Config
The config is located under `/config/unfiedcurrency.yml`

This is the default content:
```yaml
database_file: "/database.db"
allow_player_transactions: true
starter_currency: 100
```
## Demo
Watch me ramble: https://slack-files.com/T09V59WQY1E-F0APN7BEV9V-8ec2a951a6

## Contributing
Pull requests are welcome. Open an issue for major changes.

## License
MIT @ [LICENSE](LICENSE.txt)
