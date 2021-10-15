# Proximity
A tool to assemble Magic: The Gathering proxies from a set of template images. To get started, check out [the wiki](https://github.com/Haven-King/Proximity/wiki/How-to-Use).

## Building
Proximity can be built using the command `gradlew shadowJar`, or by running the `shadowJar` task in your IDE.

# Docker support
To run this tool in docker, clone the repository, then run the below command from the root of the project.
If you are on Linux, make sure the read the segment on file ownership in the [docker readme](./docker/README.md). 
```
docker-compose -f docker/docker-compose.yml up
```
For more details on the docker setup, and how to tweek if further, please see the [docker readme](./docker/README.md).