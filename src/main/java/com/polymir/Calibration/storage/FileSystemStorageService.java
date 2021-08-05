package com.polymir.Calibration.storage;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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

		appendHeader(destinationFile);
		appendParameters(destinationFile);
		appendLocalTags(destinationFile, graduateTable);
		appendRoutines(destinationFile);
		appendFooter(destinationFile);

	}

	private void appendHeader(Path destinationFile) {
		Path partFile = this.partsLocation.resolve("header.L5X").normalize().toAbsolutePath();

		try(FileWriter writer = new FileWriter(destinationFile.toString(), false))
		{
			try(FileReader reader = new FileReader(partFile.toString()))
			{
				int c;
				while((c=reader.read())!=-1){
					writer.write(c);
				}
			}
			catch(IOException ex){
				System.out.println(ex.getMessage());
			}

			writer.flush();
		}
		catch(IOException ex){
			System.out.println(ex.getMessage());
		}
	}

	private void appendParameters(Path destinationFile) {
		Path partFile = this.partsLocation.resolve("parameters.L5X").normalize().toAbsolutePath();

		try(FileWriter writer = new FileWriter(destinationFile.toString(), true))
		{
			try(FileReader reader = new FileReader(partFile.toString()))
			{
				int c;
				while((c=reader.read())!=-1){
					writer.write(c);
				}
			}
			catch(IOException ex){
				System.out.println(ex.getMessage());
			}

			writer.flush();
		}
		catch(IOException ex){
			System.out.println(ex.getMessage());
		}
	}

	private void appendLocalTags(Path destinationFile, List<Float> graduateTable) {
		Path partFile = this.partsLocation.resolve("localTags.L5X").normalize().toAbsolutePath();

		try(FileWriter writer = new FileWriter(destinationFile.toString(), true))
		{
			writer.write("<LocalTags>\n");

			writer.write("<LocalTag Name=\"table\" DataType=\"REAL\" Dimensions=\"20\" Radix=\"Float\" ExternalAccess=\"None\">\n");
			writer.write("<DefaultData Format=\"Decorated\">\n");
			writer.write("<Array DataType=\"REAL\" Dimensions=\"20\" Radix=\"Float\">\n");

			for (int i=0; i<graduateTable.size(); i++) {
				String element = String.format("<Element Index=\"[%d]\" Value=\"%f\"/>\n", i, graduateTable.get(i));
				writer.write(element);
			}
			//<Element Index="[0]" Value="2.0"/>

			writer.write("</Array>\n");
			writer.write("</DefaultData>\n");
			writer.write("</LocalTag>\n");

			try(FileReader reader = new FileReader(partFile.toString()))
			{
				int c;
				while((c=reader.read())!=-1){
					writer.write(c);
				}
			}
			catch(IOException ex){
				System.out.println(ex.getMessage());
			}
			writer.write("</LocalTags>\n");

			writer.flush();
		}
		catch(IOException ex){
			System.out.println(ex.getMessage());
		}
	}

	private void appendRoutines(Path destinationFile) {
		Path partFile = this.partsLocation.resolve("routines.L5X").normalize().toAbsolutePath();

		try(FileWriter writer = new FileWriter(destinationFile.toString(), true))
		{
			try(FileReader reader = new FileReader(partFile.toString()))
			{
				int c;
				while((c=reader.read())!=-1){
					writer.write(c);
				}
			}
			catch(IOException ex){
				System.out.println(ex.getMessage());
			}

			writer.flush();
		}
		catch(IOException ex){
			System.out.println(ex.getMessage());
		}
	}

	private void appendFooter(Path destinationFile) {
		Path partFile = this.partsLocation.resolve("footer.L5X").normalize().toAbsolutePath();

		try(FileWriter writer = new FileWriter(destinationFile.toString(), true))
		{
			try(FileReader reader = new FileReader(partFile.toString()))
			{
				int c;
				while((c=reader.read())!=-1){
					writer.write(c);
				}
			}
			catch(IOException ex){
				System.out.println(ex.getMessage());
			}

			writer.flush();
		}
		catch(IOException ex){
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
