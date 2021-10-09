package dev.hephaestus.proximity.cardart;

import java.io.InputStream;

public record ArtInputStreamWrapper(InputStream inputStream, String locationString)
{
	public static ArtInputStreamWrapper nullInputStream() {
		return new ArtInputStreamWrapper(InputStream.nullInputStream(), "<blank>");
	}
}
