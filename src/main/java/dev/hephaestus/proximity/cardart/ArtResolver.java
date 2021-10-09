package dev.hephaestus.proximity.cardart;

import java.util.Optional;

import dev.hephaestus.proximity.xml.RenderableCard;

public class ArtResolver
{
	public static ArtInputStreamWrapper findArt(RenderableCard card) {
		return Optional.of(card)
			.flatMap(LocalArtRepository::findArt)
			.or(() -> ScryfallArtRepository.findArt(card))
			.orElseGet(ArtInputStreamWrapper::nullInputStream);
	}
}
