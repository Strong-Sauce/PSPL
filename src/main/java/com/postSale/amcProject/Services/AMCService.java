package com.postSale.amcProject.Services;

import com.postSale.amcProject.Exceptions.ResourceNotFoundException;
import com.postSale.amcProject.Model.nodes.AMC;
import com.postSale.amcProject.Repositories.AMCRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AMCService {

    private final AMCRepository amcRepository;

    public AMCService(AMCRepository amcRepository) {
        this.amcRepository = amcRepository;
    }

    @Transactional
    public AMC createAMC(AMC amc) {
        return amcRepository.save(amc);
    }

    @Transactional(readOnly = true)
    public List<AMC> getAllAMCs() {
        return amcRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<AMC> getAMCById(String id) {
        return amcRepository.findById(id);
    }

    @Transactional
    public AMC updateAMC(AMC amc) {
        if (!amcRepository.existsById(amc.getAmcId())) {
            throw new ResourceNotFoundException("AMC", amc.getAmcId());
        }
        return amcRepository.save(amc);
    }

    @Transactional
    public void deleteAMC(String id) {
        if (!amcRepository.existsById(id)) {
            throw new ResourceNotFoundException("AMC", id);
        }
        amcRepository.deleteById(id);
    }
}

