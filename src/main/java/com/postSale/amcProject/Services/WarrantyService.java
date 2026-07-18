package com.postSale.amcProject.Services;

import com.postSale.amcProject.Model.nodes.Warranty;
import com.postSale.amcProject.Repositories.WarrantyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WarrantyService {

    private final WarrantyRepository warrantyRepository;

    public WarrantyService(WarrantyRepository warrantyRepository) {
        this.warrantyRepository = warrantyRepository;
    }

    @Transactional(readOnly = true)
    public List<Warranty> getExpiringWarranties() {
        return warrantyRepository.findWarrantiesExpiringSoon();
    }

    @Transactional(readOnly = true)
    public List<Warranty> getExpiringWarrantiesById(String custId) {
        return warrantyRepository.findWarrantiesByCustomerId(custId);
    }
}
