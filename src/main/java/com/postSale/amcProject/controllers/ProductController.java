package com.postSale.amcProject.controllers;

import com.postSale.amcProject.Model.nodes.Product;
import com.postSale.amcProject.Services.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // POST REQS
    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    // PUT REQS
    @PutMapping()
    public ResponseEntity<Product> updateCustomer(@RequestBody Product product){
        Product updatedProduct = productService.updateProd(product);
        return ResponseEntity.ok(updatedProduct);
    }


    // GET REQS
    @GetMapping
    public List<Product> getAllProducts(){
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable String id){
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    // DELETE REQS
    @DeleteMapping("/{id}")
    public ResponseEntity<Product> deleteProduct(@PathVariable String id) {
        boolean deletedProoduct = productService.deleteProduct(id);
        if(!deletedProoduct)
            return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }
}
