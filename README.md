# Proximity
A tool to assemble Magic: The Gathering proxies from a set of template images

# Docker support
To run this tool in docker, clone the repository, then, depending on platform, run the below command from the root of the project:
```
--LINUX 
GROUP_ID=$(id -g) USER_ID=$(id -u) docker-compose -f docker/docker-compose.yml up

-- WINDOWS or MAC
docker-compose -f docker/docker-compose.yml up
```

For more details on the docker setup, and how to tweek if further, please see the [docker readme](./docker/README.md)
