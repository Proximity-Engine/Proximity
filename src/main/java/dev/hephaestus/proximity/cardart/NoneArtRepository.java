package dev.hephaestus.proximity.cardart;

import dev.hephaestus.proximity.xml.RenderableCard;

public class NoneArtRepository implements ArtRepository
{
	@Override
	public String findArt(RenderableCard card)
	{
		return null;
	}
}
