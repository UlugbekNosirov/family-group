package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.dataFin.notificationbot.model.Products;
import uz.dataFin.notificationbot.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;


    public List<Products> findAll(String clientId) {
        return productRepository.findAllByClientId(clientId);
    }
    public void saveCode(String code, String clientId) {
        Optional<Products> product =  productRepository.getByCodeAndClientId(clientId, code);
        if (product.isEmpty()){
            List<Products> all = productRepository.getAllByClientId(clientId);
            if (all.size()==4 || all.size()>7) {
                List<Products> allByClientId = productRepository.findAllByClientId(clientId);
                Products products = allByClientId.get(0);
                productRepository.deleteById(products.getId());
            }
            productRepository.save(Products.builder().code(code).clientId(clientId).build());
        }
    }
}
