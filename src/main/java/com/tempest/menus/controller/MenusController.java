package com.tempest.menus.controller;

import com.tempest.menus.entity.Menus;
import com.tempest.menus.service.MenusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class MenusController {

  private final MenusService menusService;
  private final Path uploadDirectory;

  @Autowired
  public MenusController(MenusService menusService, @Value("${menus.upload-dir:uploads}") String uploadDirectory) {
    this.menusService = menusService;
    this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
  }

  @GetMapping("/menus")
  public List<Menus> getAllMenus(@RequestParam(name = "keyword", required = false) String keyword) {
    return menusService.findByKeyword(keyword);
  }

  @GetMapping("/menus/{id}")
  public ResponseEntity<Menus> getMenusById(@PathVariable Integer id) {
    return menusService.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/menus")
  public ResponseEntity<Menus> createMenus(@RequestBody Menus menus) {
    Menus saved = menusService.save(menus);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
    if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
      return ResponseEntity.badRequest().body(Map.of("message", "请选择有效的图片文件"));
    }

    try {
      Files.createDirectories(uploadDirectory);
      String extension = switch (file.getContentType()) {
        case "image/jpeg" -> ".jpg";
        case "image/png" -> ".png";
        case "image/gif" -> ".gif";
        case "image/webp" -> ".webp";
        default -> "";
      };
      String filename = UUID.randomUUID() + extension;
      Files.copy(file.getInputStream(), uploadDirectory.resolve(filename));
      return ResponseEntity.ok(Map.of("url", "/api/uploads/" + filename));
    } catch (IOException exception) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message", "图片上传失败"));
    }
  }

  @GetMapping("/uploads/{filename:.+}")
  public ResponseEntity<Resource> getUploadedImage(@PathVariable String filename) {
    try {
      Path imagePath = uploadDirectory.resolve(filename).normalize();
      if (!imagePath.startsWith(uploadDirectory)) {
        return ResponseEntity.badRequest().build();
      }

      Resource resource = new UrlResource(imagePath.toUri());
      if (!resource.exists() || !resource.isReadable()) {
        return ResponseEntity.notFound().build();
      }

      MediaType mediaType = MediaTypeFactory.getMediaType(resource.getFilename())
          .orElse(MediaType.APPLICATION_OCTET_STREAM);
      return ResponseEntity.ok().contentType(mediaType).body(resource);
    } catch (IOException exception) {
      return ResponseEntity.notFound().build();
    }
  }

  @PutMapping("/menus/{id}")
  public ResponseEntity<Menus> updateMenus(@PathVariable Integer id, @RequestBody Menus menus) {
    return menusService.findById(id)
        .map(existing -> {
          menus.setId(id);
          menus.setDeleteFlag(existing.getDeleteFlag() == null ? 0 : existing.getDeleteFlag());
          return ResponseEntity.ok(menusService.save(menus));
        })
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/menus/{id}")
  public ResponseEntity<Void> deleteMenus(@PathVariable Integer id) {
    if (!menusService.deleteById(id)) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
  }
}
