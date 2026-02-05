package com.seproject.admin.banned.controller;

import com.seproject.admin.banned.controller.dto.SpamWordRequest;
import com.seproject.admin.banned.controller.dto.SpamWordRequest.SpamWordCreateRequest;
import com.seproject.admin.banned.controller.dto.SpamWordResponse;
import com.seproject.admin.banned.service.SpamWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/spamword")
@RequiredArgsConstructor
public class SpamWordController {
    private final SpamWordService spamWordService;

    @GetMapping
    public ResponseEntity<List<SpamWordResponse>> findAll(){
        return ResponseEntity.ok(spamWordService.findAll());
    }

    @PostMapping
    public ResponseEntity<String> createSpamWord(@RequestBody SpamWordCreateRequest request){
        spamWordService.createSpamWord(request);
        return ResponseEntity.ok("success");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSpamWord(@PathVariable Long id){
        spamWordService.deleteSpamWord(id);
        return ResponseEntity.ok("success");
    }
}
