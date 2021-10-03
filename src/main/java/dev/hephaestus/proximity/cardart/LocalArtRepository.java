package dev.hephaestus.proximity.cardart;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.hephaestus.proximity.xml.RenderableCard;

public class LocalArtRepository implements ArtRepository
{
	private static final Logger LOG = LogManager.getLogger("LocalArtRepository");
	private static final Set<String> allowedFileExtensions = Set.of("png", "jpg", "jpeg", "tif", "tiff", "bmp", "svg", "gif");
	private static volatile List<ArtFile> artFilesCache;

	@Override public String findArt(RenderableCard card)
	{
		String name = cleanString(card.getName());
		String set = cleanString(card.getSet());
		LOG.debug("Attempting to find local art for card {} [{}]", name, set);
		// List of all paths to art files that contains the card name
		Path artDirectory = Path.of("art");
		String match = findAllArt(artDirectory)
			.stream()
			.filter(artFile -> name.equals(artFile.getCardName()) )
			.filter(artFile -> setFilter(artFile, set))
			.sorted(LocalArtRepository::artFileSorter)
			.findFirst()
			.map(ArtFile::getPath)
			.map(Path::of)
			.map(Path::toAbsolutePath)
			.map(Path::toFile)
			.map(File::toURI)
			.map(URI::toString)
			.orElse(null);

		if(match != null) LOG.debug("Local art found for card {} [{}] at location {}", name, set, match);
		else LOG.debug("No local art found for card {} [{}]", name, set);

		return match;
	}

	private static int artFileSorter(ArtFile l, ArtFile r) {
		// Since we sort after filtering away art files without matching set, this sorter effectively just sorts
		// art with a set assigned to it over art without a set assigned
		if(l.hasSet()) return -1;
		if(r.hasSet()) return 1;
		return l.getCardName().compareTo(r.getCardName());
	}

	private static boolean setFilter(ArtFile artFile, String set) {
		// Discard art with set assigned, but the set doesn't match. Art without set is OK
		if(artFile.hasSet())
			return artFile.getSet().equals(set);
		return true;
	}

	private static synchronized List<ArtFile> findAllArt(Path root) {
		// Cache the found art files so we don't need to scan the file system several times per run
		// This is also the reason why this method is synchronized, so that several threads don't need to battle
		// to run this method
		if(LocalArtRepository.artFilesCache != null) {
			LOG.debug("{} cached art files found", artFilesCache.size());
			return artFilesCache;
		}

		LOG.debug("No cached art files found. Building cache");
		artFilesCache = findFiles(root);
		LOG.debug("{} art files found and cached", artFilesCache.size());
		return artFilesCache;
	}

	private static synchronized List<ArtFile> findFiles(Path directoryPath) {
		File artDirectory = directoryPath.toFile();
		if(!artDirectory.exists())
			return new ArrayList<>();

		File[] files = artDirectory.listFiles();
		if(files == null)
			return new ArrayList<>();

		List<ArtFile> allFiles = new ArrayList<ArtFile>();
		for(File file : files) {
			if(file.isDirectory())
				allFiles.addAll(findFiles(file.toPath()));
			else
				ArtFile.of(file).ifPresent(allFiles::add);
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
		private final String cardName;
		private final String set;
		private final String path;

		private ArtFile(String cardName, String set, String path) {
			this.cardName = cardName;
			this.set = set;
			this.path = path;
		}

		private boolean hasSet() { return set != null; }
		private String getCardName() { return cardName; }
		private String getSet() { return set; }
		private String getPath() { return path; }

		private static Optional<ArtFile> of(File file) {
			if(file.isHidden()) return Optional.empty();
			if(file.isDirectory()) return Optional.empty();
			if(!file.canRead()) return Optional.empty();

			String filePath = file.getPath();
			if(!allowedFileExtensions.contains( filePath.substring(filePath.lastIndexOf(".")+1) ))
				return Optional.empty();

			String parentDirectory = file.getParentFile().getName();
			String fileName = file.getName();

			Matcher cardNameMatches = cardNamePattern.matcher(fileName);
			String cardName = cardNameMatches.find() ? cleanString(cardNameMatches.group()) : null;

			// Set can be set either by a string between [], or by the folder the images are in under the "art" folder
			Matcher setMatches = setNamePattern.matcher(fileName);
			String set = setMatches.find()
				? cleanString( setMatches.group(1) )
				: parentDirectory.equals("art")
				? null
				: parentDirectory;

			return Optional.of(new ArtFile(cardName, set, filePath));
		}
	}
}
