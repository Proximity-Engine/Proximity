package dev.hephaestus.proximity.cardart;

import dev.hephaestus.proximity.xml.RenderableCard;

public class ScryfallArtRepository implements ArtRepository
{
	@Override public String findArt(RenderableCard card)
	{
		return card.getAsString("image_uris", "art_crop");
	}
}
