# Proximity
A tool to assemble Magic: The Gathering proxies from a set of template images

# Docker support
To run this tool in docker, clone the repository, then run: 
```
docker build proximity:latest .
```

Once it's done building, run the following:
```
docker run -v $PWD/_CARDS_:/app/cards.txt -v $PWD/_TEMPLATE_.zip:/app/templates/template.zip -v $(PWD)/images:/app/images proximity:latest
```

Where `_CARDS_` is the name of the file containing your cards list, and `_TEMPLATE_` is the name of your template zip file.
Output of proximity will end up in a folder named `images`.
