package com.tengfei.webapp.controller;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.tengfei.webapp.exceptions.UserNotFoundException;
import com.tengfei.webapp.model.Image;
import com.tengfei.webapp.model.Product;
import com.tengfei.webapp.model.User;
import com.tengfei.webapp.repository.ImagePerository;
import com.tengfei.webapp.repository.ProductRepository;
import com.tengfei.webapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class ImageController {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ImagePerository imagePerository;

    @Value("${userBucket.name}")
    private String userBucketName;

    public ImageController(UserRepository userRepository, ProductRepository productRepository, ImagePerository imagePerository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.imagePerository = imagePerository;
    }

    @GetMapping("/v1/product/{product_id}/image")
    public ResponseEntity<List<Image>> getAllImage(@PathVariable int product_id){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null){
            return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        User loginUser = userRepository.findByUsername(username);

        Optional<Product> currentProduct= productRepository.findById(product_id);
        if (currentProduct.isEmpty()){
            return ResponseEntity.status(404).build();
        }

        if (!currentProduct.get().getUser().equals(loginUser)){
            return ResponseEntity.status(403).build();
        }

        Optional<List<Image>> imagesByProduct_id = imagePerository.findImagesByProduct_Id(product_id);

        return ResponseEntity.status(200).body(imagesByProduct_id.get());
    }

    @PostMapping("/v1/product/{product_id}/image")
    public ResponseEntity<Image> upLoadImage(@PathVariable int product_id, @RequestParam("file") MultipartFile file) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null){
            return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        User loginUser = userRepository.findByUsername(username);

        Optional<Product> curProduct = productRepository.findById(product_id);

        if (curProduct.isEmpty()){
            return ResponseEntity.status(400).build();
        }

        if (!curProduct.get().getUser().equals(loginUser)){
            return ResponseEntity.status(403).build();
        }

        AmazonS3 amazonS3 = AmazonS3ClientBuilder.defaultClient();
//        List<Bucket> buckets=amazonS3.listBuckets();
        String bucketName=userBucketName;
        System.out.println(bucketName);
//        // get bucket
//        int count=0;
//        for (Bucket b:buckets) {
//            bucketName=b.getName();
//            count++;
//            if(count==2){
//                break;
//            }
//        }

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        UUID key = UUID.randomUUID();
        PutObjectResult putObjectResult = amazonS3.putObject(bucketName, key.toString(), file.getInputStream(), metadata);

        String url="https://s3.amazonaws.com/"+bucketName+"/"+key;
        String filename = file.getOriginalFilename();

//        // check format
//        if (filename.length()>0){
//            String suffix = filename.split(".")[1].strip();
//
//            Set<String> set = new HashSet<>();
//            set.add("jpg");
//            set.add("png");
//            set.add("jpeg");
//            set.add("gif");
//            set.add("bmp");
//            set.add("tiff");
//            set.add("tif");
//            set.add("svg");
//            set.add("webp");
//
//            if (!set.contains(suffix)){
//                return ResponseEntity.status(400).build();
//            }
//        }

        try {
            PutObjectRequest request = new PutObjectRequest(bucketName, key.toString(), file.getInputStream(), metadata);
            amazonS3.putObject(request);


            Image nImage = new Image();
//            nImage.setImage_id(key.toString());
            nImage.setFile_name(filename);
            nImage.setProduct(curProduct.get());
            nImage.setS3_bucket_path(url);
            nImage.setDate_created(LocalDateTime.now());
            imagePerository.saveAndFlush(nImage);

            return ResponseEntity.status(HttpStatus.OK).body(nImage);
        }catch (Exception e){
            throw e;
        }
    }

    @GetMapping("/v1/product/{product_id}/image/{image_id}")
    public ResponseEntity<Image> getImageDetail(@PathVariable String image_id, @PathVariable String product_id){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null){
            return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        User loginUser = userRepository.findByUsername(username);
        Optional<Product> currentProduct= productRepository.findById(Integer.valueOf(product_id));
        if (currentProduct.isEmpty()){
            return ResponseEntity.status(404).build();
        }

        if (!currentProduct.get().getUser().equals(loginUser)){
            return ResponseEntity.status(403).build();
        }

        Optional<Image> foundImage = imagePerository.findById(image_id);

        return ResponseEntity.status(200).body(foundImage.get());
    }

    @DeleteMapping("/v1/product/{product_id}/image/{image_id}")
    public ResponseEntity<String> deleteImage(@PathVariable String image_id, @PathVariable String product_id){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null){
            return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        User loginUser = userRepository.findByUsername(username);
        Optional<Product> currentProduct= productRepository.findById(Integer.valueOf(product_id));
        if (currentProduct.isEmpty()){
            return ResponseEntity.status(404).build();
        }

        if (!currentProduct.get().getUser().equals(loginUser)){
            return ResponseEntity.status(403).build();
        }

        Optional<Image> foundImage = imagePerository.findById(image_id);

        if (foundImage.isEmpty()){
            return ResponseEntity.status(404).build();
        }
        // Amazon find url in s3
        AmazonS3 amazonS3 = AmazonS3ClientBuilder.defaultClient();
//        List<Bucket> buckets=amazonS3.listBuckets();
        String bucketName=userBucketName;

//        // get bucket
//        int count=0;
//        for (Bucket b:buckets) {
//            bucketName=b.getName();
//            count++;
//            if(count==2){
//                break;
//            }
//        }

        String[] urls = foundImage.get().getS3_bucket_path().split("/");
        try{
            Optional<Image> fImage = imagePerository.findById(image_id);
            if (fImage.isEmpty()){
                return ResponseEntity.status(404).build();
            }
            amazonS3.deleteObject(bucketName,urls[urls.length-1]);
            imagePerository.delete(fImage.get());
            return ResponseEntity.status(204).build();
        }catch (Exception e){
            throw e;
        }
    }
}
