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
		return l.cardName().compareTo(r.cardName());
	}

	private static List<ArtFile> findFiles(Path directoryPath) throws IOException
	{
		if(!Files.exists(directoryPath))
			return new ArrayList<>();

		List<Path> files = Files.list(directoryPath).toList();
		List<ArtFile> allFiles = new ArrayList<>();
		for(Path file : files) {
			if(Files.isDirectory(file))
				allFiles.addAll(findFiles(file));
			else
				ArtFile.of(file).ifPresent(allFiles::add);
		}
		return allFiles;
	}

	private static String cleanString(String s) {
		if(s == null) return null;
		return s.replaceAll("[.,'-]", "").toLowerCase(Locale.ROOT).trim();
	}

	record ArtFile(String cardName, String set, Path filePath)
	{
		private static final Pattern cardNamePattern = Pattern.compile("^([^(\\[.])+"); //Match everything up to [ ( or .
		private static final Pattern setNamePattern = Pattern.compile("\\[(.*?)]"); //Match everything between [ and ]

		public boolean hasSet() { return set != null; }

		private static Optional<ArtFile> of(Path filePath) throws IOException {
			if (Files.isHidden(filePath)) return Optional.empty();
			if (Files.isDirectory(filePath)) return Optional.empty();
			if (!Files.isReadable(filePath)) return Optional.empty();

			String filePathStr = filePath.toString();
			if (!LocalArtRepository.allowedFileExtensions.contains(filePathStr.substring(filePathStr.lastIndexOf(".") + 1)))
				return Optional.empty();

			String parentDirectoryName = filePath.getParent().toFile().getName();
			String fileName = filePath.toFile().getName();

			Matcher cardNameMatches = cardNamePattern.matcher(fileName);
			String cardName = cardNameMatches.find() ? cleanString(cardNameMatches.group()) : null;

			// Set can be set either by a string between [], or by the folder the images are in under the "art" folder
			Matcher setMatches = setNamePattern.matcher(fileName);
			String set = setMatches.find()
				? cleanString(setMatches.group(1))
				: parentDirectoryName.equals("art")
				? null
				: parentDirectoryName;

			return Optional.of(new ArtFile(cardName, set, filePath));
		}
	}
}

