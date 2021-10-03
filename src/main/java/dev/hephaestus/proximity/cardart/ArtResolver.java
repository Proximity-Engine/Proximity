package dev.hephaestus.proximity.cardart;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.hephaestus.proximity.xml.RenderableCard;

public class ArtResolver
{
	private static final Logger LOG = LogManager.getLogger("LocalArtRepository");
	private final List<ArtRepository> artRepositories = new ArrayList<>();

	public ArtResolver() {
		artRepositories.add(new LocalArtRepository());
		artRepositories.add(new ScryfallArtRepository());
	}

	public Optional<String> findArt(RenderableCard card) {
		return artRepositories.stream()
			.map(artRepository -> artRepository.findArt(card))
			.filter(Objects::nonNull)
			.peek(art -> LOG.debug("Choosing art at {}", art))
			.findFirst();
	}
}
