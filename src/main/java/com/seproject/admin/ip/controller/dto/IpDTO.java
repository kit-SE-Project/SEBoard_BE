package com.seproject.admin.ip.controller.dto;

import com.seproject.account.Ip.domain.Ip;
import com.seproject.account.Ip.domain.IpType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

public class IpDTO {

    @Data
    public static class IpResponse {
        private Long id;
        private String ipAddress;
        private IpType ipType;

        public IpResponse(Long id, String ipAddress, IpType ipType) {
            this.id = id;
            this.ipAddress = ipAddress;
            this.ipType = ipType;
        }
    }

    @Data
    public static class CreateIpRequest {
        private String ipAddress;
        private String ipType;

    }
    @Data
    public static class DeleteIpRequest {
        private String ipAddress;

    }

    @Data
    public static class IpCondition {

        public IpCondition(String ipType) {
            this.ipType = IpType.of(ipType);
        }

        private IpType ipType;

    }

}
