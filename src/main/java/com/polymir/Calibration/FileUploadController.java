package com.polymir.Calibration;

import com.polymir.Calibration.storage.StorageFileNotFoundException;
import com.polymir.Calibration.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;
import java.util.stream.Collectors;

@Controller
public class FileUploadController {

	private final StorageService storageService;

	@Autowired
	public FileUploadController(StorageService storageService) {
		this.storageService = storageService;
	}

	@GetMapping("/")
	public String listUploadedFiles(Model model) {
		final String addonName;
		if (model.containsAttribute("addon")) {
			addonName = Objects.requireNonNull(model.getAttribute("addon")).toString();
		} else {
			addonName = " ";
		}
		model.addAttribute("files", storageService.loadAll()
				.filter(path -> path.getFileName().toString().contains(addonName))
				.map(path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
						"serveFile", path.getFileName().toString()).build().toUri().toString())
				.collect(Collectors.toList()));

		return "uploadForm";
	}

	@GetMapping("/files/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

		Resource file = storageService.loadAsResource(filename);
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + file.getFilename() + "\"").body(file);
	}

	@PostMapping("/")
	public String handleFileUpload(@RequestParam("file") MultipartFile file,
								   @RequestParam("addon_name") String addonName,
								   @RequestParam("version") String version,
								   RedirectAttributes redirectAttributes) {

		storageService.store(file, addonName, version);
		redirectAttributes.addFlashAttribute("message",
				"?????????? ?????? ?????????????? " + file.getOriginalFilename() + " ??????????????????????.");
		redirectAttributes.addFlashAttribute("addon", addonName);

		return "redirect:/";
	}

	@ExceptionHandler(StorageFileNotFoundException.class)
	public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
		return ResponseEntity.notFound().build();
	}

}
