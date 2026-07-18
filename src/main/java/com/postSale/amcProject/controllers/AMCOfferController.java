package com.postSale.amcProject.controllers;

import com.postSale.amcProject.Model.nodes.AMCOffer;
import com.postSale.amcProject.Services.AMCOfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/amc-offers")
public class AMCOfferController {

    private final AMCOfferService amcOfferService;

    public AMCOfferController(AMCOfferService amcOfferService) {
        this.amcOfferService = amcOfferService;
    }

    @PostMapping
    public AMCOffer createOffer(@RequestBody AMCOffer offer) {
        return amcOfferService.createOffer(offer);
    }

    @GetMapping
    public List<AMCOffer> getAllOffers() {
        return amcOfferService.getAllOffers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AMCOffer> getOfferById(@PathVariable String id) {
        return amcOfferService.getOfferById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<AMCOffer> updateOffer(@RequestBody AMCOffer offer) {
        return ResponseEntity.ok(amcOfferService.updateOffer(offer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(@PathVariable String id) {
        amcOfferService.deleteOffer(id);
        return ResponseEntity.noContent().build();
    }
}

