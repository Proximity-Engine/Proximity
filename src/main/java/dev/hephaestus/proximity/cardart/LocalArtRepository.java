package dev.hephaestus.proximity.cardart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
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

public class LocalArtRepository
{
	private static final Logger LOG = LogManager.getLogger("LocalArtRepository");
	private static final Set<String> allowedFileExtensions = Set.of("png", "jpg", "jpeg", "tif", "tiff", "bmp", "svg", "gif");
	private static final List<ArtFile> artFilesCache;

	static {
		// Cache the found art files so we don't need to scan the file system several times per run
		// This is also the reason why this method is synchronized, so that several threads don't need to battle
		// to run this method
		LOG.debug("Building local art cache");
		List<ArtFile> artFiles = new ArrayList<>();
		try {
			artFiles.addAll( findFiles(Path.of("art")) );
			LOG.debug("{} art files found and cached", artFiles.size());
		}
		catch (IOException e) {
			LOG.error(e);
			LOG.error("An error occurred when building local art cache. Local art will not be used.");
		}
		artFilesCache = artFiles;
	}

	private LocalArtRepository(){}

	public static Optional<ArtInputStreamWrapper> findArt(RenderableCard card)
	{
		String name = cleanString(card.getName());
		String set = cleanString(card.getSet());
		LOG.debug("Attempting to find local art for card {} [{}]", name, set);

		List<ArtFile> artFiles = new ArrayList<>(artFilesCache);
		List<ArtFile> filteredArtFiles = new ArrayList<>();
		for(ArtFile artFile : artFiles) {
			if(!name.equals(artFile.cardName())) continue;
			if(artFile.hasSet() && !set.equals(artFile.set())) continue;
			filteredArtFiles.add(artFile);
		}
		filteredArtFiles.sort(LocalArtRepository::artFileSorter);
		ArtFile match = filteredArtFiles.size() > 0 ? filteredArtFiles.get(0) : null;
		if(match != null) LOG.debug("Found local art for {} [{}]. Location was {}", name, set, match.filePath);
		else LOG.debug("Found no local art for {} [{}]", name, set);
		return Optional.ofNullable(match)
			.map(ArtFile::filePath)
			.map(Path::toFile)
			.map(LocalArtRepository::toInputStream)
			.map(is -> new ArtInputStreamWrapper(is, match.filePath().toString()));
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

	private static FileInputStream toInputStream(File file) {
		try { return new FileInputStream(file); }
		catch (FileNotFoundException e) {
			LOG.error(e);
			LOG.error("An error occurred when loading a local art file at {}", file.getAbsolutePath());
			return null;
		}
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

