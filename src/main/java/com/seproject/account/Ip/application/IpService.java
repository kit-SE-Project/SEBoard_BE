package com.seproject.account.Ip.application;

import com.seproject.account.Ip.domain.Ip;
import com.seproject.account.Ip.domain.IpType;
import com.seproject.account.Ip.domain.repository.IpRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class IpService {

    private static final String WILDCARD_IP = "0.0.0.0";

    private final IpRepository ipRepository;

    public List<Ip> findAll() {
        return ipRepository.findAll();
    }

    public List<Ip> findAllByIpType(IpType ipType) {
        if (ipType != IpType.SPAM && ipType != IpType.ADMIN) {
            throw new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_IP, null);
        }
        return ipRepository.findAllByIpType(ipType);
    }

    @Transactional
    public Long createIp(String ipAddress, IpType ipType) {
        Optional<Ip> ip = ipRepository.findByIpAddress(ipAddress);
        if (ip.isPresent()) {
            throw new CustomIllegalArgumentException(ErrorCode.ALREADY_EXIST_IP, null);
        }
        Ip newIp = Ip.builder()
                .ipAddress(ipAddress)
                .ipType(ipType)
                .build();
        ipRepository.save(newIp);
        return newIp.getId();
    }

    @Transactional
    public void deleteIp(String ipAddress) {
        Ip ip = ipRepository.findByIpAddress(ipAddress)
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_IP, null));
        ipRepository.delete(ip);
    }

    /**
     * 0.0.0.0이 admin IP 목록에 등록되어 있으면 모든 IP 허용.
     * 그렇지 않으면 요청 IP가 목록에 있을 때만 허용.
     */
    public boolean existAdminIpAddress(String ipAddress) {
        if (ipRepository.existsByIpAddressAndIpType(WILDCARD_IP, IpType.ADMIN)) {
            return true;
        }
        return ipRepository.existsByIpAddressAndIpType(ipAddress, IpType.ADMIN);
    }

    public boolean existSpamIp(String ipAddress) {
        return ipRepository.existsByIpAddressAndIpType(ipAddress, IpType.SPAM);
    }
}
