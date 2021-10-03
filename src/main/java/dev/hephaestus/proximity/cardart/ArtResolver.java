package dev.hephaestus.proximity.cardart;

import java.util.ArrayList;
import java.util.List;

import dev.hephaestus.proximity.xml.RenderableCard;

public class ArtResolver
{
	private final List<ArtRepository> artRepositories = new ArrayList<>();

	public ArtResolver() {
		artRepositories.add(new LocalArtRepository());
		artRepositories.add(new ScryfallArtRepository());
		artRepositories.add(new NoneArtRepository());
	}

	public String findArt(RenderableCard card) {
		return artRepositories.stream()
			.map(artRepository -> artRepository.findArt(card))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("No art could be found for card " + card.getName()));
	}
}
