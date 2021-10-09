package dev.hephaestus.proximity.cardart;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.hephaestus.proximity.xml.RenderableCard;

public class ScryfallArtRepository
{
	private static final Logger LOG = LogManager.getLogger("ScryfallArtRepository");

	public static Optional<ArtInputStreamWrapper> findArt(RenderableCard card) {
		String name = card.getName();
		String set = card.getSet();
		LOG.debug("Fetching art uri from Scryfall for card {} [{}]", name, set);

		String artUri = card.getAsString("image_uris", "art_crop");
		InputStream inputStream = null;
		if(artUri != null) {
			LOG.debug("Scryfall art uri found for card {} [{}]. The uri was: {}", name, set, artUri);
			try { inputStream = new URL(artUri).openStream(); }
			catch (IOException e) {
				LOG.error(e);
				LOG.error("The Scryfall art uri found for card {} [{}] caused an error. The uri was: {}",
					name, set, artUri);
			}
		}
		else {
			LOG.debug("No Scryfall art uri found for card {} [{}]", name, set);
		}
		return Optional.ofNullable( inputStream )
			.map(is -> new ArtInputStreamWrapper(is, artUri));
	}
}
