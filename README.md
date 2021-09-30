# Proximity
A tool to assemble Magic: The Gathering proxies from a set of template images

# Docker support
To run this tool in docker, clone the repository, then run: 
```
docker build -t proximity:latest -f docker/Dockerfile .
```

Once it's done building, run the following:
```
docker run -v $PWD/_CARDS_:/app/cards.txt -v $PWD/templates/_TEMPLATE_.zip:/app/templates/template.zip -v $(PWD)/images:/app/images proximity:latest
```

Where `_CARDS_` is the name of the file containing your cards list, and `_TEMPLATE_` is the name of your template zip file. 
This assumes that your _CARDS_ file resides in the root of the Proximity project, and your _TEMPLATE_ file resides in
the templates folder.
Output of proximity will end up in a folder named images.

If you prefer docker-compose, you can run `docker-compose up` from the docker folder. If you do, you might need to modify the environment variables of the 
docker-compose.yml file to fit the name of your cards file, and your template file.
