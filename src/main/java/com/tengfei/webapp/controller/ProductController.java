package com.tengfei.webapp.controller;

import com.tengfei.webapp.exceptions.UserNotFoundException;
import com.tengfei.webapp.model.Product;
import com.tengfei.webapp.model.User;
import com.tengfei.webapp.repository.ProductRepository;
import com.tengfei.webapp.repository.UserRepository;
import jakarta.validation.Valid;

import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

@RestController
public class ProductController {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public ProductController(UserRepository userRepository,ProductRepository productRepository){
        this.productRepository=productRepository;
        this.userRepository=userRepository;
    }

    @PostMapping("/v1/product")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null){
            return ResponseEntity.status(401).build();
        }

        if (product.getSku()==null || product.getName()==null
                || (product.getQuantity()<0 || product.getQuantity()>100) || product.getManufacturer()==null){
            return ResponseEntity.badRequest().build();
        }

        try {
            //sku should be unique: if user can find it, it's not right
            if (productRepository.findBySku(product.getSku())!=null){
                return ResponseEntity.badRequest().build();
            }

            // get username from auth
            String username=authentication.getName();
            User loginUser = userRepository.findByUsername(username);
            product.setUser(loginUser);

            productRepository.saveAndFlush(product);
        }catch (Exception e){
            return ResponseEntity.badRequest().build();
        }

        //build response
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @GetMapping("/v1/product/{productId}")
    public ResponseEntity<Product> getProductInformation(@PathVariable int productId){
        Optional<Product> byId = productRepository.findById(productId);
        if (byId.isEmpty()){
            return ResponseEntity.notFound().build();
        }else{
            return ResponseEntity.ok(byId.get());
        }
    }

    @PutMapping("/v1/product/{productId}")
    public ResponseEntity<Product> updateProductPut(@PathVariable int productId, @Valid @RequestBody Product productDetail){
        if (productDetail.getId()!=null || productDetail.getDate_added()!=null
                || productDetail.getDate_last_updated()!=null){
            return ResponseEntity.badRequest().build();
        }
        //The product can only be updated by the user that created it.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null){
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User loginUser = userRepository.findByUsername(username);

        Optional<Product> product = productRepository.findById(productId);
        if (product.isEmpty()){
            return ResponseEntity.badRequest().build();
        }

        if (productDetail.getQuantity()<0 || productDetail.getQuantity()>100){
            return ResponseEntity.badRequest().build();
        }


        if (loginUser.getId().equals(product.get().getOwnerUserId())){
            if (productDetail.getName()!=null && productDetail.getName().length()>0){
                product.get().setName(productDetail.getName());
            }
            if (productDetail.getDescription()!=null&&productDetail.getDescription().length()>0){
                product.get().setDescription(productDetail.getDescription());
            }
            if (productDetail.getSku()!=null && productDetail.getSku().length()>0){
                if (productRepository.findBySku(productDetail.getSku())==null || product.get().getSku().equals(productDetail.getSku())){
                    product.get().setSku(productDetail.getSku());
                }else{
                    return ResponseEntity.badRequest().build();
                }
            }
            if (productDetail.getManufacturer()!=null && productDetail.getManufacturer().length()>0){
                product.get().setManufacturer(productDetail.getManufacturer());
            }
            if (productDetail.getQuantity()!=null && productDetail.getQuantity()>=0 &&productDetail.getQuantity()<=100){
                product.get().setQuantity(productDetail.getQuantity());
            }
            Product product1 = productRepository.saveAndFlush(product.get());
            return ResponseEntity.status(204).build();
        }else{
            //forbidden: user change other people's product
            return ResponseEntity.status(403).build();
        }
    }

    @PatchMapping("/v1/product/{productId}")
    public ResponseEntity<Product> patchProduct(@PathVariable Integer productId, @Valid @RequestBody Product productDetail){
        if (productDetail.getId()!=null || productDetail.getDate_added()!=null
                || productDetail.getDate_last_updated()!=null){
            return ResponseEntity.badRequest().build();
        }
        //The product can only be updated by the user that created it.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null){
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User loginUser = userRepository.findByUsername(username);

        Optional<Product> product = productRepository.findById(productId);
        if (product.isEmpty()){
            return ResponseEntity.badRequest().build();
        }

        if (productDetail.getQuantity()<0 || productDetail.getQuantity()>100){
            return ResponseEntity.badRequest().build();
        }


        if (loginUser.getId().equals(product.get().getOwnerUserId())){
            if (productDetail.getName()!=null && productDetail.getName().length()>0){
                product.get().setName(productDetail.getName());
            }
            if (productDetail.getDescription()!=null&&productDetail.getDescription().length()>0){
                product.get().setDescription(productDetail.getDescription());
            }
            if (productDetail.getSku()!=null && productDetail.getSku().length()>0){
                if (productRepository.findBySku(productDetail.getSku())==null || product.get().getSku().equals(productDetail.getSku())){
                    product.get().setSku(productDetail.getSku());
                }else{
                    return ResponseEntity.badRequest().build();
                }
            }
            if (productDetail.getManufacturer()!=null && productDetail.getManufacturer().length()>0){
                product.get().setManufacturer(productDetail.getManufacturer());
            }
            if (productDetail.getQuantity()!=null && productDetail.getQuantity()>=0 &&productDetail.getQuantity()<=100){
                product.get().setQuantity(productDetail.getQuantity());
            }
            Product product1 = productRepository.saveAndFlush(product.get());
            return ResponseEntity.status(204).build();
        }else{
            //forbidden: user change other people's product
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/v1/product/{productId}")
    public ResponseEntity<Product> deleteProduct(@PathVariable Integer productId){
        Product product = productRepository.findById(productId).orElseThrow(() -> new UserNotFoundException("User not found on : " + productId));
        if (product==null){
            return ResponseEntity.status(404).build();
        }

        //The product can only be updated by the user that created it.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null){
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User loginUser = userRepository.findByUsername(username);
        if (!loginUser.getId().equals(product.getOwnerUserId())){
            return ResponseEntity.status(403).build();
        }

        productRepository.deleteById(productId);
        return ResponseEntity.status(204).build();
    }

}
