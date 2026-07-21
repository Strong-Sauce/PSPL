package com.postSale.amcProject.controllers;

import com.postSale.amcProject.Model.nodes.Customer;
import com.postSale.amcProject.Model.nodes.Sale;
import com.postSale.amcProject.Services.SaleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping("/{id}")
    public List<Sale> getSales(@PathVariable String id) {
        return saleService.getSalesForCustomer(id);
    }

    @PostMapping
    public Sale createSales(@RequestBody Sale sales) {
        return saleService.createSale(sales);
    }

    @GetMapping
    public List<Sale> getAllSalesOfCust(@RequestBody Customer customers) {
        return saleService.getAllSalesOfCustomer(customers);
    }
}
