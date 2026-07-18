package com.postSale.amcProject.Services;

import com.postSale.amcProject.Exceptions.ResourceNotFoundException;
import com.postSale.amcProject.Model.nodes.Customer;
import com.postSale.amcProject.Repositories.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Customer createCust(Customer customer) {
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCus(Customer customers) {
        if (!customerRepository.existsById(customers.getCustId())) {
            throw new ResourceNotFoundException("Customer", customers.getCustId());
        }
        return customerRepository.save(customers);
    }

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerById(String id) {
        return customerRepository.findById(id);
    }

    @Transactional
    public boolean deleteCustomer(String id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Customer", id);
        }
        customerRepository.deleteById(id);
        return true;
    }
}
