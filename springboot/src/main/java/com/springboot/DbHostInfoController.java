package com.springboot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Here are all of the api mappings. These are essentially the "websites" that you visit in order to trigger operations
 */
@RestController
@RequestMapping("/api/host_info")
public class DbHostInfoController {
    @Autowired
    private DbHostInfoRepository dbHostInfoRepository;


    // Endpoint to register a file
    @PostMapping("/registerFile")
    public String registerFile(@RequestBody DbHostInfo dbHostInfo) {
        if (dbHostInfoRepository.findByFilename(dbHostInfo.getFilename()) != null) {
            return "File with the same name already exists";
        }

        // Save the file registration information
        dbHostInfoRepository.save(dbHostInfo);
        return "File registered successfully";
    }

    //Endpoint to remove a file registration by filename
    @DeleteMapping("/removeFile/{filename}")
    public ResponseEntity<String> removeFile(
            @PathVariable String filename,
            @RequestParam String clientUsername) {

        DbHostInfo dbHostInfo = dbHostInfoRepository.findByFilenameAndUsername(filename, clientUsername);

        if (dbHostInfo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        }

        if (!dbHostInfo.getUsername().equals(clientUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission to delete this file");
        }

        // Remove the file registration
        dbHostInfoRepository.delete(dbHostInfo);
        return ResponseEntity.ok("File removed successfully");
    }



    //Endpoint to search for a file by name
    @GetMapping("/searchFile")
    public boolean searchFile(@RequestParam String filename, @RequestParam String clientUsername) {
        // Find the file by filename
        DbHostInfo dbHostInfo = dbHostInfoRepository.findByFilename(filename);
        if (dbHostInfo == null) {
            return false; // File not found
        }

        // Check if the file is shared with the client
        List<String> sharedWith = dbHostInfo.getSharedWith();
        return sharedWith.contains(clientUsername);
    }

    //Endpoint to confirm download and get IP address and port
    @GetMapping("/confirmDownload")
    public ResponseEntity<HostInfo> confirmDownload(@RequestParam String filename, @RequestParam String clientUsername) {
        DbHostInfo dbHostInfo = dbHostInfoRepository.findByFilename(filename);
        if (dbHostInfo == null) {
            return ResponseEntity.notFound().build();
        }

        // Check if the file is shared with the client
        List<String> sharedWith = dbHostInfo.getSharedWith();
        if (!sharedWith.contains(clientUsername)) {
            return ResponseEntity.notFound().build(); // File not shared with the client
        }

        // Return the host information
        HostInfo hostInfo = new HostInfo();
        hostInfo.setHostUsername(dbHostInfo.getUsername());
        hostInfo.setHostIp(dbHostInfo.getHostIp());
        hostInfo.setHostPort(dbHostInfo.getHostPort());

        return ResponseEntity.ok(hostInfo);
    }
}
