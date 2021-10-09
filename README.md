# Proximity
A tool to assemble Magic: The Gathering proxies from a set of template images. To get started, check out [the wiki](https://github.com/Haven-King/Proximity/wiki/How-to-Use).

## Building
Proximity can be built using the command `gradlew shadowJar`, or by running the `shadowJar` task in your IDE.

# Art resolution
Proximity can pick up local art and use for generated cards. Place images in a
folder called `art`, and they will automatically be picked up when a card with
a matching name is generated. For further precision you can tag cards with a set
by either enclosing the set code in  `[]` characters (e.g. `Shock [BBD].jpg`) or
creating a folder inside `art` with the set code as name, and place the image
inside that folder.

If no local art is found for a card, Proximity falls back to using online solutions.