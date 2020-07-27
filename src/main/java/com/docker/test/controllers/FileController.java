package com.docker.test.controllers;

import com.docker.test.services.FileService;
import com.spotify.docker.client.exceptions.DockerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@RestController
public class FileController {

    @Autowired
    private FileService fileService;

    @GetMapping("/clear")
    public void clearContainers() throws DockerException, InterruptedException {
        fileService.cleanContainers();
    }

    @GetMapping("/clear-vol")
    public void clearVolumes() throws DockerException, InterruptedException {
        fileService.cleanVolumes();
    }

    @GetMapping("/")
    public ResponseEntity<Boolean> createContainers() throws DockerException, InterruptedException {
        return ResponseEntity.ok(fileService.startVolumeContainer());
    }

    @GetMapping("/create-file")
    public ResponseEntity<Boolean> createFile() throws IOException {
        return ResponseEntity.ok(fileService.testFileFolder());
    }

    @GetMapping("/run-script")
    public ResponseEntity<HashMap<String, List<String>>> runScript() throws IOException, InterruptedException {
        return ResponseEntity.ok(fileService.runScript());
    }
}
