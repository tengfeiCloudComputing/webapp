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
import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    Logger logger = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private StatsDClient statsDClient;
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

    @GetMapping("/v2/product/{product_id}/image")
    public ResponseEntity<List<Image>> getAllImage(@PathVariable int product_id){
        statsDClient.incrementCounter("csye6225.http.get.all.images");
        logger.info("getting all the image of a product");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null){
            logger.error("authentication failed");
            return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        User loginUser = userRepository.findByUsername(username);

        Optional<Product> currentProduct= productRepository.findById(product_id);
        if (currentProduct.isEmpty()){
            return ResponseEntity.status(404).build();
        }

        if (!currentProduct.get().getUser().equals(loginUser)){
            logger.error("the user has existed");
            return ResponseEntity.status(403).build();
        }

        Optional<List<Image>> imagesByProduct_id = imagePerository.findImagesByProduct_Id(product_id);
        logger.info("getting all the image of a product successfully");
        return ResponseEntity.status(200).body(imagesByProduct_id.get());
    }

    @PostMapping("/v2/product/{product_id}/image")
    public ResponseEntity<Image> upLoadImage(@PathVariable int product_id, @RequestParam("file") MultipartFile file) throws IOException {
        statsDClient.incrementCounter("csye6225.http.upload.an.image");
        logger.info("uploading an image");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null){
            logger.error("post an image but user not authenticated");
            return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        User loginUser = userRepository.findByUsername(username);

        Optional<Product> curProduct = productRepository.findById(product_id);

        if (curProduct.isEmpty()){
            logger.error("post an image but product is not found");
            return ResponseEntity.status(400).build();
        }

        if (!curProduct.get().getUser().equals(loginUser)){
            logger.error("post an image for another user not allowed");
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
        String url="https://s3.amazonaws.com/"+bucketName+"/"+key;
        String filename = file.getOriginalFilename();

        // check format
        List<String> set = new LinkedList<>();
        set.add("jpg");
        set.add("png");
        set.add("jpeg");
        set.add("gif");
        set.add("bmp");
        set.add("tiff");
        set.add("tif");
        set.add("svg");
        set.add("webp");

        boolean isImage = false;
        for (String e : set) {
            if (filename.endsWith(e)) {
                isImage = true;
                metadata.setContentType("image/"+e);
            }
        }

        if (!isImage){
            logger.error("post an image but not image format file");
            return ResponseEntity.status(400).build();
        }

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
            logger.info("uploaded an image successfully");
            return ResponseEntity.status(HttpStatus.OK).body(nImage);
        }catch (Exception e){
            throw e;
        }
    }

    @GetMapping("/v2/product/{product_id}/image/{image_id}")
    public ResponseEntity<Image> getImageDetail(@PathVariable String image_id, @PathVariable String product_id){
        statsDClient.incrementCounter("csye6225.http.get.an.image.detail");
        logger.info("getting an image detail");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null){
            logger.error("get an image but user not authenticated");

            return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        User loginUser = userRepository.findByUsername(username);
        Optional<Product> currentProduct= productRepository.findById(Integer.valueOf(product_id));
        if (currentProduct.isEmpty()){
            logger.error("get an image but product not found");

            return ResponseEntity.status(404).build();
        }


        if (!currentProduct.get().getUser().equals(loginUser)){
            logger.error("get an image of another user not allowed");

            return ResponseEntity.status(403).build();
        }

        Optional<Image> foundImage = imagePerository.findById(image_id);

        if (foundImage.isEmpty()){
            logger.error("cannot find image");
            return ResponseEntity.status(404).build();
        }
        logger.info("get an image detail successfully");
        return ResponseEntity.status(200).body(foundImage.get());
    }

    @DeleteMapping("/v2/product/{product_id}/image/{image_id}")
    public ResponseEntity<String> deleteImage(@PathVariable String image_id, @PathVariable String product_id){
        statsDClient.incrementCounter("csye6225.http.delete.an.image");
        logger.info("deleting an image");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null){
            logger.error("delete an image not authenticated");
            return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        User loginUser = userRepository.findByUsername(username);
        Optional<Product> currentProduct= productRepository.findById(Integer.valueOf(product_id));
        if (currentProduct.isEmpty()){
            logger.error("delete an image but product not found");
            return ResponseEntity.status(404).build();
        }

        if (!currentProduct.get().getUser().equals(loginUser)){
            logger.error("delete another user's image not allowed");
            return ResponseEntity.status(403).build();
        }

        Optional<Image> foundImage = imagePerository.findById(image_id);

        if (foundImage.isEmpty()){
            logger.error("delete an image but image not found");

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
                logger.error("delete an image but image not found");
                return ResponseEntity.status(404).build();
            }
            amazonS3.deleteObject(bucketName,urls[urls.length-1]);
            imagePerository.delete(fImage.get());
            logger.info("deleted an image detail successfully");

            return ResponseEntity.status(204).build();
        }catch (Exception e){
            throw e;
        }
    }
}
