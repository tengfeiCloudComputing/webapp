package com.tengfei.webapp.controller;

import com.tengfei.webapp.exceptions.UserNotFoundException;
import com.tengfei.webapp.model.Product;
import com.tengfei.webapp.model.User;
import com.tengfei.webapp.repository.ProductRepository;
import com.tengfei.webapp.repository.UserRepository;
import com.timgroup.statsd.StatsDClient;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private StatsDClient statsDClient;

    public ProductController(UserRepository userRepository,ProductRepository productRepository){
        this.productRepository=productRepository;
        this.userRepository=userRepository;
    }

    @PostMapping("/v1/product")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product){

        statsDClient.incrementCounter("csye6225.http.create.product");
        logger.info("creating product");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null){
            logger.error("not authenticated to create product");
            return ResponseEntity.status(401).build();
        }

        if (product.getSku()==null || product.getName()==null
                || (product.getQuantity()<0 || product.getQuantity()>100) || product.getManufacturer()==null || product.getDescription()==null){
            logger.error("product format is wrong, check again");
            return ResponseEntity.badRequest().build();
        }

        try {
            //sku should be unique: if user can find it, it's not right
            if (productRepository.findBySku(product.getSku())!=null){
                logger.error("sku for the product has existed:"+product.getSku());
                return ResponseEntity.badRequest().build();
            }

            // get username from auth
            String username=authentication.getName();
            User loginUser = userRepository.findByUsername(username);
            product.setUser(loginUser);

            productRepository.saveAndFlush(product);
            logger.info("product created successfully");
        }catch (Exception e){
            return ResponseEntity.badRequest().build();
        }

        //build response
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @GetMapping("/v1/product/{productId}")
    public ResponseEntity<Product> getProductInformation(@PathVariable int productId){
        statsDClient.incrementCounter("csye6225.http.get.product.information");
        logger.info("getting product info");
        Optional<Product> byId = productRepository.findById(productId);
        if (byId.isEmpty()){
            logger.error("product info not found");
            return ResponseEntity.notFound().build();
        }else{
            logger.info("product info returned successfully");
            return ResponseEntity.ok(byId.get());
        }
    }

    @PutMapping("/v1/product/{productId}")
    public ResponseEntity<Product> updateProductPut(@PathVariable int productId, @Valid @RequestBody Product productDetail){
        statsDClient.incrementCounter("csye6225.http.update.product.information");
        logger.info("updating product info");
        if (productDetail.getId()!=null || productDetail.getDate_added()!=null
                || productDetail.getDate_last_updated()!=null){
            return ResponseEntity.badRequest().build();
        }

        //The product can only be updated by the user that created it.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null){
            logger.error("updating product not authenticated");
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User loginUser = userRepository.findByUsername(username);

        Optional<Product> product = productRepository.findById(productId);

        if (productDetail.getSku()==null || productDetail.getName()==null
                || (productDetail.getQuantity()<0 || productDetail.getQuantity()>100) || productDetail.getManufacturer()==null || productDetail.getDescription()==null){
            logger.error("updating product fields not allowed");
            return ResponseEntity.badRequest().build();
        }

        if (product.isEmpty()){
            logger.error("updating product product not found");
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
            logger.info("updated product info successfully");
            return ResponseEntity.status(204).build();
        }else{
            //forbidden: user change other people's product
            logger.error("updating others' product not allowed");
            return ResponseEntity.status(403).build();
        }
    }

    @PatchMapping("/v1/product/{productId}")
    public ResponseEntity<Product> patchProduct(@PathVariable Integer productId, @Valid @RequestBody Product productDetail){
        statsDClient.incrementCounter("csye6225.http.patch.product");
        logger.info("patching product info");
        if (productDetail.getId()!=null || productDetail.getDate_added()!=null
                || productDetail.getDate_last_updated()!=null){
            logger.error("fields don't meet format");
            return ResponseEntity.badRequest().build();
        }
        //The product can only be updated by the user that created it.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null){
            logger.error("patch product not authenticated");
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User loginUser = userRepository.findByUsername(username);

        Optional<Product> product = productRepository.findById(productId);
        if (product.isEmpty()){
            logger.error("patch product product not found");
            return ResponseEntity.badRequest().build();
        }

        if (productDetail.getQuantity()<0 || productDetail.getQuantity()>100){
            logger.error("patch product product quantity not correct");
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
            logger.info("patched product successfully");
            return ResponseEntity.status(204).build();
        }else{
            logger.error("patched others' product not allowed");
            //forbidden: user change other people's product
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/v1/product/{productId}")
    public ResponseEntity<Product> deleteProduct(@PathVariable Integer productId){
        statsDClient.incrementCounter("csye6225.http.delete.product");
        logger.info("deleting product");
        Product product = productRepository.findById(productId).orElseThrow(() -> new UserNotFoundException("User not found on : " + productId));
        if (product==null){
            logger.error("delete product not found");
            return ResponseEntity.status(404).build();
        }

        //The product can only be updated by the user that created it.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null){
            logger.error("delete product not authenticated");
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User loginUser = userRepository.findByUsername(username);
        if (!loginUser.getId().equals(product.getOwnerUserId())){
            logger.error("delete others' product not allowed");
            return ResponseEntity.status(403).build();
        }

        productRepository.deleteById(productId);
        logger.info("delete product successfully");
        return ResponseEntity.status(204).build();
    }

}
