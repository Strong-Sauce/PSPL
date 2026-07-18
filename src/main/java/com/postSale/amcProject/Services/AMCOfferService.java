package com.postSale.amcProject.Services;

import com.postSale.amcProject.Exceptions.ResourceNotFoundException;
import com.postSale.amcProject.Model.nodes.AMCOffer;
import com.postSale.amcProject.Repositories.AMCOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AMCOfferService {

    private final AMCOfferRepository amcOfferRepository;

    public AMCOfferService(AMCOfferRepository amcOfferRepository) {
        this.amcOfferRepository = amcOfferRepository;
    }

    @Transactional
    public AMCOffer createOffer(AMCOffer offer) {
        return amcOfferRepository.save(offer);
    }

    @Transactional(readOnly = true)
    public List<AMCOffer> getAllOffers() {
        return amcOfferRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<AMCOffer> getOfferById(String id) {
        return amcOfferRepository.findById(id);
    }

    @Transactional
    public AMCOffer updateOffer(AMCOffer offer) {
        if (!amcOfferRepository.existsById(offer.getOfferId())) {
            throw new ResourceNotFoundException("AMCOffer", offer.getOfferId());
        }
        return amcOfferRepository.save(offer);
    }

    @Transactional
    public void deleteOffer(String id) {
        if (!amcOfferRepository.existsById(id)) {
            throw new ResourceNotFoundException("AMCOffer", id);
        }
        amcOfferRepository.deleteById(id);
    }
}

