package com.seproject.board.bulletin.domain.model;

import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.InvalidDateException;
import com.seproject.file.domain.model.FileMetaData;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
public class Banner {
    @Id @GeneratedValue
    private Long bannerId;
    private LocalDate startDate;
    private LocalDate endDate;
    //File?
    //TODO : FileMetaData 변경?
    @OneToOne
    @JoinColumn(name = "file_meta_data_id")
    private FileMetaData fileMetaData;
    private String bannerUrl;

    public Banner(LocalDate startDate, LocalDate endDate, FileMetaData fileMetaData, String bannerUrl) {
        setProperties(startDate, endDate, fileMetaData, bannerUrl);
    }

    protected void setProperties(LocalDate startDate, LocalDate endDate, FileMetaData fileMetaData, String bannerUrl){
        this.startDate = startDate;
        this.endDate = endDate;

        validatePeriod(startDate, endDate);

        this.fileMetaData = fileMetaData;
        this.bannerUrl = bannerUrl;
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate){
        if(startDate.isAfter(endDate)){
            throw new InvalidDateException(ErrorCode.INVALID_DATE);
        }
    }

    public void updateBanner(LocalDate startDate, LocalDate endDate, FileMetaData fileMetaData, String bannerUrl) {
        setProperties(startDate, endDate, fileMetaData, bannerUrl);
    }
}
