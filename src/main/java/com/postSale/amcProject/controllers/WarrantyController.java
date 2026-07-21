package com.postSale.amcProject.controllers;

import com.postSale.amcProject.Model.nodes.Warranty;
import com.postSale.amcProject.Services.WarrantyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/warranty")
public class WarrantyController {

    private final WarrantyService warrantyService;

    public WarrantyController(WarrantyService warrantyService) {
        this.warrantyService = warrantyService;
    }

    @GetMapping
    public List<Warranty> getAllSoonExpiring() {
        return warrantyService.getExpiringWarranties();
    }

    @GetMapping("/{id}")
    public List<Warranty> getSoonExpiringById(@PathVariable String id) {
        return warrantyService.getExpiringWarrantiesById(id);
    }
}
