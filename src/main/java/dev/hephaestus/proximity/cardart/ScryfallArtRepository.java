package dev.hephaestus.proximity.cardart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.hephaestus.proximity.xml.RenderableCard;

public class ScryfallArtRepository implements ArtRepository
{
	private static final Logger LOG = LogManager.getLogger("ScryfallArtRepository");
	@Override public String findArt(RenderableCard card) {
		String name = card.getName();
		String set = card.getSet();
		LOG.debug("Fetching art uri from Scryfall for card {} [{}]", name, set);

		String artUri = card.getAsString("image_uris", "art_crop");
		if(artUri != null) LOG.debug("Scryfall art uri found for card {} [{}] at: {}", name, set, artUri);
		else LOG.debug("No Scryfall art uri found for card {} [{}]", name, set);

		return artUri;
	}
}
