package md.pandatur.filesharingapp.storage.cloud;

import org.springframework.web.multipart.MultipartFile;

public interface CloudStorageService {

    String uploadFile(MultipartFile file);

    void deleteFile(String url);
}
