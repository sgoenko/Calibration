package com.polymir.Calibration.storage;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
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

	private final List<Float> graduateTable19 = new ArrayList<>();
	private final List<String> graduateTable16 = new ArrayList<>();

	@Autowired
	public FileSystemStorageService(StorageProperties properties) {
		this.rootLocation = Paths.get(properties.getLocation());
		this.partsLocation = Paths.get(properties.getAddonParts());
	}

	@Override
	public void store(MultipartFile file, String addonName, String version) {
		Path destinationFile = this.rootLocation.resolve(addonName + ".L5X")
				.normalize().toAbsolutePath();

		fillGraduateTable(file, version);

		String tankName = Paths.get(Objects.requireNonNull(file.getOriginalFilename())).toString();

		appendPart(version, destinationFile, Part.HEADER, false);
		modifyFile(destinationFile, "Calibration_Header", "Calibration_" + tankName);
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		String creationDate = formatter.format(new Date());
		modifyFile(destinationFile, "Calibration_Date", creationDate);
		modifyFile(destinationFile, "Logix_Version", "v" + version);

		appendPart(version, destinationFile, Part.PARAMETERS, true);
		appendLocalTags(version, destinationFile);
		appendPart(version, destinationFile, Part.ROUTINES, true);
		appendPart(version, destinationFile, Part.FOOTER, true);

	}

	private void fillGraduateTable(MultipartFile file, String version) {
		try {
			if (version.equals("19")) {
				buildGraduateTable19(file, graduateTable19);
			} else if (version.equals("16")) {
				buildGraduateTable16(file, graduateTable16);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void appendPart(String version, Path destinationFile, Part part, boolean append) {
		Path partFile = this.partsLocation.resolve(version + "\\" + part.value + ".L5X").normalize().toAbsolutePath();

		File infile = new File(partFile.toString());
		File outfile = new File(destinationFile.toString());

		try (InputStream in = new BufferedInputStream(new FileInputStream(infile));
			 OutputStream out = new BufferedOutputStream(new FileOutputStream(outfile, append))) {
			byte[] buffer = new byte[1024];
			int length;
			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void modifyFile(Path filePath, String oldString, String newString) {
		File fileToBeModified = new File(filePath.toString());
		StringBuilder oldContent = new StringBuilder();
		BufferedReader reader = null;
		FileWriter writer = null;

		try {
			reader = new BufferedReader(new FileReader(fileToBeModified));
			String line = reader.readLine();
			while (line != null) {
				oldContent.append(line).append(System.lineSeparator());
				line = reader.readLine();
			}

			String newContent = oldContent.toString().replaceAll(oldString, newString);
			writer = new FileWriter(fileToBeModified);
			writer.write(newContent);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				assert reader != null;
				reader.close();
				assert writer != null;
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void appendLocalTags(String version, Path destinationFile) {
		Path partFile = this.partsLocation.resolve(version + "\\" + Part.LOCAL_TAGS.value + ".L5X").normalize().toAbsolutePath();

		try (FileWriter writer = new FileWriter(destinationFile.toString(), true)) {
			writer.write("<LocalTags>\n");

			addTableTag(version, writer);
			addTableContent(version, writer);
			addOtherTags(partFile, writer);

			writer.write("</LocalTags>\n");

			writer.flush();
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	}

	private void addTableTag(String version, FileWriter writer) throws IOException {
		if (version.equals("19")) {
			writer.write(String.format(
					"<LocalTag Name=\"Table\" DataType=\"REAL\" Dimensions=\"%d\" Radix=\"Float\" ExternalAccess=\"None\">\n",
					graduateTable19.size()));
		} else if (version.equals("16")) {
			writer.write(String.format(
					"<LocalTag Name=\"Table1\" DataType=\"REAL\" Dimensions=\"%d\" Radix=\"Float\">\n",
					graduateTable16.size()));
		}
		writer.write("<Description>\n");
		writer.write("<![CDATA[Таблица значений объема продукта в емкости (м3). Интервал значений соответствует UNITS.]]>\n");
		writer.write("</Description>\n");
	}

	private void addTableContent(String version, FileWriter writer) throws IOException {
		if (version.equals("19")) {
			writer.write("<DefaultData Format=\"Decorated\">\n");
			writer.write(String.format(
					"<Array DataType=\"REAL\" Dimensions=\"%d\" Radix=\"Float\">\n",
					graduateTable19.size()));

			for (int i = 0; i < graduateTable19.size(); i++) {
				String element = String.format("<Element Index=\"[%d]\" Value=\"%s\"/>\n", i, graduateTable19.get(i).toString());
				writer.write(element);
			}
			writer.write("</Array>\n");
		} else if (version.equals("16")) {
			writer.write("<DefaultData>");
			int inLine = 0;
			for (String s : graduateTable16) {
				writer.write(s + " ");
				inLine++;
				if (inLine == 4) {
					inLine = 0;
					writer.write("\n");
				}
			}
		}
		writer.write("</DefaultData>\n");
		writer.write("</LocalTag>\n");
	}

	private void addOtherTags(Path partFile, FileWriter writer) {
		try (FileReader reader = new FileReader(partFile.toString())) {
			int c;
			while ((c = reader.read()) != -1) {
				writer.write(c);
			}
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	}

	private void buildGraduateTable19(MultipartFile file, List<Float> graduateTable) throws IOException {
		try (InputStream inputStream = file.getInputStream(); Scanner sc = new Scanner(inputStream, "UTF-8")) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				line = line.replaceAll(",", ".");
				graduateTable.add(Float.parseFloat(line));
			}
		} catch (Exception e) {
			throw new StorageException("Failed to store file.", e);
		}
	}

	private void buildGraduateTable16(MultipartFile file, List<String> graduateTable) throws IOException {
		try (InputStream inputStream = file.getInputStream(); Scanner sc = new Scanner(inputStream, "UTF-8")) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				line = line.replaceAll(",", ".");
				String hex = Integer.toHexString(Float.floatToIntBits(Float.parseFloat(line))).toUpperCase();
				String result = hex.substring(6, 8) + " " + hex.substring(4, 6) + " " + hex.substring(2, 4) + " " + hex.substring(0, 2);
				graduateTable.add(result);
			}
		} catch (Exception e) {
			throw new StorageException("Failed to store file.", e);
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
