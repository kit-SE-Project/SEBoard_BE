package com.seproject.file.controller.dto;

import com.seproject.file.domain.model.FileConfiguration;
import com.seproject.file.domain.model.FileExtension;
import com.seproject.member.controller.dto.UserResponse;
import com.seproject.file.domain.model.FileMetaData;
import com.seproject.board.post.domain.model.Post;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

public class FileResponse {
    @Data
    public static class FileConfigurationResponse{
        private Long maxSizePerFile;
        private Long maxSizePerPost;

        public FileConfigurationResponse(FileConfiguration fileConfiguration) {
            this.maxSizePerFile = fileConfiguration.getMaxSizePerFile();
            this.maxSizePerPost = fileConfiguration.getMaxSizePerPost();
        }
    }
    @Data
    public static class PostOfFile{
        private Long postId;
        private UserResponse author;
        private String title;

        public PostOfFile(Post post){
            this.postId = post.getPostId();
            this.author = new UserResponse(post.getAuthor());
            this.title = post.getTitle();
        }
    }

    @Data
    public static class AdminFileElement{
        private Long fileMetaDataId;
        private String originalFileName;
        private String storedFileName;
        private String url;

        public AdminFileElement(FileMetaData fileMetaData){
            this.fileMetaDataId = fileMetaData.getFileMetaDataId();
            this.originalFileName = fileMetaData.getOriginalFileName();
            this.storedFileName = fileMetaData.getStoredFileName();
            this.url = fileMetaData.getUrlPath();
        }
    }

    @Data
    public static class AdminFileRetrieveResponse{
        private PostOfFile post;
        private AdminFileElement fileMetaDatList;

        public AdminFileRetrieveResponse(FileMetaData fileMetaData){
            if(fileMetaData.getPost() == null) {
                this.post = null;
            }else{
                this.post = new PostOfFile(fileMetaData.getPost());
            }
            this.fileMetaDatList = new AdminFileElement(fileMetaData);
        }

    }


    @Data
    public static class FileExtensionResponse {
        private List<String> extensionName;

        protected FileExtensionResponse(List<String> extensionName) {
            this.extensionName = extensionName;
        }

        public static FileExtensionResponse of(List<FileExtension> fileExtensions){
            List<String> extenionList = fileExtensions.stream().map(FileExtension::getExtensionName).collect(Collectors.toList());
            return new FileExtensionResponse(extenionList);
        }
    }
}
