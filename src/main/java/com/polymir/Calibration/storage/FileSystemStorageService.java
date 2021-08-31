package com.polymir.Calibration.storage;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileSystemStorageService implements StorageService {

	private final Path rootLocation;
	private final Path partsLocation;

	@Autowired
	public FileSystemStorageService(StorageProperties properties) {
		this.rootLocation = Paths.get(properties.getLocation());
		this.partsLocation = Paths.get(properties.getAddonParts());
	}

	@Override
	public void store(MultipartFile file) {
		Path destinationFile = this.rootLocation.resolve(
				"Calibration_" + Paths.get(file.getOriginalFilename()) + ".L5X")
				.normalize().toAbsolutePath();

		List<Float> graduateTable = new ArrayList<>();
		try {
			buildGradueteTable(file, graduateTable);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String tankName = Paths.get(file.getOriginalFilename()).toString();

		appendPart(destinationFile, Part.HEADER, false);
		modifyFile(destinationFile, "Calibration_Header", "Calibration_" + tankName);
		appendPart(destinationFile, Part.PARAMETERS, true);
		appendLocalTags(destinationFile, graduateTable);
		appendPart(destinationFile, Part.ROUTINES, true);
		appendPart(destinationFile, Part.FOOTER, true);

	}

	private void appendPart(Path destinationFile, Part part, boolean append) {
		Path partFile = this.partsLocation.resolve(part.value + ".L5X").normalize().toAbsolutePath();

		File infile = new File(partFile.toString());
		File outfile = new File(destinationFile.toString());

		try (InputStream in = new BufferedInputStream(new FileInputStream(infile));
			 OutputStream out = new BufferedOutputStream(new FileOutputStream(outfile, append))) {
			byte[] buffer = new byte[1024];
			int length;
			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void modifyFile(Path filePath, String oldString, String newString) {
		File fileToBeModified = new File(filePath.toString());
		String oldContent = "";
		BufferedReader reader = null;
		FileWriter writer = null;

		try {
			reader = new BufferedReader(new FileReader(fileToBeModified));
			String line = reader.readLine();
			while (line != null) {
				oldContent = oldContent + line + System.lineSeparator();
				line = reader.readLine();
			}

			String newContent = oldContent.replaceAll(oldString, newString);
			writer = new FileWriter(fileToBeModified);
			writer.write(newContent);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void appendLocalTags(Path destinationFile, List<Float> graduateTable) {
		Path partFile = this.partsLocation.resolve("localTags.L5X").normalize().toAbsolutePath();

		try (FileWriter writer = new FileWriter(destinationFile.toString(), true)) {
			writer.write("<LocalTags>\n");

			writer.write(String.format(
					"<LocalTag Name=\"table\" DataType=\"REAL\" Dimensions=\"%d\" Radix=\"Float\" ExternalAccess=\"None\">\n",
					graduateTable.size()));
			writer.write("<DefaultData Format=\"Decorated\">\n");
			writer.write(String.format(
					"<Array DataType=\"REAL\" Dimensions=\"%d\" Radix=\"Float\">\n",
					graduateTable.size()));

			for (int i = 0; i < graduateTable.size(); i++) {
				String element = String.format("<Element Index=\"[%d]\" Value=\"%s\"/>\n", i, graduateTable.get(i).toString());
				writer.write(element);
			}

			writer.write("</Array>\n");
			writer.write("</DefaultData>\n");
			writer.write("</LocalTag>\n");

			try (FileReader reader = new FileReader(partFile.toString())) {
				int c;
				while ((c = reader.read()) != -1) {
					writer.write(c);
				}
			} catch (IOException ex) {
				System.out.println(ex.getMessage());
			}
			writer.write("</LocalTags>\n");

			writer.flush();
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	}

	private void buildGradueteTable(MultipartFile file, List<Float> graduateTable) throws IOException {
		InputStream inputStream = null;
		Scanner sc = null;
		try {
			inputStream = file.getInputStream();
			sc = new Scanner(inputStream, "UTF-8");
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				line = line.replaceAll(",", ".");
				graduateTable.add(Float.parseFloat(line));
			}
		} catch (Exception e) {
			throw new StorageException("Failed to store file.", e);
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
			if (sc != null) {
				sc.close();
			}
		}
	}

	@Override
	public Stream<Path> loadAll() {
		try {
			return Files.walk(this.rootLocation, 1)
					.filter(path -> !path.equals(this.rootLocation))
					.map(this.rootLocation::relativize);
		} catch (IOException e) {
			throw new StorageException("Failed to read stored files", e);
		}

	}

	@Override
	public Path load(String filename) {
		return rootLocation.resolve(filename);
	}

	@Override
	public Resource loadAsResource(String filename) {
		try {
			Path file = load(filename);
			Resource resource = new UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource;
			} else {
				throw new StorageFileNotFoundException(
						"Could not read file: " + filename);

			}
		} catch (MalformedURLException e) {
			throw new StorageFileNotFoundException("Could not read file: " + filename, e);
		}
	}

	@Override
	public void deleteAll() {
		FileSystemUtils.deleteRecursively(rootLocation.toFile());
	}

	@Override
	public void init() {
		try {
			Files.createDirectories(rootLocation);
		} catch (IOException e) {
			throw new StorageException("Could not initialize storage", e);
		}
	}
}
