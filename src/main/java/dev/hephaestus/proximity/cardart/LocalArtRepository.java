package dev.hephaestus.proximity.cardart;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.hephaestus.proximity.xml.RenderableCard;

public class LocalArtRepository implements ArtRepository
{
	private static List<ArtFile> artPaths;

	@Override public String findArt(RenderableCard card)
	{
		String name = LocalArtRepository.cleanString(card.getName());
		String set = LocalArtRepository.cleanString(card.getAsString("set"));
		String artist = LocalArtRepository.cleanString(card.getAsString("artist"));

		// List of all paths to art files that contains the card name
		Path artDirectory = Path.of("art");
		String match = findAllArt(artDirectory)
			.stream()
			.filter(artFile -> name.equals(artFile.getCardName()) )
			.sorted(this::orderArtByPrio)
			.filter(artFile -> setFilter(artFile, set))
			.filter(artFile -> artistFilter(artFile, artist))
			.findFirst()
			.map(ArtFile::getPath)
			.map(Path::of)
			.map(Path::toAbsolutePath)
			.map(Path::toFile)
			.map(File::toURI)
			.map(URI::toString)
			.orElse(null);

		return match;
	}

	private int orderArtByPrio(ArtFile l, ArtFile r) {
		/*  Card art matcher priority
			1) CARD_NAME [SET] (ARTIST)
			2) CARD_NAME [SET]
			3) CARD_NAME (ARTIST)
			4) CARD_NAME
		 */
		if(l.hasSet() && l.hasArtist()) return -1;
		if(r.hasSet() && r.hasArtist()) return 1;
		if(l.hasSet()) return -1;
		if(r.hasSet()) return 1;
		if(l.hasArtist()) return -1;
		if(r.hasArtist()) return 1;
		return 0;
	}

	private boolean setFilter(ArtFile artFile, String set) {
		if(artFile.hasSet())
			return artFile.getSet().equals(set);
		return true;
	}

	private boolean artistFilter(ArtFile artFile, String artist) {
		if(artFile.hasArtist())
			return artFile.getArtist().equals(artist);
		return true;
	}

	private static synchronized List<ArtFile> findAllArt(Path root) {
		if(LocalArtRepository.artPaths != null) return LocalArtRepository.artPaths;
		List<ArtFile> artPaths = findFiles(root);
		LocalArtRepository.artPaths = artPaths;
		return artPaths;
	}

	private static synchronized List<ArtFile> findFiles(Path directoryPath) {
		File artDirectory = directoryPath.toFile();
		File[] files = artDirectory.listFiles();
		if(files == null) {
			return new ArrayList<>();
		}
		var allFiles = new ArrayList<ArtFile>();
		for(File file : files) {
			if(file.isDirectory()) {
				allFiles.addAll(findFiles(file.toPath()));
			}
			else {
				allFiles.add(ArtFile.of(file));
			}
		}
		return allFiles;
	}

	private static String cleanString(String s) {
		if(s == null) return null;
		return s.replaceAll("[.,'-]", "").toLowerCase(Locale.ROOT).trim();
	}

	private static class ArtFile {
		private static final Pattern cardNamePattern = Pattern.compile("^([^(\\[.])+"); //Match everything up to [ ( or .
		private static final Pattern setNamePattern = Pattern.compile("\\[(.*?)]"); //Match everything between [ and ]
		private static final Pattern artistPattern = Pattern.compile("\\((.*?)\\)"); //Match everything between ( and )
		private final String cardName;
		private final String set;
		private final String artist;
		private final String path;

		private ArtFile(String cardName, String set, String artist, String path) {
			this.cardName = cardName;
			this.set = set;
			this.artist = artist;
			this.path = path;
		}

		private boolean hasSet() { return set != null; }
		private boolean hasArtist() { return artist != null; }
		private String getCardName() { return cardName; }
		private String getSet() { return set; }
		private String getArtist() { return artist; }
		private String getPath() { return path; }

		private static ArtFile of(File file) {
			String parentDirectory = file.getParentFile().getName();
			String fileName = file.getName();

			Matcher cardNameMatches = cardNamePattern.matcher(fileName);
			String cardName = cardNameMatches.find() ? cleanString(cardNameMatches.group()) : null;

			Matcher artistMatches = artistPattern.matcher(fileName);
			String artist = artistMatches.find() ? cleanString( artistMatches.group(1) ) : null;

			Matcher setMatches = setNamePattern.matcher(fileName);
			String set = setMatches.find()
				? cleanString( setMatches.group() )
				: parentDirectory.equals("art")
				? null
				: parentDirectory;

			return new ArtFile(cardName, set, artist, file.getPath());
		}
	}
}
