package com.postSale.amcProject.Services;

import com.postSale.amcProject.Model.nodes.Customer;
import com.postSale.amcProject.Model.nodes.Sale;
import com.postSale.amcProject.Repositories.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class SaleService {

    private final SaleRepository saleRepository;

    public SaleService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Transactional(readOnly = true)
    public List<Sale> getSalesForCustomer(String id) {
        return saleRepository.findAllSalesByCustomerId(id);
    }

    @Transactional
    public Sale createSale(Sale sales) {
        return saleRepository.save(sales);
    }

    @Transactional(readOnly = true)
    public List<Sale> getAllSalesOfCustomer(Customer customers) {
        return saleRepository.findAllById(Collections.singleton(customers.getCustId()));
    }
}
