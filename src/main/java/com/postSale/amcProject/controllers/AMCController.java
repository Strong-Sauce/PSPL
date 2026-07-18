package com.postSale.amcProject.controllers;

import com.postSale.amcProject.Model.nodes.AMC;
import com.postSale.amcProject.Services.AMCService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/amcs")
public class AMCController {

    private final AMCService amcService;

    public AMCController(AMCService amcService) {
        this.amcService = amcService;
    }

    @PostMapping
    public AMC createAMC(@RequestBody AMC amc) {
        return amcService.createAMC(amc);
    }

    @GetMapping
    public List<AMC> getAllAMCs() {
        return amcService.getAllAMCs();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AMC> getAMCById(@PathVariable String id) {
        return amcService.getAMCById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<AMC> updateAMC(@RequestBody AMC amc) {
        return ResponseEntity.ok(amcService.updateAMC(amc));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAMC(@PathVariable String id) {
        amcService.deleteAMC(id);
        return ResponseEntity.noContent().build();
    }
}

