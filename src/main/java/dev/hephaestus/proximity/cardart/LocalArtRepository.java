package dev.hephaestus.proximity.cardart;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.hephaestus.proximity.xml.RenderableCard;

public class LocalArtRepository implements ArtRepository
{
	private static List<String> artPaths;

	@Override public String findArt(RenderableCard card)
	{
		String name = card.getName();
		String set = card.getAsString("set");
		String setName = card.getAsString("set_name");
		String artist = card.getAsString("artist");

		// Regex for:  (SET)?/CARD_NAME( SET)?( ARTIST)?
		// With groups   1       2        3        4
		Pattern regex = Pattern.compile(
			".+" +	                            // Allow for anything at the start
			"("+set+"|"+setName+")?" +          // Check for set as directory name (Optional)
			".+" +	                            // Allow for path separator (Optional, in case set was a directory)
			"("+name+")" +                      // Check for the card name
			"("+set+"|"+setName+")?" +          // Check for set in file name (Optional)
			"( "+artist+")?" +                  // Check for artist in file name (Optional)
			"\\..*", Pattern.CASE_INSENSITIVE); // Check for a dot, so we know there is a file extension straigh away

		// List of all paths to art files that contains the card name
		Path artDirectory = Path.of("art");
		String match = findAllArt(artDirectory)
			.stream()
			.map(regex::matcher)
			.filter(Matcher::find)
			.sorted(this::sortArt)
			.findFirst()
			.map(Matcher::group)
			.map(Path::of)
			.map(artDirectory::resolve)
			.map(Path::toAbsolutePath)
			.map(Path::toFile)
			.map(File::toURI)
			.map(URI::toString)
			.orElse(null);

		return match;
	}

	private int sortArt(Matcher l, Matcher r) {
		// Regex for:  (SET)?/CARD_NAME( SET)?( ARTIST)?
		// With groups   1       2        3        4
		/*  Card art matcher priority
			1) SET/CARD_NAME (ARTIST)
			2) SET/CARD_NAME
			3) CARD_NAME (SET) (ARTIST)
			4) CARD_NAME (SET)
			5) CARD_NAME (ARTIST)
			6) CARD_NAME
		 */
		if(l.group(1) != null && l.group(4) != null) return -1;
		if(r.group(1) != null && r.group(4) != null) return 1;
		if(l.group(1) != null) return -1;
		if(r.group(1) != null) return 1;
		if(l.group(3) != null && l.group(4) != null) return -1;
		if(r.group(3) != null && r.group(4) != null) return 1;
		if(l.group(3) != null) return -1;
		if(r.group(3) != null) return 1;
		if(l.group(4) != null) return -1;
		if(r.group(4) != null) return 1;
		return 0;
	}

	private static synchronized List<String> findAllArt(Path root) {
		if(LocalArtRepository.artPaths != null) return LocalArtRepository.artPaths;
		List<String> artPaths = findFiles(root)
			.stream()
			.map(File::toPath)
			.map(Path::toString)
			.toList();
		LocalArtRepository.artPaths = artPaths;
		return artPaths;
	}

	private static synchronized List<File> findFiles(Path directoryPath) {
		File artDirectory = new File(directoryPath.toUri());
		File[] files = artDirectory.listFiles();
		if(files == null) {
			return new ArrayList<>();
		}

		var allFiles = new ArrayList<File>();
		for(File file : files) {
			if(file.isDirectory()) {
				allFiles.addAll(findFiles(file.toPath()));
			}
			else {
				allFiles.add( file );
			}
		}
		return allFiles;
	}
}
