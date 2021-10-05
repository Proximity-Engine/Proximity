# Proximity
A tool to assemble Magic: The Gathering proxies from a set of template images

# Docker support
To run this tool in docker, clone the repository, then, depending on platform, run the below command from the root of the project:
```
-- LINUX
docker build -t proximity:latest -f docker/Dockerfile --build-arg GROUP_ID=$(id -g) --build-arg USER_ID=$(id -u) .

-- WINDOWS or MAC
docker build -t proximity:latest -f docker/Dockerfile .
```

Once it's done building, run the command below. Output of proximity will end up in a folder named `images`.
```
docker-compose -f docker/docker-compose.yml up
```


For more details on the docker setup, and how to tweek if further, please see the [docker readme](./docker/README.md)
