package com.seproject.board.bulletin.domain.repository;

import com.seproject.board.bulletin.domain.model.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannerRepository extends JpaRepository<Banner, Long> {
//    @Query("select new com.seproject.board.bulletin.controller.dto.AdminBannerResponse(b) from Banner b")
//    Page<AdminBannerResponse> findAllBanner(Pageable pageable);

//    @Query(value = "select new com.seproject.board.bulletin.controller.dto.AdminBannerResponse(b) from Banner b where b.startDate <= current_date and b.endDate >= current_date",
//    countQuery = "select count(b) from Banner b where b.startDate <= current_date and b.endDate >= current_date")
//    Page<AdminBannerResponse> findActiveBanners(Pageable pageable);

//    @Query(value = "select new com.seproject.board.bulletin.controller.dto.AdminBannerResponse(b) from Banner b where b.startDate > current_date or b.endDate < current_date",
//    countQuery = "select count(b) from Banner b where b.startDate > current_date or b.endDate < current_date")
//    Page<AdminBannerResponse> findUnActiveBanners(Pageable pageable);

//    @Query("select new com.seproject.board.bulletin.controller.dto.BannerResponse(b) from Banner b where b.startDate <= current_date and b.endDate >= current_date")
//    List<BannerResponse> findBannersForUser();
}
